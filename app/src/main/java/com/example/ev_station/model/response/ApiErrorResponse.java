package com.example.ev_station.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Common response model for backend API errors.
 */
public class ApiErrorResponse {

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("status")
    private Integer status;

    @SerializedName("error")
    private String error;

    @SerializedName("message")
    private String message;

    @SerializedName("errors")
    private Map<String, String> errors;

    public String getTimestamp() {
        return timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
