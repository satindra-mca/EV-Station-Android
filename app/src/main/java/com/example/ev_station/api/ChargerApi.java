package com.example.ev_station.api;

import com.example.ev_station.model.response.ChargerResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Defines REST API endpoints for charger operations.
 */
public interface ChargerApi {

    /**
     * Retrieves chargers belonging to a specific station.
     */
    @GET("api/chargers")
    Call<List<ChargerResponse>> getChargersByStationId(
            @Query("stationId") Long stationId
    );

    /**
     * Retrieves a charger by its unique identifier.
     */
    @GET("api/chargers/{id}")
    Call<ChargerResponse> getChargerById(
            @Path("id") Long chargerId
    );
}