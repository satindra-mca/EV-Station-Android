package com.example.ev_station.api;

import com.example.ev_station.model.request.BookingRequest;
import com.example.ev_station.model.response.BookingPreviewResponse;
import com.example.ev_station.model.response.BookingResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Defines REST API endpoints for booking operations.
 */
public interface BookingApi {

    /**
     * Checks slot availability and estimated charges
     * without creating a booking.
     */
    @POST("api/bookings/preview")
    Call<BookingPreviewResponse> previewBooking(
            @Body BookingRequest request
    );

    /**
     * Creates a pending booking after server-side validation.
     */
    @POST("api/bookings")
    Call<BookingResponse> createBooking(
            @Body BookingRequest request
    );
}