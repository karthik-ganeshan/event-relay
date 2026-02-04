package org.karthik.eventrelay.api.dto;

import java.time.Instant;
import java.util.UUID;

public class DeliveryAttemptResponse {
    public UUID id;
    public UUID deliveryId;
    public int attemptNo;
    public Integer statusCode;
    public String error;
    public Instant startedAt;
    public Instant finishedAt;
}
