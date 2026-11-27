package com.example.ev_station.adapter;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ev_station.R;
import com.example.ev_station.model.response.ChargingStationResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Displays nearby charging stations sorted by distance.
 */
public class NearbyStationAdapter extends
        RecyclerView.Adapter<NearbyStationAdapter.StationViewHolder> {

    private final List<NearbyStationItem> stationList =
            new ArrayList<>();

    private final OnStationActionListener listener;

    /**
     * Provides callbacks for station actions.
     */
    public interface OnStationActionListener {

        void onDirectionsClick(
                ChargingStationResponse station
        );

        void onDetailsClick(
                ChargingStationResponse station
        );
    }

    public NearbyStationAdapter(
            OnStationActionListener listener
    ) {

        this.listener = listener;
    }

    /**
     * Updates the station list and sorts stations
     * by their straight-line distance.
     */
    public void setStations(
            List<ChargingStationResponse> stations,
            double userLatitude,
            double userLongitude
    ) {

        stationList.clear();

        Set<Long> addedStationIds = new HashSet<>();

        if (stations != null) {

            for (ChargingStationResponse station : stations) {

                if (station == null || station.getId() == null) {
                    continue;
                }

                Double latitude = station.getLatitude();
                Double longitude = station.getLongitude();

                // Skip stations with invalid coordinates.
                if (latitude == null
                        || longitude == null
                        || !Double.isFinite(latitude)
                        || !Double.isFinite(longitude)
                        || latitude < -90
                        || latitude > 90
                        || longitude < -180
                        || longitude > 180) {

                    continue;
                }

                // Prevent duplicate station records.
                if (!addedStationIds.add(station.getId())) {
                    continue;
                }

                float[] distanceResult = new float[1];

                Location.distanceBetween(
                        userLatitude,
                        userLongitude,
                        latitude,
                        longitude,
                        distanceResult
                );

                double distanceKm =
                        distanceResult[0] / 1000.0;

                stationList.add(
                        new NearbyStationItem(
                                station,
                                distanceKm
                        )
                );
            }
        }

        // Display the nearest charging stations first.
        stationList.sort(
                Comparator.comparingDouble(
                        item -> item.distanceKm
                )
        );

        notifyDataSetChanged();
    }

    /**
     * Removes all currently displayed stations.
     */
    public void clearStations() {

        stationList.clear();

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_nearby_station,
                        parent,
                        false
                );

        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull StationViewHolder holder,
            int position
    ) {

        NearbyStationItem item =
                stationList.get(position);

        ChargingStationResponse station =
                item.station;

        Context context =
                holder.itemView.getContext();

        // Bind station operator name.
        holder.tvStationName.setText(
                getDisplayValue(
                        station.getCpoName(),
                        context.getString(
                                R.string.station_unknown_operator
                        )
                )
        );

        // Bind station address.
        holder.tvStationAddress.setText(
                getDisplayValue(
                        station.getLocation(),
                        context.getString(
                                R.string.station_address_unavailable
                        )
                )
        );

        // Bind calculated distance.
        holder.tvStationDistance.setText(
                context.getString(
                        R.string.nearby_distance_format,
                        item.distanceKm
                )
        );

        // Handle directions button.
        holder.btnDirections.setOnClickListener(view -> {

            if (listener != null) {
                listener.onDirectionsClick(station);
            }
        });

        // Handle station details button.
        holder.btnViewDetails.setOnClickListener(view -> {

            if (listener != null) {
                listener.onDetailsClick(station);
            }
        });
    }

    @Override
    public int getItemCount() {

        return stationList.size();
    }

    /**
     * Returns a fallback for missing station information.
     */
    private String getDisplayValue(
            String value,
            String fallback
    ) {

        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    /**
     * Holds a station and its calculated distance.
     */
    private static class NearbyStationItem {

        final ChargingStationResponse station;

        final double distanceKm;

        NearbyStationItem(
                ChargingStationResponse station,
                double distanceKm
        ) {

            this.station = station;
            this.distanceKm = distanceKm;
        }
    }

    /**
     * Holds the nearby station card views.
     */
    static class StationViewHolder
            extends RecyclerView.ViewHolder {

        final TextView tvStationName;
        final TextView tvStationAddress;
        final TextView tvStationDistance;

        final MaterialButton btnDirections;
        final MaterialButton btnViewDetails;

        StationViewHolder(@NonNull View itemView) {

            super(itemView);

            tvStationName = itemView.findViewById(
                    R.id.tvNearbyStationName
            );

            tvStationAddress = itemView.findViewById(
                    R.id.tvNearbyStationAddress
            );

            tvStationDistance = itemView.findViewById(
                    R.id.tvNearbyStationDistance
            );

            btnDirections = itemView.findViewById(
                    R.id.btnNearbyDirections
            );

            btnViewDetails = itemView.findViewById(
                    R.id.btnNearbyViewDetails
            );
        }
    }
}