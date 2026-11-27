package com.example.ev_station.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ev_station.R;
import com.example.ev_station.adapter.ChargerAdapter;
import com.example.ev_station.model.response.ChargerResponse;
import com.example.ev_station.model.response.ChargingStationResponse;
import com.example.ev_station.repository.ChargerRepository;
import com.example.ev_station.repository.StationRepository;
import com.example.ev_station.session.SessionManager;
import com.example.ev_station.utils.SessionNavigator;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Displays selected station information and its chargers.
 *
 * Handles authenticated API requests, loading states,
 * error handling and activity lifecycle cleanup.
 */
public class StationDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_STATION_ID = "station_id";

    private long stationId;

    private View layoutDetailsContent;
    private View layoutDetailsLoading;
    private View layoutDetailsError;
    private View btnDetailsRetry;

    private TextView tvDetailsStationName;
    private TextView tvDetailsLocation;
    private TextView tvDetailsCategory;
    private TextView tvDetailsStatus;
    private TextView tvDetailsError;
    private TextView tvNoChargers;

    private RecyclerView rvChargers;

    private ChargerAdapter chargerAdapter;

    private StationRepository stationRepository;
    private ChargerRepository chargerRepository;
    private SessionManager sessionManager;

    private Call<ChargingStationResponse> stationCall;
    private Call<List<ChargerResponse>> chargerCall;

    private final ExecutorService sessionExecutor =
            Executors.newSingleThreadExecutor();

    private boolean requestInProgress = false;
    private boolean detailsLoaded = false;
    private boolean navigatingToLogin = false;

    /**
     * Creates an intent containing only the selected station ID.
     */
    public static Intent createIntent(
            Context context,
            long stationId
    ) {

        Intent intent = new Intent(
                context,
                StationDetailsActivity.class
        );

        intent.putExtra(EXTRA_STATION_ID, stationId);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_station_details);

        stationId = getIntent().getLongExtra(
                EXTRA_STATION_ID,
                -1L
        );

        initializeViews();

        sessionManager = new SessionManager(this);
        stationRepository = new StationRepository(this);
        chargerRepository = new ChargerRepository(this);

        setupRecyclerView();
        setupListeners();

        if (stationId <= 0) {

            showError(
                    R.string.station_details_invalid_id,
                    false
            );

            return;
        }

        showLoading();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (stationId > 0
                && !requestInProgress
                && !navigatingToLogin) {

            validateSession(!detailsLoaded);
        }
    }

    /**
     * Initializes all screen views.
     */
    private void initializeViews() {

        layoutDetailsContent =
                findViewById(R.id.layoutDetailsContent);

        layoutDetailsLoading =
                findViewById(R.id.layoutDetailsLoading);

        layoutDetailsError =
                findViewById(R.id.layoutDetailsError);

        btnDetailsRetry =
                findViewById(R.id.btnDetailsRetry);

        tvDetailsStationName =
                findViewById(R.id.tvDetailsStationName);

        tvDetailsLocation =
                findViewById(R.id.tvDetailsLocation);

        tvDetailsCategory =
                findViewById(R.id.tvDetailsCategory);

        tvDetailsStatus =
                findViewById(R.id.tvDetailsStatus);

        tvDetailsError =
                findViewById(R.id.tvDetailsError);

        tvNoChargers =
                findViewById(R.id.tvNoChargers);

        rvChargers =
                findViewById(R.id.rvChargers);
    }

    /**
     * Configures the charger RecyclerView.
     */
    private void setupRecyclerView() {

        chargerAdapter = new ChargerAdapter(
                this::onChargerSelected
        );

        rvChargers.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvChargers.setAdapter(chargerAdapter);
    }

    private void setupListeners() {

        findViewById(R.id.ivDetailsBack)
                .setOnClickListener(view -> finish());

        btnDetailsRetry.setOnClickListener(view -> {

            if (stationId > 0
                    && !requestInProgress
                    && !navigatingToLogin) {

                validateSession(true);
            }
        });
    }

    /**
     * Validates the encrypted session on a worker thread.
     */
    private void validateSession(boolean loadDetails) {

        if (!isActivityAvailable()
                || requestInProgress) {

            return;
        }

        requestInProgress = true;

        if (loadDetails) {
            showLoading();
        }

        sessionExecutor.execute(() -> {

            boolean loggedIn =
                    sessionManager.isLoggedIn();

            String role = loggedIn
                    ? sessionManager.getRole()
                    : null;

            runOnUiThread(() -> {

                if (!isActivityAvailable()) {
                    return;
                }

                requestInProgress = false;

                if (!loggedIn || role == null) {

                    navigateToLogin();
                    return;
                }

                if (!"USER".equals(role)) {

                    showError(
                            R.string.station_access_denied,
                            false
                    );

                    return;
                }

                if (loadDetails) {

                    fetchStationDetails();
                }
            });
        });
    }

    /**
     * Retrieves the selected station from the backend.
     */
    private void fetchStationDetails() {

        if (!isActivityAvailable()
                || requestInProgress) {

            return;
        }

        cancelRequests();

        requestInProgress = true;
        detailsLoaded = false;

        showLoading();

        chargerAdapter.setChargers(null);

        stationCall =
                stationRepository.getStationById(stationId);

        stationCall.enqueue(
                new Callback<ChargingStationResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<ChargingStationResponse> call,
                            @NonNull Response<ChargingStationResponse> response
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != stationCall) {

                            return;
                        }

                        stationCall = null;

                        if (!response.isSuccessful()) {

                            requestInProgress = false;

                            handleHttpError(response.code());

                            return;
                        }

                        ChargingStationResponse station =
                                response.body();

                        if (station == null
                                || station.getId() == null
                                || station.getId() != stationId) {

                            showError(
                                    R.string.station_invalid_response,
                                    true
                            );

                            return;
                        }

                        bindStationDetails(station);

                        fetchChargers();
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ChargingStationResponse> call,
                            @NonNull Throwable throwable
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != stationCall) {

                            return;
                        }

                        stationCall = null;

                        handleNetworkFailure(throwable);
                    }
                });
    }

    /**
     * Displays actual station information.
     */
    private void bindStationDetails(
            ChargingStationResponse station
    ) {

        tvDetailsStationName.setText(
                safeText(station.getCpoName())
        );

        List<String> addressParts = new ArrayList<>();

        addAddressPart(addressParts, station.getLocation());
        addAddressPart(addressParts, station.getCityVillage());
        addAddressPart(addressParts, station.getDistrict());
        addAddressPart(addressParts, station.getState());

        String address = addressParts.isEmpty()
                ? getString(R.string.station_details_unknown)
                : android.text.TextUtils.join(
                ", ",
                addressParts
        );

        tvDetailsLocation.setText(address);

        tvDetailsCategory.setText(
                safeText(station.getGovtPrivate())
        );

        tvDetailsStatus.setText(
                safeText(station.getStatus())
        );
    }

    /**
     * Retrieves chargers belonging to the selected station.
     */
    private void fetchChargers() {

        if (!isActivityAvailable()) {
            return;
        }

        chargerCall =
                chargerRepository.getChargersByStationId(
                        stationId
                );

        chargerCall.enqueue(
                new Callback<List<ChargerResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<ChargerResponse>> call,
                            @NonNull Response<List<ChargerResponse>> response
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != chargerCall) {

                            return;
                        }

                        chargerCall = null;
                        requestInProgress = false;

                        if (!response.isSuccessful()) {

                            handleHttpError(response.code());

                            return;
                        }

                        List<ChargerResponse> chargers =
                                response.body();

                        if (chargers == null) {

                            showError(
                                    R.string.station_invalid_response,
                                    true
                            );

                            return;
                        }

                        List<ChargerResponse> validChargers =
                                new ArrayList<>();

                        for (ChargerResponse charger : chargers) {

                            if (charger == null
                                    || charger.getId() == null
                                    || charger.getId() <= 0
                                    || charger.getChargingStationId() == null
                                    || charger.getChargingStationId()
                                    != stationId) {

                                showError(
                                        R.string.station_invalid_response,
                                        true
                                );

                                return;
                            }

                            validChargers.add(charger);
                        }

                        chargerAdapter.setChargers(validChargers);

                        detailsLoaded = true;

                        showContent();
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<ChargerResponse>> call,
                            @NonNull Throwable throwable
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != chargerCall) {

                            return;
                        }

                        chargerCall = null;

                        handleNetworkFailure(throwable);
                    }
                });
    }

    /**
     * Opens the connector selection screen
     * for the selected charger.
     */
    private void onChargerSelected(
            ChargerResponse charger
    ) {

        if (!isActivityAvailable()
                || requestInProgress
                || !detailsLoaded
                || charger == null
                || charger.getId() == null
                || charger.getId() <= 0
                || charger.getChargingStationId() == null
                || charger.getChargingStationId() != stationId) {

            return;
        }

        startActivity(
                ConnectorSelectionActivity.createIntent(
                        this,
                        stationId,
                        charger.getId()
                )
        );
    }

    /**
     * Handles backend HTTP errors.
     */
    private void handleHttpError(int statusCode) {

        detailsLoaded = false;
        requestInProgress = false;

        if (statusCode == 401) {

            handleUnauthorized();

        } else if (statusCode == 403) {

            showError(
                    R.string.station_access_denied,
                    false
            );

        } else if (statusCode == 404) {

            showError(
                    R.string.station_details_not_found,
                    false
            );

        } else {

            showError(
                    R.string.station_details_error,
                    true
            );
        }
    }

    /**
     * Rechecks session after HTTP 401.
     *
     * AuthInterceptor handles rejected-token cleanup.
     */
    private void handleUnauthorized() {

        requestInProgress = true;

        sessionExecutor.execute(() -> {

            boolean loggedIn =
                    sessionManager.isLoggedIn();

            String role = loggedIn
                    ? sessionManager.getRole()
                    : null;

            runOnUiThread(() -> {

                if (!isActivityAvailable()) {
                    return;
                }

                requestInProgress = false;

                if (!loggedIn || role == null) {

                    navigateToLogin();

                } else if (!"USER".equals(role)) {

                    showError(
                            R.string.station_access_denied,
                            false
                    );

                } else {

                    showError(
                            R.string.station_unauthorized,
                            true
                    );
                }
            });
        });
    }

    /**
     * Handles connectivity and unexpected failures.
     */
    private void handleNetworkFailure(Throwable throwable) {

        if (throwable instanceof SocketTimeoutException) {

            showError(
                    R.string.api_timeout_error,
                    true
            );

        } else if (throwable instanceof IOException) {

            showError(
                    R.string.api_network_error,
                    true
            );

        } else {

            showError(
                    R.string.api_unexpected_error,
                    true
            );
        }
    }

    private void showLoading() {

        layoutDetailsLoading.setVisibility(View.VISIBLE);
        layoutDetailsContent.setVisibility(View.GONE);
        layoutDetailsError.setVisibility(View.GONE);
    }

    private void showContent() {

        layoutDetailsLoading.setVisibility(View.GONE);
        layoutDetailsError.setVisibility(View.GONE);
        layoutDetailsContent.setVisibility(View.VISIBLE);

        boolean empty =
                chargerAdapter.getItemCount() == 0;

        tvNoChargers.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );

        rvChargers.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );
    }

    private void showError(
            int messageResId,
            boolean showRetry
    ) {

        requestInProgress = false;
        detailsLoaded = false;

        chargerAdapter.setChargers(null);

        layoutDetailsLoading.setVisibility(View.GONE);
        layoutDetailsContent.setVisibility(View.GONE);
        layoutDetailsError.setVisibility(View.VISIBLE);

        tvDetailsError.setText(messageResId);

        btnDetailsRetry.setVisibility(
                showRetry ? View.VISIBLE : View.GONE
        );
    }

    private String safeText(String value) {

        if (value == null || value.trim().isEmpty()) {

            return getString(
                    R.string.station_details_unknown
            );
        }

        return value.trim();
    }

    private void addAddressPart(
            List<String> parts,
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return;
        }

        String trimmed = value.trim();

        for (String existing : parts) {

            if (existing.equalsIgnoreCase(trimmed)) {
                return;
            }
        }

        parts.add(trimmed);
    }

    private void navigateToLogin() {

        if (!isActivityAvailable()) {
            return;
        }

        navigatingToLogin = true;

        cancelRequests();

        chargerAdapter.setChargers(null);

        Toast.makeText(
                this,
                R.string.station_session_expired,
                Toast.LENGTH_SHORT
        ).show();

        SessionNavigator.navigateToLogin(this);
    }

    private boolean isActivityAvailable() {

        return !isFinishing()
                && !isDestroyed()
                && !navigatingToLogin;
    }

    /**
     * Cancels active API requests.
     */
    private void cancelRequests() {

        if (stationCall != null) {

            stationCall.cancel();
            stationCall = null;
        }

        if (chargerCall != null) {

            chargerCall.cancel();
            chargerCall = null;
        }
    }

    @Override
    protected void onDestroy() {

        cancelRequests();

        sessionExecutor.shutdownNow();

        super.onDestroy();
    }
}