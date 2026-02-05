package org.karthik.eventrelay.api;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.karthik.eventrelay.api.dto.DeliveryAttemptResponse;
import org.karthik.eventrelay.api.dto.DeliveryResponse;
import org.karthik.eventrelay.domain.DeliveryAttemptEntity;
import org.karthik.eventrelay.domain.DeliveryEntity;
import org.karthik.eventrelay.domain.DeliveryStatus;
import org.karthik.eventrelay.security.AdminEndpoint;

@AdminEndpoint
@Path("/deliveries")
@Produces(MediaType.APPLICATION_JSON)
public class DeliveryResource {
    @Inject
    MeterRegistry meterRegistry;

    @GET
    public List<DeliveryResponse> list(@QueryParam("eventId") UUID eventId) {
        if (eventId == null) {
            throw new BadRequestException("eventId is required");
        }
        return DeliveryEntity.find("eventId", eventId).list().stream()
                .map(entity -> toResponse((DeliveryEntity) entity))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}/attempts")
    public List<DeliveryAttemptResponse> listAttempts(@PathParam("id") UUID id) {
        DeliveryEntity delivery = DeliveryEntity.findById(id);
        if (delivery == null) {
            throw new NotFoundException("Delivery not found");
        }
        return DeliveryAttemptEntity.find("deliveryId = ?1 order by attemptNo", id).list().stream()
                .map(entity -> toAttemptResponse((DeliveryAttemptEntity) entity))
                .collect(Collectors.toList());
    }

    @POST
    @Path("/{id}/redrive")
    @AdminEndpoint
    @Transactional
    public Response redrive(@PathParam("id") UUID id) {
        DeliveryEntity delivery = DeliveryEntity.findById(id);
        if (delivery == null) {
            throw new NotFoundException("Delivery not found");
        }
        if (delivery.status != DeliveryStatus.FAILED) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Delivery is not in FAILED state"))
                    .build();
        }
        delivery.status = DeliveryStatus.PENDING;
        delivery.attemptCount = 0;
        delivery.nextAttemptAt = Instant.now();
        delivery.lastAttemptAt = null;
        delivery.lastStatusCode = null;
        delivery.lastError = null;
        meterRegistry.counter("eventrelay.deliveries.redriven").increment();
        return Response.noContent().build();
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

    private static DeliveryAttemptResponse toAttemptResponse(DeliveryAttemptEntity entity) {
        DeliveryAttemptResponse response = new DeliveryAttemptResponse();
        response.id = entity.id;
        response.deliveryId = entity.deliveryId;
        response.attemptNo = entity.attemptNo;
        response.statusCode = entity.statusCode;
        response.error = entity.error;
        response.startedAt = entity.startedAt;
        response.finishedAt = entity.finishedAt;
        return response;
    }
}
