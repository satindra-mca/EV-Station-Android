package com.example.ev_station.repository;

import android.content.Context;

import com.example.ev_station.api.ApiClient;
import com.example.ev_station.api.AuthApi;
import com.example.ev_station.model.request.LoginRequest;
import com.example.ev_station.model.request.RegisterRequest;
import com.example.ev_station.model.response.LoginResponse;
import com.example.ev_station.model.response.RegisterResponse;

import retrofit2.Call;

/**
 * Repository responsible for authentication-related
 * communication with the backend REST APIs.
 */
public class AuthRepository {

    private final AuthApi authApi;

    /**
     * Initializes the authentication API interface.
     */
    public AuthRepository(Context context) {

        authApi = ApiClient.getClient(
                context.getApplicationContext()
        ).create(AuthApi.class);
    }

    /**
     * Creates a login API call.
     *
     * @param request User login credentials
     * @return Retrofit login call
     */
    public Call<LoginResponse> login(LoginRequest request) {

        return authApi.login(request);
    }

    /**
     * Creates a registration API call.
     *
     * @param request New user registration details
     * @return Retrofit registration call
     */
    public Call<RegisterResponse> register(
            RegisterRequest request
    ) {

        return authApi.register(request);
    }
}
