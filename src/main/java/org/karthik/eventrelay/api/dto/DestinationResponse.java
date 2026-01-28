package org.karthik.eventrelay.api.dto;

import java.time.Instant;
import java.util.UUID;

public class DestinationResponse {
    public UUID id;
    public String name;
    public String url;
    public String authType;
    public Boolean isActive;
    public Instant createdAt;
    public Instant updatedAt;
}
