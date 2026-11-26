package com.example.ev_station.api;

import com.example.ev_station.model.response.ChargingStationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Defines REST API endpoints for charging station operations.
 *
 * Authentication headers are managed by the existing AuthInterceptor.
 */
public interface StationApi {

    /**
     * Retrieves all charging stations.
     */
    @GET("api/stations")
    Call<List<ChargingStationResponse>> getAllStations();

    /**
     * Retrieves a charging station by its unique identifier.
     */
    @GET("api/stations/{id}")
    Call<ChargingStationResponse> getStationById(
            @Path("id") Long id
    );

    /**
     * Retrieves charging stations within the specified radius.
     *
     * Radius is measured in kilometers.
     */
    @GET("api/stations/nearby")
    Call<List<ChargingStationResponse>> getNearbyStations(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("radius") double radius
    );
}
