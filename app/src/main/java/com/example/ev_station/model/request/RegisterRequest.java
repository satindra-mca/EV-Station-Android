package com.example.ev_station.model.request;

import com.google.gson.annotations.SerializedName;

/**
 * Request model for new user registration.
 */
public class RegisterRequest {

    @SerializedName("name")
    private final String name;

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    @SerializedName("mobile")
    private final String mobile;

    public RegisterRequest(
            String name,
            String email,
            String password,
            String mobile
    ) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getMobile() {
        return mobile;
    }
}
