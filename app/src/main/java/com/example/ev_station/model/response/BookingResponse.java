package com.example.ev_station.model.response;

import java.math.BigDecimal;

/**
 * Represents a booking returned by the backend.
 */
public class BookingResponse {

    private Long id;
    private Long userId;
    private Long connectorId;

    private String startTime;
    private String endTime;

    private BigDecimal estimatedAmount;
    private BigDecimal advanceAmount;
    private BigDecimal finalAmount;

    private String status;
    private String paymentStatus;
    private String paymentExpiresAt;

    private BigDecimal cancellationCharge;
    private BigDecimal refundAmount;

    private String createdAt;
    private String cancelledAt;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
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

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public BigDecimal getAdvanceAmount() {
        return advanceAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getPaymentExpiresAt() {
        return paymentExpiresAt;
    }

    public BigDecimal getCancellationCharge() {
        return cancellationCharge;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getCancelledAt() {
        return cancelledAt;
    }
}