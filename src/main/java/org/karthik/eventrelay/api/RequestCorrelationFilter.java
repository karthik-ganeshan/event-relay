package org.karthik.eventrelay.api;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.jboss.logging.MDC;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class RequestCorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_REQUEST_ID, requestId);
        requestContext.setProperty(MDC_REQUEST_ID, requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object requestId = requestContext.getProperty(MDC_REQUEST_ID);
        if (requestId != null) {
            responseContext.getHeaders().putSingle(HEADER_REQUEST_ID, requestId.toString());
        }
        MDC.remove(MDC_REQUEST_ID);
    }
}
