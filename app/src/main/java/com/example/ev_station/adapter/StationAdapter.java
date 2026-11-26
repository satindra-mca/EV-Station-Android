
package com.example.ev_station.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ev_station.R;
import com.example.ev_station.model.response.ChargingStationResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter responsible for displaying
 * charging station information returned by the backend.
 */
public class StationAdapter
        extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {

    private final List<ChargingStationResponse> stationList =
            new ArrayList<>();

    /**
     * Replaces the currently displayed station records.
     *
     * This method must be called on the main UI thread.
     */
    public void setStations(
            List<ChargingStationResponse> stations
    ) {

        stationList.clear();

        if (stations != null) {

            for (ChargingStationResponse station : stations) {

                if (station != null) {
                    stationList.add(station);
                }
            }
        }

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
                        R.layout.item_station,
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

        ChargingStationResponse station =
                stationList.get(position);

        Context context = holder.itemView.getContext();

        // Bind charging point operator.
        holder.tvStationCpoName.setText(
                getDisplayValue(
                        station.getCpoName(),
                        context.getString(
                                R.string.station_unknown_operator
                        )
                )
        );

        // Bind city, district and state.
        holder.tvStationCity.setText(
                buildStationCity(station, context)
        );

        // Bind station address.
        holder.tvStationLocation.setText(
                getDisplayValue(
                        station.getLocation(),
                        context.getString(
                                R.string.station_address_unavailable
                        )
                )
        );

        // Bind government or private ownership.
        holder.tvStationOwnership.setText(
                getDisplayValue(
                        station.getGovtPrivate(),
                        context.getString(
                                R.string.station_ownership_unknown
                        )
                )
        );

        // Bind the operational status of the station.
        bindStationStatus(
                holder,
                station.getStatus(),
                context
        );
    }

    @Override
    public int getItemCount() {

        return stationList.size();
    }

    /**
     * Returns a fallback when a backend field is
     * null, empty or contains only whitespace.
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
     * Builds a readable city, district and state label
     * without displaying null or empty values.
     */
    private String buildStationCity(
            ChargingStationResponse station,
            Context context
    ) {

        List<String> locationParts = new ArrayList<>();

        addLocationPart(
                locationParts,
                station.getCityVillage()
        );

        addLocationPart(
                locationParts,
                station.getDistrict()
        );

        addLocationPart(
                locationParts,
                station.getState()
        );

        if (locationParts.isEmpty()) {

            return context.getString(
                    R.string.station_location_unavailable
            );
        }

        return android.text.TextUtils.join(
                ", ",
                locationParts
        );
    }

    /**
     * Adds a non-empty location component while
     * avoiding duplicate location names.
     */
    private void addLocationPart(
            List<String> parts,
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return;
        }

        String cleanedValue = value.trim();

        for (String existingPart : parts) {

            if (existingPart.equalsIgnoreCase(cleanedValue)) {
                return;
            }
        }

        parts.add(cleanedValue);
    }

    /**
     * Displays the station's operational status.
     *
     * An active station does not necessarily have
     * an available charging connector.
     */
    private void bindStationStatus(
            StationViewHolder holder,
            String status,
            Context context
    ) {

        String normalizedStatus =
                status == null ? "" : status.trim();

        if ("ACTIVE".equalsIgnoreCase(normalizedStatus)) {

            holder.tvStationStatus.setText(
                    R.string.station_active
            );

            holder.tvStationStatus.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ev_success
                    )
            );

        } else if ("INACTIVE".equalsIgnoreCase(normalizedStatus)) {

            holder.tvStationStatus.setText(
                    R.string.station_inactive
            );

            holder.tvStationStatus.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ev_error
                    )
            );

        } else {

            holder.tvStationStatus.setText(
                    getDisplayValue(
                            status,
                            context.getString(
                                    R.string.station_status_unknown
                            )
                    )
            );

            holder.tvStationStatus.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.ev_text_secondary
                    )
            );
        }
    }

    /**
     * Holds references to the views of a station item.
     */
    static class StationViewHolder
            extends RecyclerView.ViewHolder {

        final TextView tvStationCpoName;
        final TextView tvStationCity;
        final TextView tvStationLocation;
        final TextView tvStationStatus;
        final TextView tvStationOwnership;

        StationViewHolder(@NonNull View itemView) {

            super(itemView);

            tvStationCpoName = itemView.findViewById(
                    R.id.tvStationCpoName
            );

            tvStationCity = itemView.findViewById(
                    R.id.tvStationCity
            );

            tvStationLocation = itemView.findViewById(
                    R.id.tvStationLocation
            );

            tvStationStatus = itemView.findViewById(
                    R.id.tvStationStatus
            );

            tvStationOwnership = itemView.findViewById(
                    R.id.tvStationOwnership
            );
        }
    }
}
