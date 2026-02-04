package org.karthik.eventrelay.security;

import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.ws.rs.core.Context;

@Provider
@RateLimited
@Priority(Priorities.USER)
public class RateLimitFilter implements ContainerRequestFilter {
    @ConfigProperty(name = "eventrelay.ratelimit.create.limit", defaultValue = "60")
    int limit;

    @ConfigProperty(name = "eventrelay.ratelimit.create.window-seconds", defaultValue = "60")
    int windowSeconds;

    @Context
    HttpServerRequest httpRequest;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String clientId = resolveClientId(requestContext);
        String key = clientId + "|" + requestContext.getMethod() + "|" + requestContext.getUriInfo().getPath();

        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.windowStartMs >= windowMs) {
                return new Window(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int current = window.count.get();
        if (current > limit) {
            long resetEpochSeconds = (window.windowStartMs + windowMs) / 1000L;
            requestContext.abortWith(Response.status(429)
                    .entity(Map.of("error", "Rate limit exceeded"))
                    .header("X-RateLimit-Limit", limit)
                    .header("X-RateLimit-Remaining", 0)
                    .header("X-RateLimit-Reset", resetEpochSeconds)
                    .build());
        }
    }

    private String resolveClientId(ContainerRequestContext requestContext) {
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (httpRequest != null && httpRequest.remoteAddress() != null) {
            return httpRequest.remoteAddress().host();
        }
        return "unknown";
    }

    private static final class Window {
        private final long windowStartMs;
        private final AtomicInteger count;

        private Window(long windowStartMs) {
            this.windowStartMs = windowStartMs;
            this.count = new AtomicInteger(1);
        }
    }
}
