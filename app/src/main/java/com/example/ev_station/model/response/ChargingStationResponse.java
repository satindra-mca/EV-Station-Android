package com.example.ev_station.model.response;

/**
 * Represents a charging station returned by the backend API.
 *
 * The field names match the backend ChargingStationResponse DTO.
 * Nullable coordinates are represented using Double.
 */
public class ChargingStationResponse {

    private Long id;
    private String cpoName;
    private String govtPrivate;
    private String state;
    private String district;
    private String cityVillage;
    private String location;
    private Double latitude;
    private Double longitude;
    private String source;
    private String googlePlaceId;
    private String status;

    public Long getId() {
        return id;
    }

    public String getCpoName() {
        return cpoName;
    }

    public String getGovtPrivate() {
        return govtPrivate;
    }

    public String getState() {
        return state;
    }

    public String getDistrict() {
        return district;
    }

    public String getCityVillage() {
        return cityVillage;
    }

    public String getLocation() {
        return location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getSource() {
        return source;
    }

    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public String getStatus() {
        return status;
    }
}
