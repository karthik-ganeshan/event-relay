package org.karthik.eventrelay.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class EventCreateRequest {
    @NotNull
    public UUID destinationId;

    public String idempotencyKey;

    @NotNull
    public JsonNode payload;
}
