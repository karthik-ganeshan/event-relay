package org.karthik.eventrelay.api;

import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
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
import jakarta.persistence.PersistenceException;
import org.karthik.eventrelay.api.dto.EventCreateRequest;
import org.karthik.eventrelay.api.dto.EventResponse;
import org.karthik.eventrelay.api.dto.IdResponse;
import org.karthik.eventrelay.domain.DestinationEntity;
import org.karthik.eventrelay.domain.EventEntity;
import org.karthik.eventrelay.service.EventService;
import org.hibernate.exception.ConstraintViolationException;

@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {
    @Inject
    EventService eventService;

    @POST
    public Response create(@Valid EventCreateRequest request,
                           @Context HttpHeaders headers,
                           @Context HttpServerRequest httpRequest) {
        DestinationEntity destination = DestinationEntity.findById(request.destinationId);
        if (destination == null) {
            throw new NotFoundException();
        }

        String idempotencyKey = normalizeKey(request.idempotencyKey);
        if (idempotencyKey != null) {
            EventEntity existing = findExistingEvent(destination.id, idempotencyKey);
            if (existing != null) {
                return Response.ok(new IdResponse(existing.id)).build();
            }
        }

        String requestId = headerValue(headers, "X-Request-Id");
        String userAgent = headerValue(headers, "User-Agent");
        String sourceIp = httpRequest != null && httpRequest.remoteAddress() != null
                ? httpRequest.remoteAddress().host()
                : null;

        try {
            EventEntity event = eventService.createEventAndDelivery(
                    destination.id,
                    idempotencyKey,
                    request.payload,
                    requestId,
                    userAgent,
                    sourceIp,
                    Instant.now()
            );
            return Response.status(Response.Status.CREATED)
                    .entity(new IdResponse(event.id))
                    .build();
        } catch (PersistenceException ex) {
            if (idempotencyKey != null && isIdempotencyViolation(ex)) {
                EventEntity existing = findExistingEvent(destination.id, idempotencyKey);
                if (existing != null) {
                    return Response.ok(new IdResponse(existing.id)).build();
                }
            }
            throw ex;
        }
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

    private static String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static EventEntity findExistingEvent(UUID destinationId, String idempotencyKey) {
        return EventEntity.find("destinationId = ?1 and idempotencyKey = ?2",
                destinationId, idempotencyKey).firstResult();
    }

    private static boolean isIdempotencyViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                String constraintName = violation.getConstraintName();
                return "events_destination_idempotency_key_idx".equals(constraintName);
            }
            current = current.getCause();
        }
        return false;
    }
}
