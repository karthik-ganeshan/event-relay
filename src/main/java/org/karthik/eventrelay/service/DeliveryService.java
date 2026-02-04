package org.karthik.eventrelay.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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

    @ConfigProperty(name = "eventrelay.worker.max-attempts", defaultValue = "10")
    int maxAttempts;

    @Transactional
    public List<UUID> claimDueDeliveries(int batchSize) {
        // Claim rows atomically to avoid double processing when multiple workers run
        String sql = """
                UPDATE deliveries
                SET status = 'IN_PROGRESS',
                    attempt_count = attempt_count + 1
                WHERE id IN (
                    SELECT id FROM deliveries
                    WHERE status = 'PENDING' AND next_attempt_at <= now()
                    ORDER BY next_attempt_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT :limit
                )
                RETURNING id
                """;

        @SuppressWarnings("unchecked")
        List<UUID> ids = entityManager.createNativeQuery(sql)
                .setParameter("limit", batchSize)
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

        MDC.put("deliveryId", delivery.id.toString());
        MDC.put("eventId", delivery.eventId.toString());

        EventEntity event = EventEntity.findById(delivery.eventId);
        DestinationEntity destination = DestinationEntity.findById(delivery.destinationId);
        Instant now = Instant.now();

        try {
            if (event == null || destination == null) {
                delivery.status = DeliveryStatus.FAILED;
                delivery.lastAttemptAt = now;
                delivery.lastError = "Missing event or destination";
                delivery.nextAttemptAt = now;
                meterRegistry.counter("eventrelay.deliveries.failed").increment();
                return;
            }

            DeliveryResult result = dispatchService.send(event, destination);
            delivery.lastAttemptAt = now;
            delivery.lastStatusCode = result.statusCode;
            delivery.lastError = result.error;

            if (result.success) {
                delivery.status = DeliveryStatus.DELIVERED;
                delivery.nextAttemptAt = now;
                meterRegistry.counter("eventrelay.deliveries.delivered").increment();
                return;
            }

            if (delivery.attemptCount >= maxAttempts) {
                delivery.status = DeliveryStatus.FAILED;
                delivery.lastError = "Max attempts exceeded";
                delivery.nextAttemptAt = now;
                meterRegistry.counter("eventrelay.deliveries.dead_lettered").increment();
                return;
            }

            long backoffSeconds = computeBackoffSeconds(delivery.attemptCount);
            delivery.status = DeliveryStatus.PENDING;
            delivery.nextAttemptAt = now.plusSeconds(backoffSeconds);
            meterRegistry.counter("eventrelay.deliveries.retried").increment();
        } finally {
            MDC.remove("deliveryId");
            MDC.remove("eventId");
        }
    }

    private long computeBackoffSeconds(int attemptCount) {
        // Simple exponential backoff with a hard cap
        long multiplier = 1L << Math.max(0, attemptCount - 1);
        long backoff = (long) retryBaseSeconds * multiplier;
        return Math.min(backoff, retryMaxSeconds);
    }
}
