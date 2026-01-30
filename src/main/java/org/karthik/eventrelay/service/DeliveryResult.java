package org.karthik.eventrelay.service;

public class DeliveryResult {
    public final boolean success;
    public final Integer statusCode;
    public final String error;

    private DeliveryResult(boolean success, Integer statusCode, String error) {
        this.success = success;
        this.statusCode = statusCode;
        this.error = error;
    }

    public static DeliveryResult fromStatus(int statusCode) {
        boolean success = statusCode >= 200 && statusCode < 300;
        String error = success ? null : "HTTP " + statusCode;
        return new DeliveryResult(success, statusCode, error);
    }

    public static DeliveryResult failure(String error) {
        return new DeliveryResult(false, null, error);
    }
}
