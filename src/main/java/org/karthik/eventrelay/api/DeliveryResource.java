package org.karthik.eventrelay.api;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.karthik.eventrelay.api.dto.DeliveryResponse;
import org.karthik.eventrelay.domain.DeliveryEntity;

@Path("/deliveries")
@Produces(MediaType.APPLICATION_JSON)
public class DeliveryResource {
    @GET
    public List<DeliveryResponse> list(@QueryParam("eventId") UUID eventId) {
        if (eventId == null) {
            throw new BadRequestException("eventId is required");
        }
        return DeliveryEntity.find("eventId", eventId).list().stream()
                .map(entity -> toResponse((DeliveryEntity) entity))
                .collect(Collectors.toList());
    }

    private static DeliveryResponse toResponse(DeliveryEntity entity) {
        DeliveryResponse response = new DeliveryResponse();
        response.id = entity.id;
        response.eventId = entity.eventId;
        response.destinationId = entity.destinationId;
        response.status = entity.status != null ? entity.status.name() : null;
        response.attemptCount = entity.attemptCount;
        response.nextAttemptAt = entity.nextAttemptAt;
        response.lastAttemptAt = entity.lastAttemptAt;
        response.lastStatusCode = entity.lastStatusCode;
        response.lastError = entity.lastError;
        response.createdAt = entity.createdAt;
        response.updatedAt = entity.updatedAt;
        return response;
    }
}
