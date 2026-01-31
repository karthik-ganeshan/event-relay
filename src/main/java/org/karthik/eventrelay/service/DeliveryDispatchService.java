package org.karthik.eventrelay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.karthik.eventrelay.domain.DestinationEntity;
import org.karthik.eventrelay.domain.EventEntity;

@ApplicationScoped
public class DeliveryDispatchService {
    private final HttpClient client = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "eventrelay.worker.request-timeout", defaultValue = "5s")
    Duration requestTimeout;

    public DeliveryResult send(EventEntity event, DestinationEntity destination) {
        try {
            String body = objectMapper.writeValueAsString(event.payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(destination.url))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
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
}
