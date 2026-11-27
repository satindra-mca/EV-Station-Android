package com.example.ev_station.api;

import com.example.ev_station.model.response.ConnectorResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Defines REST API endpoints for connector operations.
 */
public interface ConnectorApi {

    /**
     * Retrieves connectors belonging to a specific charger.
     */
    @GET("api/connectors")
    Call<List<ConnectorResponse>> getConnectorsByChargerId(
            @Query("chargerId") Long chargerId
    );

    /**
     * Retrieves a connector by its unique identifier.
     */
    @GET("api/connectors/{id}")
    Call<ConnectorResponse> getConnectorById(
            @Path("id") Long connectorId
    );
}