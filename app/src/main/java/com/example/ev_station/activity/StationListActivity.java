package com.example.ev_station.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ev_station.R;
import com.example.ev_station.adapter.StationAdapter;
import com.example.ev_station.model.response.ChargingStationResponse;
import com.example.ev_station.repository.StationRepository;
import com.example.ev_station.session.SessionManager;
import com.example.ev_station.utils.SessionNavigator;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Displays charging stations retrieved from the backend.
 *
 * Handles authentication, asynchronous API requests,
 * loading states, errors and lifecycle cleanup.
 */
public class StationListActivity extends AppCompatActivity {

    private ImageView ivStationBack;
    private ImageView ivStationMessage;

    private TextView tvStationCount;
    private TextView tvStationMessage;

    private RecyclerView rvStations;

    private LinearLayout layoutStationLoading;
    private LinearLayout layoutStationMessage;

    private MaterialButton btnStationRetry;

    private StationAdapter stationAdapter;
    private StationRepository stationRepository;
    private SessionManager sessionManager;

    private Call<List<ChargingStationResponse>> stationCall;

    private final ExecutorService sessionExecutor =
            Executors.newSingleThreadExecutor();

    private boolean requestInProgress = false;
    private boolean navigatingToLogin = false;
    private boolean stationsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_station_list);

        sessionManager = new SessionManager(this);
        stationRepository = new StationRepository(this);

        initializeViews();
        setupRecyclerView();
        setupListeners();

        showLoading();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (!requestInProgress && !navigatingToLogin) {

            validateSession(!stationsLoaded);
        }
    }

    /**
     * Initializes screen views.
     */
    private void initializeViews() {

        ivStationBack = findViewById(
                R.id.ivStationBack
        );

        ivStationMessage = findViewById(
                R.id.ivStationMessage
        );

        tvStationCount = findViewById(
                R.id.tvStationCount
        );

        tvStationMessage = findViewById(
                R.id.tvStationMessage
        );

        rvStations = findViewById(
                R.id.rvStations
        );

        layoutStationLoading = findViewById(
                R.id.layoutStationLoading
        );

        layoutStationMessage = findViewById(
                R.id.layoutStationMessage
        );

        btnStationRetry = findViewById(
                R.id.btnStationRetry
        );
    }

    /**
     * Configures the RecyclerView and station adapter.
     */
    private void setupRecyclerView() {

        stationAdapter = new StationAdapter();

        stationAdapter.setOnStationClickListener(
                this::openStationDetails
        );

        rvStations.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvStations.setAdapter(stationAdapter);
    }

    /**
     * Opens details for the selected charging station.
     */
    private void openStationDetails(
            ChargingStationResponse station
    ) {

        if (!isActivityAvailable()
                || requestInProgress
                || !stationsLoaded
                || station == null
                || station.getId() == null
                || station.getId() <= 0) {

            return;
        }

        startActivity(
                StationDetailsActivity.createIntent(
                        this,
                        station.getId()
                )
        );
    }

    /**
     * Configures back navigation and retry actions.
     */
    private void setupListeners() {

        ivStationBack.setOnClickListener(
                view -> finish()
        );

        btnStationRetry.setOnClickListener(view -> {

            if (!requestInProgress
                    && !navigatingToLogin) {

                validateSession(true);
            }
        });
    }

    /**
     * Validates the encrypted session on a worker thread.
     *
     * Session storage operations must not block the UI.
     */
    private void validateSession(boolean loadStations) {

        if (!isActivityAvailable()
                || requestInProgress) {

            return;
        }

        requestInProgress = true;

        if (loadStations) {
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

                    stationAdapter.setStations(null);

                    stationsLoaded = false;

                    showMessage(
                            R.string.station_access_denied,
                            true,
                            false
                    );

                    return;
                }

                if (loadStations) {

                    fetchStations();
                }
            });
        });
    }

    /**
     * Retrieves charging stations asynchronously.
     */
    private void fetchStations() {

        if (!isActivityAvailable()
                || requestInProgress) {

            return;
        }

        cancelStationRequest();

        requestInProgress = true;

        showLoading();

        stationCall =
                stationRepository.getAllStations();

        stationCall.enqueue(
                new Callback<List<ChargingStationResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<ChargingStationResponse>> call,
                            @NonNull Response<List<ChargingStationResponse>> response
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != stationCall) {

                            return;
                        }

                        stationCall = null;
                        requestInProgress = false;

                        if (response.isSuccessful()) {

                            handleSuccessfulResponse(
                                    response.body()
                            );

                        } else {

                            handleHttpError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<ChargingStationResponse>> call,
                            @NonNull Throwable throwable
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != stationCall) {

                            return;
                        }

                        stationCall = null;
                        requestInProgress = false;

                        handleNetworkFailure(throwable);
                    }
                });
    }

    /**
     * Handles a successful station API response.
     */
    private void handleSuccessfulResponse(
            List<ChargingStationResponse> stations
    ) {

        if (stations == null) {

            stationsLoaded = false;

            showMessage(
                    R.string.station_invalid_response,
                    true,
                    true
            );

            return;
        }

        stationAdapter.setStations(stations);

        stationsLoaded = true;

        int stationCount =
                stationAdapter.getItemCount();

        if (stationCount == 0) {

            showMessage(
                    R.string.no_stations_found,
                    false,
                    true
            );

            return;
        }

        showStationList(stationCount);
    }

    /**
     * Handles HTTP errors returned by the backend.
     */
    private void handleHttpError(int statusCode) {

        stationsLoaded = false;

        if (statusCode == 401) {

            handleUnauthorized();

        } else if (statusCode == 403) {

            showMessage(
                    R.string.station_access_denied,
                    true,
                    false
            );

        } else {

            showMessage(
                    R.string.station_load_error,
                    true,
                    true
            );
        }
    }

    /**
     * Rechecks the current session after an HTTP 401.
     *
     * AuthInterceptor already clears the rejected session
     * only when its token matches the current token.
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

                    showMessage(
                            R.string.station_access_denied,
                            true,
                            false
                    );

                } else {

                    // Preserve a potentially newer valid session.
                    showMessage(
                            R.string.station_unauthorized,
                            true,
                            true
                    );
                }
            });
        });
    }

    /**
     * Handles timeout, connectivity and unexpected errors.
     */
    private void handleNetworkFailure(
            Throwable throwable
    ) {

        stationsLoaded = false;

        if (throwable instanceof SocketTimeoutException) {

            showMessage(
                    R.string.api_timeout_error,
                    true,
                    true
            );

        } else if (throwable instanceof IOException) {

            showMessage(
                    R.string.api_network_error,
                    true,
                    true
            );

        } else {

            showMessage(
                    R.string.api_unexpected_error,
                    true,
                    true
            );
        }
    }

    /**
     * Displays the loading state.
     */
    private void showLoading() {

        layoutStationLoading.setVisibility(View.VISIBLE);

        layoutStationMessage.setVisibility(View.GONE);

        rvStations.setVisibility(View.GONE);

        tvStationCount.setVisibility(View.GONE);
    }

    /**
     * Displays the station list and actual record count.
     */
    private void showStationList(int stationCount) {

        layoutStationLoading.setVisibility(View.GONE);

        layoutStationMessage.setVisibility(View.GONE);

        rvStations.setVisibility(View.VISIBLE);

        tvStationCount.setText(
                getString(
                        R.string.station_list_count,
                        stationCount
                )
        );

        tvStationCount.setVisibility(View.VISIBLE);
    }

    /**
     * Displays an empty or error state.
     */
    private void showMessage(
            int messageResId,
            boolean isError,
            boolean showRetry
    ) {

        layoutStationLoading.setVisibility(View.GONE);

        rvStations.setVisibility(View.GONE);

        tvStationCount.setVisibility(View.GONE);

        layoutStationMessage.setVisibility(View.VISIBLE);

        tvStationMessage.setText(messageResId);

        btnStationRetry.setVisibility(
                showRetry ? View.VISIBLE : View.GONE
        );

        ivStationMessage.setImageResource(
                isError
                        ? android.R.drawable.ic_dialog_alert
                        : android.R.drawable.ic_dialog_info
        );

        ivStationMessage.setColorFilter(
                ContextCompat.getColor(
                        this,
                        isError
                                ? R.color.ev_error
                                : R.color.ev_text_secondary
                )
        );
    }

    /**
     * Redirects unauthenticated users to the login screen.
     */
    private void navigateToLogin() {

        if (!isActivityAvailable()) {
            return;
        }

        navigatingToLogin = true;

        cancelStationRequest();

        stationAdapter.setStations(null);

        Toast.makeText(
                this,
                R.string.station_session_expired,
                Toast.LENGTH_SHORT
        ).show();

        SessionNavigator.navigateToLogin(this);
    }

    /**
     * Checks whether UI updates are still permitted.
     */
    private boolean isActivityAvailable() {

        return !isFinishing()
                && !isDestroyed()
                && !navigatingToLogin;
    }

    /**
     * Cancels the currently active station request.
     */
    private void cancelStationRequest() {

        if (stationCall != null) {

            stationCall.cancel();

            stationCall = null;
        }
    }

    @Override
    protected void onDestroy() {

        cancelStationRequest();

        sessionExecutor.shutdownNow();

        super.onDestroy();
    }
}
