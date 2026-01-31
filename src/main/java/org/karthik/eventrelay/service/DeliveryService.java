package org.karthik.eventrelay.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.karthik.eventrelay.domain.DeliveryEntity;
import org.karthik.eventrelay.domain.DeliveryStatus;
import org.karthik.eventrelay.domain.DestinationEntity;
import org.karthik.eventrelay.domain.EventEntity;

@ApplicationScoped
public class DeliveryService {
    @Inject
    DeliveryDispatchService dispatchService;

    @ConfigProperty(name = "eventrelay.worker.retry-base-seconds", defaultValue = "5")
    int retryBaseSeconds;

    @ConfigProperty(name = "eventrelay.worker.retry-max-seconds", defaultValue = "300")
    int retryMaxSeconds;

    @ConfigProperty(name = "eventrelay.worker.max-attempts", defaultValue = "10")
    int maxAttempts;

    @Transactional
    public List<UUID> claimDueDeliveries(int batchSize) {
        Instant now = Instant.now();
        List<DeliveryEntity> due = DeliveryEntity.find(
                        "status = ?1 and nextAttemptAt <= ?2 order by nextAttemptAt",
                        DeliveryStatus.PENDING,
                        now)
                .page(0, batchSize)
                .list();

        for (DeliveryEntity delivery : due) {
            delivery.status = DeliveryStatus.IN_PROGRESS;
            delivery.attemptCount = delivery.attemptCount + 1;
        }

        return due.stream().map(d -> d.id).toList();
    }

    @Transactional
    public void dispatchDelivery(UUID deliveryId) {
        DeliveryEntity delivery = DeliveryEntity.findById(deliveryId);
        if (delivery == null) {
            return;
        }

        EventEntity event = EventEntity.findById(delivery.eventId);
        DestinationEntity destination = DestinationEntity.findById(delivery.destinationId);
        Instant now = Instant.now();

        if (event == null || destination == null) {
            delivery.status = DeliveryStatus.FAILED;
            delivery.lastAttemptAt = now;
            delivery.lastError = "Missing event or destination";
            delivery.nextAttemptAt = now;
            return;
        }

        if (delivery.attemptCount >= maxAttempts) {
            delivery.status = DeliveryStatus.FAILED;
            delivery.lastAttemptAt = now;
            delivery.lastError = "Max attempts exceeded";
            delivery.nextAttemptAt = now;
            return;
        }

        DeliveryResult result = dispatchService.send(event, destination);
        delivery.lastAttemptAt = now;
        delivery.lastStatusCode = result.statusCode;
        delivery.lastError = result.error;

        if (result.success) {
            delivery.status = DeliveryStatus.DELIVERED;
            delivery.nextAttemptAt = now;
            return;
        }

        long backoffSeconds = computeBackoffSeconds(delivery.attemptCount);
        delivery.status = DeliveryStatus.PENDING;
        delivery.nextAttemptAt = now.plusSeconds(backoffSeconds);
    }

    private long computeBackoffSeconds(int attemptCount) {
        // Simple exponential backoff with a hard cap.
        long multiplier = 1L << Math.max(0, attemptCount - 1);
        long backoff = (long) retryBaseSeconds * multiplier;
        return Math.min(backoff, retryMaxSeconds);
    }
}
