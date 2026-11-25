package com.example.ev_station.utils;

import com.example.ev_station.model.response.ApiErrorResponse;
import com.google.gson.Gson;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Utility class for parsing backend API error responses.
 */
public final class ApiErrorParser {

    private static final Gson GSON = new Gson();

    // Prevent class instantiation.
    private ApiErrorParser() {
    }

    /**
     * Parses the error body returned by the backend.
     */
    public static ApiErrorResponse parseError(
            Response<?> response
    ) {
        if (response == null) {
            return null;
        }

        ResponseBody errorBody = response.errorBody();

        if (errorBody == null) {
            return null;
        }

        try (ResponseBody body = errorBody) {

            return GSON.fromJson(
                    body.string(),
                    ApiErrorResponse.class
            );

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns a user-friendly error message.
     */
    public static String getErrorMessage(
            int statusCode,
            ApiErrorResponse apiError
    ) {

        // Handle unexpected server errors safely.
        if (statusCode >= 500) {
            return "Server error. Please try again later.";
        }

        // Use the backend message when available.
        if (apiError != null &&
                apiError.getMessage() != null &&
                !apiError.getMessage().trim().isEmpty()) {

            return apiError.getMessage();
        }

        // Provide fallback messages.
        switch (statusCode) {

            case 400:
                return "Please check your input.";

            case 401:
                return "Authentication failed.";

            case 403:
                return "Access denied.";

            case 404:
                return "Requested resource not found.";

            case 409:
                return "Request conflicts with existing data.";

            case 429:
                return "Too many requests. Please try again later.";

            default:
                return "Something went wrong. Please try again.";
        }
    }
}
