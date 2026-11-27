package com.example.ev_station.model.response;

import java.math.BigDecimal;

/**
 * Represents charger information returned by the backend.
 */
public class ChargerResponse {

    private Long id;
    private String chargerType;
    private String chargerRating;
    private String connectorRating;
    private Integer noOfConnector;
    private Long chargingStationId;
    private BigDecimal pricePerKwh;

    public Long getId() {
        return id;
    }

    public String getChargerType() {
        return chargerType;
    }

    public String getChargerRating() {
        return chargerRating;
    }

    public String getConnectorRating() {
        return connectorRating;
    }

    public Integer getNoOfConnector() {
        return noOfConnector;
    }

    public Long getChargingStationId() {
        return chargingStationId;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }
}