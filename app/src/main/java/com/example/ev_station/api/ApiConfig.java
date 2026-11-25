package com.example.ev_station.api;

/**
 * Central configuration for backend API communication.
 */
public final class ApiConfig {

    // Local Spring Boot backend for Android Emulator.
    public static final String BASE_URL =
            "http://10.0.2.2:8080/";

    // Prevent class instantiation.
    private ApiConfig() {
    }
}