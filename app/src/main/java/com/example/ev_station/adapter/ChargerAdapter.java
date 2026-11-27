package com.example.ev_station.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ev_station.R;
import com.example.ev_station.model.response.ChargerResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays chargers belonging to the selected station.
 */
public class ChargerAdapter
        extends RecyclerView.Adapter<ChargerAdapter.ChargerViewHolder> {

    public interface OnChargerClickListener {

        void onChargerClick(ChargerResponse charger);
    }

    private final List<ChargerResponse> chargers =
            new ArrayList<>();

    private final OnChargerClickListener listener;

    public ChargerAdapter(OnChargerClickListener listener) {

        this.listener = listener;
    }

    public void setChargers(List<ChargerResponse> newChargers) {

        chargers.clear();

        if (newChargers != null) {

            for (ChargerResponse charger : newChargers) {

                if (charger != null) {
                    chargers.add(charger);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChargerViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_charger,
                        parent,
                        false
                );

        return new ChargerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ChargerViewHolder holder,
            int position) {

        ChargerResponse charger = chargers.get(position);

        holder.tvChargerType.setText(
                safeText(holder.itemView, charger.getChargerType())
        );

        holder.tvChargerRating.setText(
                holder.itemView.getContext().getString(
                        R.string.charger_power_format,
                        safeText(
                                holder.itemView,
                                charger.getChargerRating()
                        )
                )
        );

        holder.tvConnectorRating.setText(
                holder.itemView.getContext().getString(
                        R.string.charger_connector_rating_format,
                        safeText(
                                holder.itemView,
                                charger.getConnectorRating()
                        )
                )
        );

        if (charger.getNoOfConnector() != null) {

            holder.tvConnectorCount.setText(
                    holder.itemView.getContext().getString(
                            R.string.charger_connector_count_format,
                            charger.getNoOfConnector()
                    )
            );

        } else {

            holder.tvConnectorCount.setText(
                    R.string.station_details_unknown
            );
        }

        if (charger.getPricePerKwh() != null) {

            holder.tvChargerPrice.setText(
                    holder.itemView.getContext().getString(
                            R.string.charger_price_format,
                            charger.getPricePerKwh().toPlainString()
                    )
            );

        } else {

            holder.tvChargerPrice.setText(
                    R.string.charger_price_unavailable
            );
        }

        holder.btnSelectCharger.setEnabled(
                charger.getId() != null
                        && charger.getId() > 0
        );

        holder.btnSelectCharger.setOnClickListener(view -> {

            int currentPosition =
                    holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            ChargerResponse selected =
                    chargers.get(currentPosition);

            if (listener != null
                    && selected.getId() != null
                    && selected.getId() > 0) {

                listener.onChargerClick(selected);
            }
        });
    }

    @Override
    public int getItemCount() {

        return chargers.size();
    }

    private String safeText(View view, String value) {

        if (value == null || value.trim().isEmpty()) {

            return view.getContext().getString(
                    R.string.station_details_unknown
            );
        }

        return value;
    }

    static class ChargerViewHolder
            extends RecyclerView.ViewHolder {

        final TextView tvChargerType;
        final TextView tvChargerPrice;
        final TextView tvChargerRating;
        final TextView tvConnectorRating;
        final TextView tvConnectorCount;

        final MaterialButton btnSelectCharger;

        ChargerViewHolder(@NonNull View itemView) {

            super(itemView);

            tvChargerType = itemView.findViewById(
                    R.id.tvChargerType
            );

            tvChargerPrice = itemView.findViewById(
                    R.id.tvChargerPrice
            );

            tvChargerRating = itemView.findViewById(
                    R.id.tvChargerRating
            );

            tvConnectorRating = itemView.findViewById(
                    R.id.tvConnectorRating
            );

            tvConnectorCount = itemView.findViewById(
                    R.id.tvConnectorCount
            );

            btnSelectCharger = itemView.findViewById(
                    R.id.btnSelectCharger
            );
        }
    }
}