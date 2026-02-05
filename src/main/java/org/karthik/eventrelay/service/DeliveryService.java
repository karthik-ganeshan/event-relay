package org.karthik.eventrelay.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.karthik.eventrelay.domain.DeliveryAttemptEntity;
import org.karthik.eventrelay.domain.DeliveryEntity;
import org.karthik.eventrelay.domain.DeliveryStatus;
import org.karthik.eventrelay.domain.DestinationEntity;
import org.karthik.eventrelay.domain.EventEntity;
import org.jboss.logging.MDC;

@ApplicationScoped
public class DeliveryService {
    @Inject
    DeliveryDispatchService dispatchService;

    @Inject
    EntityManager entityManager;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "eventrelay.worker.retry-base-seconds", defaultValue = "5")
    int retryBaseSeconds;

    @ConfigProperty(name = "eventrelay.worker.retry-max-seconds", defaultValue = "300")
    int retryMaxSeconds;

    @ConfigProperty(name = "eventrelay.worker.retry-jitter-percent", defaultValue = "20")
    int retryJitterPercent;

    @ConfigProperty(name = "eventrelay.worker.max-attempts", defaultValue = "10")
    int maxAttempts;

    @ConfigProperty(name = "eventrelay.worker.max-in-flight-per-destination", defaultValue = "2")
    int maxInFlightPerDestination;

    @ConfigProperty(name = "eventrelay.worker.failure-threshold", defaultValue = "3")
    int failureThreshold;

    @ConfigProperty(name = "eventrelay.worker.cooldown-seconds", defaultValue = "60")
    int cooldownSeconds;

    @Transactional
    public List<UUID> claimDueDeliveries(int batchSize) {
        // Claim rows atomically to avoid double processing when multiple workers run
        String sql = """
                WITH ranked AS (
                    SELECT d.id,
                           d.destination_id,
                           d.next_attempt_at,
                           row_number() over (partition by d.destination_id order by d.next_attempt_at) AS rn
                    FROM deliveries d
                    JOIN destinations dest ON dest.id = d.destination_id
                    WHERE d.status = 'PENDING'
                      AND d.next_attempt_at <= now()
                      AND (dest.cooldown_until IS NULL OR dest.cooldown_until <= now())
                ),
                capacity AS (
                    SELECT destination_id,
                           GREATEST(0, :maxInFlight - COUNT(*)) AS slots
                    FROM deliveries
                    WHERE status = 'IN_PROGRESS'
                    GROUP BY destination_id
                ),
                selected AS (
                    SELECT r.id
                    FROM ranked r
                    LEFT JOIN capacity c ON c.destination_id = r.destination_id
                    WHERE r.rn <= COALESCE(c.slots, :maxInFlight)
                    ORDER BY r.next_attempt_at
                    LIMIT :limit
                ),
                locked AS (
                    SELECT d.id
                    FROM deliveries d
                    JOIN selected s ON s.id = d.id
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE deliveries
                SET status = 'IN_PROGRESS',
                    attempt_count = attempt_count + 1
                WHERE id IN (SELECT id FROM locked)
                RETURNING id
                """;

        @SuppressWarnings("unchecked")
        List<UUID> ids = entityManager.createNativeQuery(sql)
                .setParameter("limit", batchSize)
                .setParameter("maxInFlight", maxInFlightPerDestination)
                .getResultList();
        if (!ids.isEmpty()) {
            meterRegistry.counter("eventrelay.deliveries.claimed").increment(ids.size());
        }
        return ids;
    }

    @Transactional
    public void dispatchDelivery(UUID deliveryId) {
        DeliveryEntity delivery = DeliveryEntity.findById(deliveryId);
        if (delivery == null) {
            return;
        }

        long startNanos = System.nanoTime();
        Instant startedAt = Instant.now();
        Integer attemptStatusCode = null;
        String attemptError = null;
        String outcome = "unknown";
        DestinationEntity destination = null;

        MDC.put("deliveryId", delivery.id.toString());
        MDC.put("eventId", delivery.eventId.toString());

        Instant now = Instant.now();

        try {
            EventEntity event = EventEntity.findById(delivery.eventId);
            destination = DestinationEntity.findById(delivery.destinationId);

            if (destination != null && destination.cooldownUntil != null && destination.cooldownUntil.isAfter(now)) {
                attemptError = "Destination in cooldown";
                outcome = "cooldown";
                delivery.status = DeliveryStatus.PENDING;
                delivery.lastAttemptAt = now;
                delivery.lastError = attemptError;
                delivery.nextAttemptAt = destination.cooldownUntil;
                meterRegistry.counter("eventrelay.deliveries.deferred_cooldown").increment();
                return;
            }

            if (event == null || destination == null) {
                attemptError = "Missing event or destination";
                outcome = "missing";
                delivery.status = DeliveryStatus.FAILED;
                delivery.lastAttemptAt = now;
                delivery.lastError = attemptError;
                delivery.nextAttemptAt = now;
                meterRegistry.counter("eventrelay.deliveries.failed").increment();
                return;
            }

            DeliveryResult result = dispatchService.send(event, destination);
            attemptStatusCode = result.statusCode;
            attemptError = result.error;
            delivery.lastAttemptAt = now;
            delivery.lastStatusCode = result.statusCode;
            delivery.lastError = result.error;

            if (result.success) {
                outcome = "delivered";
                resetFailures(destination);
                delivery.status = DeliveryStatus.DELIVERED;
                delivery.nextAttemptAt = now;
                meterRegistry.counter("eventrelay.deliveries.delivered").increment();
                return;
            }

            registerFailure(destination, now);

            if (delivery.attemptCount >= maxAttempts) {
                outcome = "dead_letter";
                delivery.status = DeliveryStatus.FAILED;
                delivery.lastError = "Max attempts exceeded";
                delivery.nextAttemptAt = now;
                meterRegistry.counter("eventrelay.deliveries.dead_lettered").increment();
                return;
            }

            outcome = "retry";
            long backoffSeconds = computeBackoffSeconds(delivery.attemptCount);
            delivery.status = DeliveryStatus.PENDING;
            delivery.nextAttemptAt = now.plusSeconds(backoffSeconds);
            meterRegistry.counter("eventrelay.deliveries.retried").increment();
        } finally {
            recordAttempt(delivery, startedAt, Instant.now(), attemptStatusCode, attemptError);
            recordOutcomeMetrics(destination, outcome, startNanos);
            MDC.remove("deliveryId");
            MDC.remove("eventId");
        }
    }

    private void recordAttempt(DeliveryEntity delivery, Instant startedAt, Instant finishedAt, Integer statusCode, String error) {
        DeliveryAttemptEntity attempt = new DeliveryAttemptEntity();
        attempt.id = UUID.randomUUID();
        attempt.deliveryId = delivery.id;
        attempt.attemptNo = delivery.attemptCount;
        attempt.statusCode = statusCode;
        attempt.error = error;
        attempt.startedAt = startedAt;
        attempt.finishedAt = finishedAt;
        attempt.persist();
    }

    private void recordOutcomeMetrics(DestinationEntity destination, String outcome, long startNanos) {
        String destinationId = destination != null ? destination.id.toString() : "unknown";
        meterRegistry.counter("eventrelay.deliveries.outcome", "destinationId", destinationId, "outcome", outcome)
                .increment();
        meterRegistry.timer("eventrelay.deliveries.latency", "destinationId", destinationId, "outcome", outcome)
                .record(Duration.ofNanos(System.nanoTime() - startNanos));
    }

    private long computeBackoffSeconds(int attemptCount) {
        // Simple exponential backoff with a hard cap
        long multiplier = 1L << Math.max(0, attemptCount - 1);
        long backoff = (long) retryBaseSeconds * multiplier;
        long capped = Math.min(backoff, retryMaxSeconds);
        if (capped == 0 || retryJitterPercent <= 0) {
            return capped;
        }
        double jitter = retryJitterPercent / 100.0;
        double min = Math.max(0.0, 1.0 - jitter);
        double max = 1.0 + jitter;
        double factor = ThreadLocalRandom.current().nextDouble(min, max);
        return Math.max(0L, Math.round(capped * factor));
    }

    private void resetFailures(DestinationEntity destination) {
        if (destination == null) {
            return;
        }
        destination.consecutiveFailures = 0;
        destination.cooldownUntil = null;
    }

    private void registerFailure(DestinationEntity destination, Instant now) {
        if (destination == null) {
            return;
        }
        destination.consecutiveFailures = destination.consecutiveFailures + 1;
        if (destination.consecutiveFailures >= failureThreshold) {
            destination.cooldownUntil = now.plusSeconds(cooldownSeconds);
            meterRegistry.counter("eventrelay.destinations.cooldowned").increment();
        }
    }
}
