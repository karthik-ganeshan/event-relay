package org.karthik.eventrelay.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public class EventResponse {
    public UUID id;
    public UUID destinationId;
    public String idempotencyKey;
    public JsonNode payload;
    public Instant receivedAt;
    public String requestId;
    public String sourceIp;
    public String userAgent;
}
