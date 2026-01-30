package org.karthik.eventrelay.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DeliveryWorker {
    private static final Logger LOG = LoggerFactory.getLogger(DeliveryWorker.class);

    @ConfigProperty(name = "eventrelay.worker.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "eventrelay.worker.batch-size", defaultValue = "25")
    int batchSize;

    @Inject
    DeliveryService deliveryService;

    @Scheduled(every = "{eventrelay.worker.poll-interval}")
    void claimDueDeliveries() {
        if (!enabled) {
            return;
        }

        List<UUID> claimed = deliveryService.claimDueDeliveries(batchSize);
        if (claimed.isEmpty()) {
            return;
        }

        for (UUID deliveryId : claimed) {
            deliveryService.dispatchDelivery(deliveryId);
        }

        LOG.info("Claimed {} deliveries", claimed.size());
    }
}
