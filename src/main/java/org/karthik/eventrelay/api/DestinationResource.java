package org.karthik.eventrelay.api;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.karthik.eventrelay.api.dto.DestinationCreateRequest;
import org.karthik.eventrelay.api.dto.DestinationResponse;
import org.karthik.eventrelay.api.dto.IdResponse;
import org.karthik.eventrelay.domain.DestinationEntity;

@Path("/destinations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DestinationResource {
    @POST
    @Transactional
    public Response create(@Valid DestinationCreateRequest request) {
        DestinationEntity entity = new DestinationEntity();
        entity.id = UUID.randomUUID();
        entity.name = request.name;
        entity.url = request.url;
        entity.authType = request.authType;
        entity.authSecret = request.authSecret;
        entity.isActive = request.isActive != null ? request.isActive : true;
        entity.persist();

        return Response.status(Response.Status.CREATED)
                .entity(new IdResponse(entity.id))
                .build();
    }

    @GET
    @Path("/{id}")
    public DestinationResponse get(@PathParam("id") UUID id) {
        DestinationEntity entity = DestinationEntity.findById(id);
        if (entity == null) {
            throw new NotFoundException();
        }
        return toResponse(entity);
    }

    private static DestinationResponse toResponse(DestinationEntity entity) {
        DestinationResponse response = new DestinationResponse();
        response.id = entity.id;
        response.name = entity.name;
        response.url = entity.url;
        response.authType = entity.authType;
        response.isActive = entity.isActive;
        response.createdAt = entity.createdAt;
        response.updatedAt = entity.updatedAt;
        return response;
    }
}
