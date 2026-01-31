package org.karthik.eventrelay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.karthik.eventrelay.domain.DestinationEntity;
import org.karthik.eventrelay.domain.EventEntity;

@ApplicationScoped
public class DeliveryDispatchService {
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String AUTH_TYPE_HMAC = "HMAC";
    private static final String HEADER_SIGNATURE = "X-Event-Relay-Signature";
    private static final String HEADER_TIMESTAMP = "X-Event-Relay-Timestamp";

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "eventrelay.worker.request-timeout", defaultValue = "5s")
    Duration requestTimeout;

    public DeliveryResult send(EventEntity event, DestinationEntity destination) {
        try {
            String body = objectMapper.writeValueAsString(event.payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(destination.url))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            applyAuthHeaders(builder, destination, body);
            HttpRequest request = builder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            return DeliveryResult.fromStatus(statusCode);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return DeliveryResult.failure(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } catch (IOException ex) {
            return DeliveryResult.failure(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } catch (Exception ex) {
            return DeliveryResult.failure(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private void applyAuthHeaders(HttpRequest.Builder builder, DestinationEntity destination, String body) {
        if (destination.authType == null || destination.authSecret == null) {
            return;
        }
        if (!AUTH_TYPE_HMAC.equalsIgnoreCase(destination.authType)) {
            return;
        }
        String secret = destination.authSecret.trim();
        if (secret.isEmpty()) {
            return;
        }
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signingInput = timestamp + "." + body;
        String signature = hmacSha256(secret, signingInput);
        builder.header(HEADER_TIMESTAMP, timestamp);
        builder.header(HEADER_SIGNATURE, signature);
    }

    private static String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute HMAC", ex);
        }
    }
}
