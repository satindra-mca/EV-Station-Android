package com.example.ev_station.model.request;

/**
 * Represents a slot preview or booking creation request.
 */
public class BookingRequest {

    private final Long connectorId;
    private final String startTime;
    private final String endTime;

    public BookingRequest(
            Long connectorId,
            String startTime,
            String endTime) {

        this.connectorId = connectorId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getConnectorId() {
        return connectorId;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
}