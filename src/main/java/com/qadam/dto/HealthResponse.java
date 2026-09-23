package com.qadam.dto;

public record HealthResponse(String status) {

    public static HealthResponse ok() {
        return new HealthResponse("ok");
    }
}
