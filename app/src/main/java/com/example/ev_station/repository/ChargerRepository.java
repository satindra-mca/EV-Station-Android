package com.example.ev_station.repository;

import android.content.Context;

import com.example.ev_station.api.ApiClient;
import com.example.ev_station.api.ChargerApi;
import com.example.ev_station.model.response.ChargerResponse;

import java.util.List;

import retrofit2.Call;

/**
 * Repository responsible for charger data operations.
 */
public class ChargerRepository {

    private final ChargerApi chargerApi;

    public ChargerRepository(Context context) {

        chargerApi = ApiClient
                .getClient(context.getApplicationContext())
                .create(ChargerApi.class);
    }

    public Call<List<ChargerResponse>> getChargersByStationId(
            Long stationId) {

        return chargerApi.getChargersByStationId(stationId);
    }

    public Call<ChargerResponse> getChargerById(
            Long chargerId) {

        return chargerApi.getChargerById(chargerId);
    }
}