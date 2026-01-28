package org.karthik.eventrelay.api.dto;

import jakarta.validation.constraints.NotBlank;

public class DestinationCreateRequest {
    @NotBlank
    public String name;

    @NotBlank
    public String url;

    public String authType;

    public String authSecret;

    public Boolean isActive;
}
