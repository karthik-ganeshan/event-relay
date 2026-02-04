package org.karthik.eventrelay.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@AdminEndpoint
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {
    private static final String HEADER = "X-API-Key";

    @ConfigProperty(name = "eventrelay.auth.api-key")
    Optional<String> apiKey;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String configured = apiKey.map(String::trim).orElse("");
        if (configured.isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("API key not configured")
                    .build());
            return;
        }

        String provided = requestContext.getHeaderString(HEADER);
        if (provided == null || provided.isBlank()) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("API key required")
                    .build());
            return;
        }

        if (!configured.equals(provided.trim())) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Invalid API key")
                    .build());
        }
    }
}
