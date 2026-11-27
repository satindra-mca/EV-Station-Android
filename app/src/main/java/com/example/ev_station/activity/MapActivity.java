package com.example.ev_station.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.ev_station.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import com.example.ev_station.session.SessionManager;
import com.example.ev_station.utils.SessionNavigator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

import com.example.ev_station.model.response.ChargingStationResponse;
import com.example.ev_station.repository.StationRepository;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.ArrayList;

import android.content.ActivityNotFoundException;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ev_station.adapter.NearbyStationAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap googleMap;

    private static final String TAG = "MapActivity";

    private FusedLocationProviderClient fusedLocationClient;

    private CancellationTokenSource locationCancellationToken;

    private Double currentLatitude;
    private Double currentLongitude;

    private boolean isLocationRequestInProgress = false;
    private boolean isActivityResumed = false;

    private static final float CURRENT_LOCATION_ZOOM = 15f;
    private static final long LOCATION_TIMEOUT_MS = 15000L;

    private boolean permissionRequested = false;

    // Default nearby station search radius in kilometers.
    private static final double NEARBY_RADIUS_KM = 10.0;

    // Repository for charging station API requests.
    private StationRepository stationRepository;

    // Track the active nearby station API request.
    private Call<List<ChargingStationResponse>> nearbyStationsCall;

    // Prevent duplicate nearby station requests.
    private boolean isNearbyRequestInProgress = false;

    private static final String KEY_PERMISSION_REQUESTED =
            "permission_requested";

    // Secure session management.
    private SessionManager sessionManager;

    private final ExecutorService sessionExecutor =
            Executors.newSingleThreadExecutor();

    private boolean isSessionValid = false;
    private boolean navigatingToLogin = false;

    private int validationGeneration = 0;

    // Nearby station list UI components.
    private RecyclerView rvNearbyStations;
    private TextView tvNearbyStationCount;
    private TextView tvNearbyMessage;
    private ProgressBar nearbyProgressBar;

    private NearbyStationAdapter nearbyStationAdapter;
    private BottomSheetBehavior<View> nearbySheetBehavior;

    // Track station markers for safe removal and refresh.
    private final List<Marker> stationMarkers = new ArrayList<>();

    // Request foreground location permissions.
    private final ActivityResultLauncher<String[]>
            locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {

                if (hasLocationPermission()) {
                    enableMyLocation();
                } else {
                    handleLocationPermissionDenied();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initializeNearbyStationsUi();

        // Initialize secure session management.
        sessionManager = new SessionManager(this);

        // Initialize the charging station repository.
        stationRepository = new StationRepository(this);

        // Initialize the foreground location provider.
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        if (savedInstanceState != null) {
            permissionRequested = savedInstanceState.getBoolean(
                    KEY_PERMISSION_REQUESTED, false
            );
        }

        // Configure toolbar navigation.
        Toolbar mapToolbar = findViewById(R.id.mapToolbar);

        mapToolbar.setNavigationContentDescription(
                R.string.navigate_back
        );

        mapToolbar.setNavigationOnClickListener(
                view -> finish()
        );

        // Initialize Google Maps asynchronously.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // Initialize the expandable nearby station list.
    private void initializeNearbyStationsUi() {

        rvNearbyStations = findViewById(
                R.id.rvNearbyStations
        );

        tvNearbyStationCount = findViewById(
                R.id.tvNearbyStationCount
        );

        tvNearbyMessage = findViewById(
                R.id.tvNearbyMessage
        );

        nearbyProgressBar = findViewById(
                R.id.nearbyProgressBar
        );

        View bottomSheet = findViewById(
                R.id.nearbyBottomSheet
        );

        View sheetHandle = findViewById(
                R.id.nearbySheetHandle
        );

        nearbySheetBehavior =
                BottomSheetBehavior.from(bottomSheet);

        nearbySheetBehavior.setHideable(false);
        nearbySheetBehavior.setFitToContents(true);

        nearbySheetBehavior.setState(
                BottomSheetBehavior.STATE_COLLAPSED
        );

        // Expand or collapse the list using the drag handle.
        sheetHandle.setOnClickListener(view -> {

            if (nearbySheetBehavior.getState()
                    == BottomSheetBehavior.STATE_EXPANDED) {

                nearbySheetBehavior.setState(
                        BottomSheetBehavior.STATE_COLLAPSED
                );

            } else {

                nearbySheetBehavior.setState(
                        BottomSheetBehavior.STATE_EXPANDED
                );
            }
        });

        nearbyStationAdapter = new NearbyStationAdapter(
                new NearbyStationAdapter.OnStationActionListener() {

                    @Override
                    public void onDirectionsClick(
                            ChargingStationResponse station
                    ) {

                        openStationDirections(station);
                    }

                    @Override
                    public void onDetailsClick(
                            ChargingStationResponse station
                    ) {

                        openStationDetails(station);
                    }
                }
        );

        rvNearbyStations.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvNearbyStations.setAdapter(
                nearbyStationAdapter
        );

        rvNearbyStations.setHasFixedSize(true);
    }

    // Display the nearby station loading state.
    private void showNearbyLoading() {

        nearbyProgressBar.setVisibility(View.VISIBLE);

        tvNearbyMessage.setVisibility(View.GONE);

        tvNearbyStationCount.setText(
                R.string.nearby_loading
        );

        rvNearbyStations.setVisibility(View.GONE);
    }


    // Display an empty or unsuccessful request state.
    private void showNearbyMessage(int messageResId) {

        nearbyProgressBar.setVisibility(View.GONE);

        nearbyStationAdapter.clearStations();

        rvNearbyStations.setVisibility(View.GONE);

        tvNearbyStationCount.setText("");

        tvNearbyMessage.setText(messageResId);

        tvNearbyMessage.setVisibility(View.VISIBLE);
    }


    // Display the retrieved nearby charging stations.
    private void showNearbyStations(
            List<ChargingStationResponse> stations
    ) {

        if (currentLatitude == null
                || currentLongitude == null) {
            return;
        }

        nearbyProgressBar.setVisibility(View.GONE);

        nearbyStationAdapter.setStations(
                stations,
                currentLatitude,
                currentLongitude
        );

        int stationCount =
                nearbyStationAdapter.getItemCount();

        if (stationCount == 0) {

            showNearbyMessage(
                    R.string.no_stations_found
            );

            return;
        }

        tvNearbyMessage.setVisibility(View.GONE);

        rvNearbyStations.setVisibility(View.VISIBLE);

        tvNearbyStationCount.setText(
                getString(
                        R.string.nearby_station_count,
                        stationCount,
                        NEARBY_RADIUS_KM
                )
        );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        this.googleMap = googleMap;

        // Display Surat as the default map location.
        LatLng surat = new LatLng(21.1702, 72.8311);

        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(surat, 12f)
        );

        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Keep map controls above the collapsed station list.
        if (nearbySheetBehavior != null) {

            googleMap.setPadding(
                    0,
                    0,
                    0,
                    nearbySheetBehavior.getPeekHeight()
            );
        }

        // Start location setup only after session validation.
        if (isSessionValid
                && isActivityResumed
                && !navigatingToLogin
                && !isFinishing()
                && !isDestroyed()) {

            checkLocationPermission();
        }
    }

    // Validate the encrypted session on a background thread.
    private void validateSession() {

        if (!isActivityResumed
                || isFinishing()
                || isDestroyed()
                || navigatingToLogin
                || sessionExecutor.isShutdown()) {
            return;
        }

        isSessionValid = false;

        final int generation = ++validationGeneration;

        sessionExecutor.execute(() -> {

            // getRole() also validates the stored session and JWT expiry.
            String role = sessionManager.getRole();

            runOnUiThread(() -> {

                // Ignore outdated validation results.
                if (generation != validationGeneration
                        || !isActivityResumed
                        || isFinishing()
                        || isDestroyed()
                        || navigatingToLogin) {
                    return;
                }

                if (role == null) {

                    navigateToLogin();

                    return;
                }

                // Restrict this screen to the USER role.
                if (!"USER".equals(role)) {

                    Toast.makeText(
                            this,
                            R.string.station_access_denied,
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();

                    return;
                }

                isSessionValid = true;

                // Continue location setup after session validation.
                if (googleMap != null) {
                    checkLocationPermission();
                }
            });
        });
    }

    // Redirect unauthenticated users to the login screen.
    private void navigateToLogin() {

        if (navigatingToLogin
                || isFinishing()
                || isDestroyed()) {
            return;
        }

        navigatingToLogin = true;
        isSessionValid = false;

        // Invalidate pending session validation results.
        validationGeneration++;

        // Cancel any active location request.
        cancelCurrentLocationRequest();

        // Cancel any active nearby stations request.
        cancelNearbyStationsRequest();

        // Clear cached location.
        currentLatitude = null;
        currentLongitude = null;

        Toast.makeText(
                this,
                R.string.station_session_expired,
                Toast.LENGTH_SHORT
        ).show();

        // Clear the previous activity stack and open login.
        SessionNavigator.navigateToLogin(this);
    }

    // Check whether authenticated location operations are allowed.
    private boolean canAccessLocation() {

        return isSessionValid
                && isActivityResumed
                && !navigatingToLogin
                && !isFinishing()
                && !isDestroyed();
    }

    // Accept either precise or approximate location access.
    private boolean hasLocationPermission() {

        boolean fineLocationGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocationGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        return fineLocationGranted || coarseLocationGranted;
    }

    // Check whether device location services are enabled.
    private boolean isLocationServiceEnabled() {

        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            return false;
        }

        return locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
        ) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    // Retrieve a single current location fix.
    private void fetchCurrentLocation() {

        if (!canAccessLocation()
                || googleMap == null
                || fusedLocationClient == null
                || isLocationRequestInProgress
                || !hasLocationPermission()) {
            return;
        }

        // Verify that device location services are enabled.
        if (!isLocationServiceEnabled()) {

            showNearbyMessage(
                    R.string.location_services_disabled
            );

            Toast.makeText(
                    this,
                    R.string.location_services_disabled,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Select location priority based on granted permission.
        boolean hasFinePermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        int priority = hasFinePermission
                ? Priority.PRIORITY_HIGH_ACCURACY
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;

        CurrentLocationRequest locationRequest =
                new CurrentLocationRequest.Builder()
                        .setPriority(priority)
                        .setDurationMillis(LOCATION_TIMEOUT_MS)
                        .setMaxUpdateAgeMillis(0L)
                        .build();

        isLocationRequestInProgress = true;

        CancellationTokenSource requestToken =
                new CancellationTokenSource();

        locationCancellationToken = requestToken;

        try {

            fusedLocationClient.getCurrentLocation(
                    locationRequest,
                    requestToken.getToken()
            ).addOnCompleteListener(this, task -> {

                // Ignore callbacks from cancelled or outdated requests.
                if (locationCancellationToken != requestToken) {
                    return;
                }

                isLocationRequestInProgress = false;
                locationCancellationToken = null;

                if (!canAccessLocation()
                        || !hasLocationPermission()
                        || task.isCanceled()) {
                    return;
                }

                if (!task.isSuccessful()) {

                    Log.w(
                            TAG,
                            "Current location retrieval failed",
                            task.getException()
                    );

                    Toast.makeText(
                            this,
                            R.string.current_location_failed,
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                Location location = task.getResult();

                if (location == null) {

                    Toast.makeText(
                            this,
                            R.string.current_location_unavailable,
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                handleCurrentLocation(location);


            });

        } catch (SecurityException exception) {

            cancelCurrentLocationRequest();

            Log.w(TAG, "Location permission unavailable", exception);

            Toast.makeText(
                    this,
                    R.string.location_access_unavailable,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Store current coordinates and update the map camera.
    private void handleCurrentLocation(Location location) {

        if (!canAccessLocation() || !hasLocationPermission()) {
            return;
        }

        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        if (googleMap == null) {
            return;
        }

        LatLng currentPosition = new LatLng(
                currentLatitude,
                currentLongitude
        );

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        currentPosition,
                        CURRENT_LOCATION_ZOOM
                )
        );

        Log.d(TAG, "Current location retrieved successfully");

        // Retrieve nearby charging stations using current coordinates.
        fetchNearbyStations();
    }

    // Retrieve nearby charging stations after session validation.
    private void fetchNearbyStations() {

        if (!canAccessLocation()
                || !hasLocationPermission()
                || stationRepository == null
                || currentLatitude == null
                || currentLongitude == null
                || isNearbyRequestInProgress
                || sessionExecutor.isShutdown()) {
            return;
        }

        isNearbyRequestInProgress = true;

        final double latitude = currentLatitude;
        final double longitude = currentLongitude;
        final int generation = validationGeneration;

        // Revalidate the session before accessing the protected API.
        sessionExecutor.execute(() -> {

            String role = sessionManager.getRole();

            runOnUiThread(() -> {

                // Ignore outdated session validation results.
                if (generation != validationGeneration
                        || !canAccessLocation()) {
                    return;
                }

                if (role == null) {

                    isNearbyRequestInProgress = false;

                    navigateToLogin();

                    return;
                }

                if (!"USER".equals(role)) {

                    isNearbyRequestInProgress = false;
                    isSessionValid = false;

                    Toast.makeText(
                            this,
                            R.string.station_access_denied,
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();

                    return;
                }

                if (!hasLocationPermission()) {

                    isNearbyRequestInProgress = false;

                    return;
                }

                executeNearbyStationsRequest(
                        latitude,
                        longitude
                );
            });
        });
    }


    // Execute the authenticated nearby charging station request.
    private void executeNearbyStationsRequest(

            double latitude,
            double longitude
    ) {
        showNearbyLoading();

        nearbyStationsCall =
                stationRepository.getNearbyStations(
                        latitude,
                        longitude,
                        NEARBY_RADIUS_KM
                );

        nearbyStationsCall.enqueue(
                new Callback<List<ChargingStationResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<ChargingStationResponse>> call,
                            @NonNull Response<List<ChargingStationResponse>> response
                    ) {

                        if (call != nearbyStationsCall
                                || call.isCanceled()) {
                            return;
                        }

                        nearbyStationsCall = null;
                        isNearbyRequestInProgress = false;

                        if (!canAccessLocation()
                                || !hasLocationPermission()) {
                            return;
                        }

                        if (response.isSuccessful()) {

                            handleNearbyStationsResponse(
                                    response.body()
                            );

                        } else {

                            handleNearbyHttpError(
                                    response.code()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<ChargingStationResponse>> call,
                            @NonNull Throwable throwable
                    ) {

                        if (call != nearbyStationsCall
                                || call.isCanceled()) {
                            return;
                        }

                        nearbyStationsCall = null;
                        isNearbyRequestInProgress = false;

                        if (!canAccessLocation()
                                || !hasLocationPermission()) {
                            return;
                        }

                        handleNearbyNetworkFailure(throwable);
                    }
                }
        );
    }


    // Handle network failures while retrieving nearby stations.
    private void handleNearbyNetworkFailure(Throwable throwable) {

        if (!canAccessLocation()) {
            return;
        }

        clearStationMarkers();

        if (throwable instanceof SocketTimeoutException) {

            showNearbyMessage(
                    R.string.api_timeout_error
            );

        } else if (throwable instanceof IOException) {

            showNearbyMessage(
                    R.string.api_network_error
            );

        } else {

            showNearbyMessage(
                    R.string.api_unexpected_error
            );
        }

        if (throwable instanceof SocketTimeoutException) {

            Toast.makeText(
                    this,
                    R.string.api_timeout_error,
                    Toast.LENGTH_SHORT
            ).show();

            Log.w(TAG, "Nearby stations request timed out");

        } else if (throwable instanceof IOException) {

            Toast.makeText(
                    this,
                    R.string.api_network_error,
                    Toast.LENGTH_SHORT
            ).show();

            Log.w(TAG, "Nearby stations network failure");

        } else {

            Toast.makeText(
                    this,
                    R.string.api_unexpected_error,
                    Toast.LENGTH_SHORT
            ).show();

            Log.w(TAG, "Unexpected nearby stations request failure");
        }
    }

    // Cancel nearby requests and clear the associated UI.
    private void cancelNearbyStationsRequest() {

        Call<List<ChargingStationResponse>> activeCall =
                nearbyStationsCall;

        nearbyStationsCall = null;

        isNearbyRequestInProgress = false;

        if (activeCall != null) {
            activeCall.cancel();
        }

        clearStationMarkers();

        if (nearbyStationAdapter != null) {
            nearbyStationAdapter.clearStations();
        }

        if (nearbyProgressBar != null) {
            nearbyProgressBar.setVisibility(View.GONE);
        }

        if (tvNearbyStationCount != null) {
            tvNearbyStationCount.setText("");
        }

        if (tvNearbyMessage != null) {
            tvNearbyMessage.setVisibility(View.GONE);
        }

        if (rvNearbyStations != null) {
            rvNearbyStations.setVisibility(View.GONE);
        }
    }

    // Handle the nearby charging station API response.
    private void handleNearbyStationsResponse(
            List<ChargingStationResponse> stations
    ) {

        if (!canAccessLocation()
                || !hasLocationPermission()) {
            return;
        }

        clearStationMarkers();

        if (stations == null) {

            showNearbyMessage(
                    R.string.station_invalid_response
            );

            Log.w(TAG, "Nearby stations response is null");

            return;
        }

        if (stations.isEmpty()) {

            showNearbyMessage(
                    R.string.no_stations_found
            );

            Log.d(TAG, "No nearby charging stations found");

            return;
        }

        Log.d(
                TAG,
                "Nearby charging stations retrieved: "
                        + stations.size()
        );

        // Populate both map markers and the station list.
        displayStationMarkers(stations);

        showNearbyStations(stations);
    }

    // Display nearby charging stations on Google Maps.
    private void displayStationMarkers(
            List<ChargingStationResponse> stations
    ) {

        if (!canAccessLocation()
                || !hasLocationPermission()
                || googleMap == null) {
            return;
        }

        for (ChargingStationResponse station : stations) {

            if (station == null || station.getId() == null) {
                continue;
            }

            Double latitude = station.getLatitude();
            Double longitude = station.getLongitude();

            // Skip stations with missing or invalid coordinates.
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

            String stationName = station.getCpoName();

            if (stationName == null
                    || stationName.trim().isEmpty()) {

                stationName = getString(
                        R.string.station_unknown_operator
                );
            }

            String stationAddress = station.getLocation();

            if (stationAddress == null
                    || stationAddress.trim().isEmpty()) {

                stationAddress = getString(
                        R.string.station_address_unavailable
                );
            }

            LatLng stationPosition = new LatLng(
                    latitude,
                    longitude
            );

            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(stationPosition)
                            .title(stationName)
                            .snippet(stationAddress)
                            .icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_GREEN
                                    )
                            )
            );

            if (marker != null) {

                // Store station ID for future navigation.
                marker.setTag(station.getId());

                stationMarkers.add(marker);
            }
        }

        Log.d(
                TAG,
                "Station markers displayed: "
                        + stationMarkers.size()
        );
    }

    // Remove previously displayed station markers.
    private void clearStationMarkers() {

        for (Marker marker : stationMarkers) {

            if (marker != null) {
                marker.remove();
            }
        }

        stationMarkers.clear();
    }

    // Handle HTTP errors returned by the nearby stations API.
    private void handleNearbyHttpError(int statusCode) {

        if (!canAccessLocation()) {
            return;
        }

        clearStationMarkers();

        if (statusCode == 401) {

            showNearbyMessage(
                    R.string.station_unauthorized
            );

        } else if (statusCode == 403) {

            showNearbyMessage(
                    R.string.station_access_denied
            );

        } else {

            showNearbyMessage(
                    R.string.station_load_error
            );
        }

        Log.w(TAG, "Nearby stations HTTP error: " + statusCode);

        if (statusCode == 401) {

            handleNearbyUnauthorized();

        } else if (statusCode == 403) {

            Toast.makeText(
                    this,
                    R.string.station_access_denied,
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    R.string.station_load_error,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Revalidate the current session after an HTTP 401 response.
    private void handleNearbyUnauthorized() {

        if (!canAccessLocation()
                || sessionExecutor.isShutdown()) {
            return;
        }

        isNearbyRequestInProgress = true;

        final int generation = validationGeneration;

        sessionExecutor.execute(() -> {

            String role = sessionManager.getRole();

            runOnUiThread(() -> {

                // Ignore outdated session validation results.
                if (generation != validationGeneration
                        || !canAccessLocation()) {
                    return;
                }

                isNearbyRequestInProgress = false;

                if (role == null) {

                    navigateToLogin();

                } else if (!"USER".equals(role)) {

                    isSessionValid = false;

                    Toast.makeText(
                            this,
                            R.string.station_access_denied,
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();

                } else {

                    // Preserve a potentially newer valid session.
                    Toast.makeText(
                            this,
                            R.string.station_unauthorized,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });
    }

    // Cancel any pending location request.
    private void cancelCurrentLocationRequest() {

        CancellationTokenSource token = locationCancellationToken;

        locationCancellationToken = null;
        isLocationRequestInProgress = false;

        if (token != null) {
            token.cancel();
        }
    }

    // Check permission before enabling location features.
    private void checkLocationPermission() {

        if (!canAccessLocation()) {
            return;
        }

        // Disable location features if permission has been revoked.
        if (!hasLocationPermission() && googleMap != null) {

            cancelCurrentLocationRequest();

            validationGeneration++;
            cancelNearbyStationsRequest();

            currentLatitude = null;
            currentLongitude = null;

            try {

                googleMap.setMyLocationEnabled(false);

            } catch (SecurityException exception) {

                Log.w(TAG, "Unable to disable location layer", exception);
            }

            googleMap.getUiSettings()
                    .setMyLocationButtonEnabled(false);
        }

        if (hasLocationPermission()) {
            enableMyLocation();
            return;
        }

        // Avoid repeated permission requests after recreation.
        if (permissionRequested) {
            return;
        }

        if (shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION)
                || shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_COARSE_LOCATION)) {

            showPermissionRationale();

        } else {
            requestLocationPermission();
        }
    }

    // Request both permissions for Android 12+ compatibility.
    private void requestLocationPermission() {

        permissionRequested = true;

        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    // Explain why location access is required.
    private void showPermissionRationale() {

        if (!canAccessLocation()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.location_permission_title)
                .setMessage(R.string.location_permission_rationale)
                .setPositiveButton(
                        R.string.location_permission_allow,
                        (dialog, which) ->
                                requestLocationPermission()
                )
                .setNegativeButton(
                        R.string.location_permission_not_now,
                        (dialog, which) -> {
                            permissionRequested = true;
                            dialog.dismiss();
                        }
                )
                .show();
    }

    // Enable the Google Maps location layer safely.
    private void enableMyLocation() {

        if (!canAccessLocation()
                || googleMap == null
                || !hasLocationPermission()) {
            return;
        }

        try {

            googleMap.setMyLocationEnabled(true);

            // Retrieve current coordinates when they are not available.
            if (currentLatitude == null || currentLongitude == null) {
                fetchCurrentLocation();
            }

            googleMap.getUiSettings()
                    .setMyLocationButtonEnabled(true);

        } catch (SecurityException exception) {

            Toast.makeText(
                    this,
                    R.string.location_access_unavailable,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Handle denied and permanently denied permissions.
    private void handleLocationPermissionDenied() {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        boolean canRequestAgain =
                shouldShowRequestPermissionRationale(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) || shouldShowRequestPermissionRationale(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                );

        if (canRequestAgain) {

            Toast.makeText(
                    this,
                    R.string.location_permission_denied,
                    Toast.LENGTH_LONG
            ).show();

        } else {

            showLocationSettingsDialog();
        }
    }

    // Allow the user to grant permission from app settings.
    private void showLocationSettingsDialog() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.location_permission_title)
                .setMessage(
                        R.string.location_permission_settings_message
                )
                .setPositiveButton(
                        R.string.location_permission_open_settings,
                        (dialog, which) -> {

                            Intent intent = new Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            );

                            intent.setData(
                                    Uri.fromParts(
                                            "package",
                                            getPackageName(),
                                            null
                                    )
                            );

                            startActivity(intent);
                        }
                )
                .setNegativeButton(
                        R.string.location_permission_not_now,
                        (dialog, which) -> dialog.dismiss()
                )
                .show();
    }

    // Open external navigation for the selected station.
    private void openStationDirections(
            ChargingStationResponse station
    ) {

        if (!canAccessLocation()
                || !hasLocationPermission()
                || station == null) {
            return;
        }

        Double latitude = station.getLatitude();
        Double longitude = station.getLongitude();

        if (latitude == null
                || longitude == null
                || !Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90
                || latitude > 90
                || longitude < -180
                || longitude > 180) {

            return;
        }

        Uri navigationUri = Uri.parse(
                "google.navigation:q="
                        + latitude
                        + ","
                        + longitude
        );

        Intent navigationIntent = new Intent(
                Intent.ACTION_VIEW,
                navigationUri
        );

        navigationIntent.setPackage(
                "com.google.android.apps.maps"
        );

        try {

            startActivity(navigationIntent);

        } catch (ActivityNotFoundException exception) {

            // Fall back to browser-based directions.
            Uri browserUri = Uri.parse(
                            "https://www.google.com/maps/dir/"
                    ).buildUpon()
                    .appendQueryParameter("api", "1")
                    .appendQueryParameter(
                            "destination",
                            latitude + "," + longitude
                    )
                    .appendQueryParameter(
                            "travelmode",
                            "driving"
                    )
                    .build();

            try {

                startActivity(
                        new Intent(
                                Intent.ACTION_VIEW,
                                browserUri
                        )
                );

            } catch (ActivityNotFoundException fallbackException) {

                Toast.makeText(
                        this,
                        R.string.nearby_directions_unavailable,
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    // Handle station details selection.
    private void openStationDetails(
            ChargingStationResponse station
    ) {

        if (!canAccessLocation()
                || station == null
                || station.getId() == null) {
            return;
        }

        Log.d(
                TAG,
                "Selected station ID: " + station.getId()
        );

        Toast.makeText(
                this,
                R.string.nearby_details_pending,
                Toast.LENGTH_SHORT
        ).show();

        // StationDetailsActivity navigation will be added in Phase 5.
    }

    @Override
    protected void onResume() {

        super.onResume();

        isActivityResumed = true;

        // Revalidate the session whenever the activity resumes.
        validateSession();
    }

    @Override
    protected void onPause() {

        isActivityResumed = false;
        isSessionValid = false;

        validationGeneration++;

        cancelCurrentLocationRequest();
        cancelNearbyStationsRequest();

        currentLatitude = null;
        currentLongitude = null;

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        isActivityResumed = false;
        isSessionValid = false;

        validationGeneration++;

        cancelCurrentLocationRequest();
        cancelNearbyStationsRequest();

        sessionExecutor.shutdownNow();

        googleMap = null;

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putBoolean(
                KEY_PERMISSION_REQUESTED,
                permissionRequested
        );

        super.onSaveInstanceState(outState);
    }


}
