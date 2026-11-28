package com.example.ev_station.repository;

import android.content.Context;

import com.example.ev_station.api.ApiClient;
import com.example.ev_station.api.BookingApi;
import com.example.ev_station.model.request.BookingRequest;
import com.example.ev_station.model.response.BookingPreviewResponse;
import com.example.ev_station.model.response.BookingResponse;

import retrofit2.Call;

/**
 * Repository responsible for booking data operations.
 */
public class BookingRepository {

    private final BookingApi bookingApi;

    public BookingRepository(Context context) {

        bookingApi = ApiClient
                .getClient(context.getApplicationContext())
                .create(BookingApi.class);
    }

    /**
     * Retrieves slot availability and estimated charges.
     */
    public Call<BookingPreviewResponse> previewBooking(
            BookingRequest request) {

        return bookingApi.previewBooking(request);
    }

    /**
     * Creates a pending booking.
     */
    public Call<BookingResponse> createBooking(
            BookingRequest request) {

        return bookingApi.createBooking(request);
    }
}