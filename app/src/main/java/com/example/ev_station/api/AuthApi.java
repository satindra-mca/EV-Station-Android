package com.example.ev_station.api;

import com.example.ev_station.model.request.LoginRequest;
import com.example.ev_station.model.request.RegisterRequest;
import com.example.ev_station.model.response.LoginResponse;
import com.example.ev_station.model.response.RegisterResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit interface for authentication-related REST APIs.
 */
public interface AuthApi {

    /**
     * Authenticates a registered user.
     */
    @POST("api/auth/login")
    Call<LoginResponse> login(
            @Body LoginRequest request
    );

    /**
     * Registers a new user account.
     */
    @POST("api/auth/register")
    Call<RegisterResponse> register(
            @Body RegisterRequest request
    );
}
