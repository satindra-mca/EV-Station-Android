package com.example.ev_station.api;

import android.content.Context;

import com.example.ev_station.session.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Central Retrofit client for backend API communication.
 *
 * Maintains a single Retrofit instance throughout
 * the application lifecycle.
 */
public final class ApiClient {

    private static volatile Retrofit retrofit;

    // Prevent class instantiation.
    private ApiClient() {
    }

    /**
     * Returns the singleton Retrofit instance.
     *
     * Application context prevents Activity memory leaks.
     */
    public static Retrofit getClient(Context context) {

        if (retrofit == null) {

            synchronized (ApiClient.class) {

                if (retrofit == null) {

                    Context applicationContext =
                            context.getApplicationContext();

                    // Initialize secure session management.
                    SessionManager sessionManager =
                            new SessionManager(applicationContext);

                    // Configure HTTP client.
                    OkHttpClient okHttpClient =
                            new OkHttpClient.Builder()
                                    .connectTimeout(
                                            30,
                                            TimeUnit.SECONDS
                                    )
                                    .readTimeout(
                                            30,
                                            TimeUnit.SECONDS
                                    )
                                    .writeTimeout(
                                            30,
                                            TimeUnit.SECONDS
                                    )
                                    .addInterceptor(
                                            new AuthInterceptor(
                                                    sessionManager
                                            )
                                    )
                                    .followRedirects(false)
                                    .followSslRedirects(false)
                                    .build();

                    // Initialize Retrofit.
                    retrofit = new Retrofit.Builder()
                            .baseUrl(ApiConfig.BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            )
                            .build();
                }
            }
        }

        return retrofit;
    }
}
