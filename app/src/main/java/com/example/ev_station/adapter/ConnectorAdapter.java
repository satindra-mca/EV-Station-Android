package com.example.ev_station.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ev_station.R;
import com.example.ev_station.model.response.ConnectorResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays connectors belonging to the selected charger.
 */
public class ConnectorAdapter extends
        RecyclerView.Adapter<ConnectorAdapter.ConnectorViewHolder> {

    public interface OnConnectorClickListener {

        void onConnectorClick(ConnectorResponse connector);
    }

    private final List<ConnectorResponse> connectors =
            new ArrayList<>();

    private final OnConnectorClickListener listener;

    public ConnectorAdapter(
            OnConnectorClickListener listener
    ) {

        this.listener = listener;
    }

    public void setConnectors(
            List<ConnectorResponse> newConnectors
    ) {

        connectors.clear();

        if (newConnectors != null) {

            for (ConnectorResponse connector : newConnectors) {

                if (connector != null) {
                    connectors.add(connector);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConnectorViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_connector,
                        parent,
                        false
                );

        return new ConnectorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ConnectorViewHolder holder,
            int position
    ) {

        ConnectorResponse connector =
                connectors.get(position);

        if (connector.getConnectorNumber() != null) {

            holder.tvConnectorNumber.setText(
                    holder.itemView.getContext().getString(
                            R.string.connector_number_format,
                            connector.getConnectorNumber()
                    )
            );

        } else {

            holder.tvConnectorNumber.setText(
                    R.string.station_details_unknown
            );
        }

        String status = connector.getStatus();

        if (status == null || status.trim().isEmpty()) {

            status = holder.itemView.getContext().getString(
                    R.string.station_details_unknown
            );
        }

        holder.tvConnectorStatus.setText(
                holder.itemView.getContext().getString(
                        R.string.connector_status_format,
                        status
                )
        );

        boolean available =
                "AVAILABLE".equalsIgnoreCase(
                        connector.getStatus()
                );

        boolean occupied =
                "OCCUPIED".equalsIgnoreCase(
                        connector.getStatus()
                );

        int statusColor = available
                ? R.color.ev_success
                : occupied
                ? R.color.ev_warning
                : R.color.ev_text_secondary;

        holder.tvConnectorStatus.setTextColor(
                ContextCompat.getColor(
                        holder.itemView.getContext(),
                        statusColor
                )
        );

        holder.btnSelectConnector.setEnabled(
                isSelectable(connector)
        );

        holder.btnSelectConnector.setOnClickListener(view -> {

            int currentPosition =
                    holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            ConnectorResponse selected =
                    connectors.get(currentPosition);

            if (listener != null
                    && isSelectable(selected)) {

                listener.onConnectorClick(selected);
            }
        });
    }

    /**
     * Allows connectors with supported statuses
     * to be selected for future slot booking.
     */
    private boolean isSelectable(
            ConnectorResponse connector
    ) {

        return connector != null
                && connector.getId() != null
                && connector.getId() > 0
                && connector.getConnectorNumber() != null
                && connector.getConnectorNumber() > 0
                && (
                "AVAILABLE".equalsIgnoreCase(
                        connector.getStatus()
                )
                        || "OCCUPIED".equalsIgnoreCase(
                        connector.getStatus()
                )
        );
    }

    @Override
    public int getItemCount() {

        return connectors.size();
    }

    static class ConnectorViewHolder
            extends RecyclerView.ViewHolder {

        final TextView tvConnectorNumber;
        final TextView tvConnectorStatus;

        final MaterialButton btnSelectConnector;

        ConnectorViewHolder(@NonNull View itemView) {

            super(itemView);

            tvConnectorNumber = itemView.findViewById(
                    R.id.tvConnectorNumber
            );

            tvConnectorStatus = itemView.findViewById(
                    R.id.tvConnectorStatus
            );

            btnSelectConnector = itemView.findViewById(
                    R.id.btnSelectConnector
            );
        }
    }
}