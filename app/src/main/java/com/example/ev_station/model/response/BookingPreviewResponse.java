package com.example.ev_station.model.response;

import java.math.BigDecimal;

/**
 * Represents slot availability and estimated booking charges.
 */
public class BookingPreviewResponse {

    private Long connectorId;
    private boolean available;
    private String startTime;
    private String endTime;
    private long durationMinutes;
    private BigDecimal pricePerKwh;
    private BigDecimal estimatedEnergy;
    private BigDecimal estimatedAmount;
    private BigDecimal advanceAmount;
    private String message;

    public Long getConnectorId() {
        return connectorId;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public BigDecimal getEstimatedEnergy() {
        return estimatedEnergy;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public BigDecimal getAdvanceAmount() {
        return advanceAmount;
    }

    public String getMessage() {
        return message;
    }
}