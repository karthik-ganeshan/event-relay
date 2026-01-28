package org.karthik.eventrelay.api.dto;

import java.time.Instant;
import java.util.UUID;

public class DeliveryResponse {
    public UUID id;
    public UUID eventId;
    public UUID destinationId;
    public String status;
    public int attemptCount;
    public Instant nextAttemptAt;
    public Instant lastAttemptAt;
    public Integer lastStatusCode;
    public String lastError;
    public Instant createdAt;
    public Instant updatedAt;
}
