package org.karthik.eventrelay.api;

import io.vertx.core.http.HttpServerRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.UUID;
import org.karthik.eventrelay.api.dto.EventCreateRequest;
import org.karthik.eventrelay.api.dto.EventResponse;
import org.karthik.eventrelay.api.dto.IdResponse;
import org.karthik.eventrelay.domain.DeliveryEntity;
import org.karthik.eventrelay.domain.DeliveryStatus;
import org.karthik.eventrelay.domain.DestinationEntity;
import org.karthik.eventrelay.domain.EventEntity;

@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {
    @POST
    @Transactional
    public Response create(@Valid EventCreateRequest request,
                           @Context HttpHeaders headers,
                           @Context HttpServerRequest httpRequest) {
        DestinationEntity destination = DestinationEntity.findById(request.destinationId);
        if (destination == null) {
            throw new NotFoundException();
        }

        EventEntity event = new EventEntity();
        event.id = UUID.randomUUID();
        event.destinationId = destination.id;
        event.idempotencyKey = request.idempotencyKey;
        event.payload = request.payload;
        event.requestId = headerValue(headers, "X-Request-Id");
        event.userAgent = headerValue(headers, "User-Agent");
        event.sourceIp = httpRequest != null && httpRequest.remoteAddress() != null
                ? httpRequest.remoteAddress().host()
                : null;
        event.persist();

        DeliveryEntity delivery = new DeliveryEntity();
        delivery.id = UUID.randomUUID();
        delivery.eventId = event.id;
        delivery.destinationId = destination.id;
        delivery.status = DeliveryStatus.PENDING;
        delivery.attemptCount = 0;
        delivery.nextAttemptAt = Instant.now();
        delivery.persist();

        return Response.status(Response.Status.CREATED)
                .entity(new IdResponse(event.id))
                .build();
    }

    @GET
    @Path("/{id}")
    public EventResponse get(@PathParam("id") UUID id) {
        EventEntity event = EventEntity.findById(id);
        if (event == null) {
            throw new NotFoundException();
        }
        return toResponse(event);
    }

    private static EventResponse toResponse(EventEntity entity) {
        EventResponse response = new EventResponse();
        response.id = entity.id;
        response.destinationId = entity.destinationId;
        response.idempotencyKey = entity.idempotencyKey;
        response.payload = entity.payload;
        response.receivedAt = entity.receivedAt;
        response.requestId = entity.requestId;
        response.sourceIp = entity.sourceIp;
        response.userAgent = entity.userAgent;
        return response;
    }

    private static String headerValue(HttpHeaders headers, String name) {
        if (headers == null) {
            return null;
        }
        String value = headers.getHeaderString(name);
        return value != null && !value.isBlank() ? value : null;
    }
}
