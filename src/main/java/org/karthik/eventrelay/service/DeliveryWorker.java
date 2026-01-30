package org.karthik.eventrelay.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.karthik.eventrelay.domain.DeliveryEntity;
import org.karthik.eventrelay.domain.DeliveryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DeliveryWorker {
    private static final Logger LOG = LoggerFactory.getLogger(DeliveryWorker.class);

    @ConfigProperty(name = "eventrelay.worker.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "eventrelay.worker.batch-size", defaultValue = "25")
    int batchSize;

    @Scheduled(every = "{eventrelay.worker.poll-interval}")
    @Transactional
    void claimDueDeliveries() {
        if (!enabled) {
            return;
        }

        Instant now = Instant.now();
        List<DeliveryEntity> due = DeliveryEntity.find(
                        "status = ?1 and nextAttemptAt <= ?2 order by nextAttemptAt",
                        DeliveryStatus.PENDING,
                        now)
                .page(0, batchSize)
                .list();

        if (due.isEmpty()) {
            return;
        }

        for (DeliveryEntity delivery : due) {
            delivery.status = DeliveryStatus.IN_PROGRESS;
            delivery.attemptCount = delivery.attemptCount + 1;
        }

        LOG.info("Claimed {} deliveries", due.size());
    }
}
