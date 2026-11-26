package com.example.ev_station.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ev_station.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import androidx.appcompat.widget.Toolbar;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Configure toolbar back navigation
        Toolbar mapToolbar = findViewById(R.id.mapToolbar);

        mapToolbar.setNavigationContentDescription(
                R.string.navigate_back
        );

        mapToolbar.setNavigationOnClickListener(
                view -> finish()
        );

        // Initialize the Google Maps fragment
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        // Set the initial camera position to Surat, Gujarat
        LatLng surat = new LatLng(21.1702, 72.8311);

        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(surat, 12f)
        );

        // Enable map zoom controls
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }
}
