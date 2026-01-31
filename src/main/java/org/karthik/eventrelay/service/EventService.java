package org.karthik.eventrelay.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import jakarta.inject.Inject;
import io.micrometer.core.instrument.MeterRegistry;
import org.karthik.eventrelay.domain.DeliveryEntity;
import org.karthik.eventrelay.domain.DeliveryStatus;
import org.karthik.eventrelay.domain.EventEntity;

@ApplicationScoped
public class EventService {
    @Inject
    MeterRegistry meterRegistry;

    @Transactional
    public EventEntity createEventAndDelivery(UUID destinationId,
                                              String idempotencyKey,
                                              JsonNode payload,
                                              String requestId,
                                              String userAgent,
                                              String sourceIp,
                                              Instant nextAttemptAt) {
        EventEntity event = new EventEntity();
        event.id = UUID.randomUUID();
        event.destinationId = destinationId;
        event.idempotencyKey = idempotencyKey;
        event.payload = payload;
        event.requestId = requestId;
        event.userAgent = userAgent;
        event.sourceIp = sourceIp;
        event.persist();

        DeliveryEntity delivery = new DeliveryEntity();
        delivery.id = UUID.randomUUID();
        delivery.eventId = event.id;
        delivery.destinationId = destinationId;
        delivery.status = DeliveryStatus.PENDING;
        delivery.attemptCount = 0;
        delivery.nextAttemptAt = nextAttemptAt != null ? nextAttemptAt : Instant.now();
        delivery.persist();

        meterRegistry.counter("eventrelay.events.created").increment();
        meterRegistry.counter("eventrelay.deliveries.created").increment();

        return event;
    }
}
