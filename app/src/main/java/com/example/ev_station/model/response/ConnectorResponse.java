package com.example.ev_station.model.response;

/**
 * Represents a charging connector returned by the backend.
 */
public class ConnectorResponse {

    private Long id;
    private Integer connectorNumber;
    private String status;
    private Long chargerId;

    public Long getId() {
        return id;
    }

    public Integer getConnectorNumber() {
        return connectorNumber;
    }

    public String getStatus() {
        return status;
    }

    public Long getChargerId() {
        return chargerId;
    }
}