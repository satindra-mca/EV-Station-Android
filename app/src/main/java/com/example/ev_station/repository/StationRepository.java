package com.example.ev_station.repository;

import android.content.Context;

import com.example.ev_station.api.ApiClient;
import com.example.ev_station.api.StationApi;
import com.example.ev_station.model.response.ChargingStationResponse;

import java.util.List;

import retrofit2.Call;

/**
 * Repository responsible for charging station data operations.
 *
 * Uses the shared Retrofit client and existing JWT authentication
 * infrastructure to communicate with the backend.
 */
public class StationRepository {

    private final StationApi stationApi;

    /**
     * Initializes the charging station API using the shared client.
     *
     * @param context application or activity context
     */
    public StationRepository(Context context) {

        stationApi = ApiClient
                .getClient(context.getApplicationContext())
                .create(StationApi.class);
    }

    /**
     * Retrieves all charging stations.
     */
    public Call<List<ChargingStationResponse>> getAllStations() {

        return stationApi.getAllStations();
    }

    /**
     * Retrieves a charging station by its unique identifier.
     *
     * @param id charging station ID
     */
    public Call<ChargingStationResponse> getStationById(Long id) {

        return stationApi.getStationById(id);
    }

    /**
     * Retrieves charging stations within the specified radius.
     *
     * @param latitude search latitude
     * @param longitude search longitude
     * @param radius search radius in kilometers
     */
    public Call<List<ChargingStationResponse>> getNearbyStations(
            double latitude,
            double longitude,
            double radius
    ) {

        return stationApi.getNearbyStations(
                latitude,
                longitude,
                radius
        );
    }
}
