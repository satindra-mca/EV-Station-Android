package com.example.ev_station.repository;

import android.content.Context;

import com.example.ev_station.api.ApiClient;
import com.example.ev_station.api.ConnectorApi;
import com.example.ev_station.model.response.ConnectorResponse;

import java.util.List;

import retrofit2.Call;

/**
 * Repository responsible for connector data operations.
 */
public class ConnectorRepository {

    private final ConnectorApi connectorApi;

    public ConnectorRepository(Context context) {

        connectorApi = ApiClient
                .getClient(context.getApplicationContext())
                .create(ConnectorApi.class);
    }

    public Call<List<ConnectorResponse>> getConnectorsByChargerId(
            Long chargerId) {

        return connectorApi.getConnectorsByChargerId(chargerId);
    }

    public Call<ConnectorResponse> getConnectorById(
            Long connectorId) {

        return connectorApi.getConnectorById(connectorId);
    }
}