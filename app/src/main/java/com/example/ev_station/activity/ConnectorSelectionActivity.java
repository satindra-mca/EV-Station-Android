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
import com.example.ev_station.adapter.ConnectorAdapter;
import com.example.ev_station.model.response.ChargerResponse;
import com.example.ev_station.model.response.ConnectorResponse;
import com.example.ev_station.repository.ChargerRepository;
import com.example.ev_station.repository.ConnectorRepository;
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
 * Displays the selected charger and its connectors.
 *
 * Handles session validation, authenticated API requests,
 * connector selection and lifecycle cleanup.
 */
public class ConnectorSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_STATION_ID = "station_id";
    public static final String EXTRA_CHARGER_ID = "charger_id";

    private long stationId;
    private long chargerId;

    private View layoutConnectorContent;
    private View layoutConnectorLoading;
    private View layoutConnectorError;
    private View btnConnectorRetry;

    private TextView tvSelectedChargerType;
    private TextView tvSelectedChargerRating;
    private TextView tvSelectedChargerPrice;
    private TextView tvConnectorError;
    private TextView tvNoConnectors;

    private RecyclerView rvConnectors;

    private ConnectorAdapter connectorAdapter;

    private ChargerRepository chargerRepository;
    private ConnectorRepository connectorRepository;
    private SessionManager sessionManager;

    private Call<ChargerResponse> chargerCall;
    private Call<List<ConnectorResponse>> connectorCall;

    private final ExecutorService sessionExecutor =
            Executors.newSingleThreadExecutor();

    private boolean requestInProgress = false;
    private boolean connectorsLoaded = false;
    private boolean navigatingToLogin = false;

    /**
     * Creates an intent containing the selected station
     * and charger identifiers.
     */
    public static Intent createIntent(
            Context context,
            long stationId,
            long chargerId
    ) {

        Intent intent = new Intent(
                context,
                ConnectorSelectionActivity.class
        );

        intent.putExtra(EXTRA_STATION_ID, stationId);
        intent.putExtra(EXTRA_CHARGER_ID, chargerId);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connector_selection);

        stationId = getIntent().getLongExtra(
                EXTRA_STATION_ID,
                -1L
        );

        chargerId = getIntent().getLongExtra(
                EXTRA_CHARGER_ID,
                -1L
        );

        initializeViews();

        sessionManager = new SessionManager(this);
        chargerRepository = new ChargerRepository(this);
        connectorRepository = new ConnectorRepository(this);

        setupRecyclerView();
        setupListeners();

        if (stationId <= 0 || chargerId <= 0) {

            showError(
                    R.string.connector_invalid_selection,
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
                && chargerId > 0
                && !requestInProgress
                && !navigatingToLogin) {

            validateSession(true);
        }
    }

    /**
     * Initializes screen views.
     */
    private void initializeViews() {

        layoutConnectorContent =
                findViewById(R.id.layoutConnectorContent);

        layoutConnectorLoading =
                findViewById(R.id.layoutConnectorLoading);

        layoutConnectorError =
                findViewById(R.id.layoutConnectorError);

        btnConnectorRetry =
                findViewById(R.id.btnConnectorRetry);

        tvSelectedChargerType =
                findViewById(R.id.tvSelectedChargerType);

        tvSelectedChargerRating =
                findViewById(R.id.tvSelectedChargerRating);

        tvSelectedChargerPrice =
                findViewById(R.id.tvSelectedChargerPrice);

        tvConnectorError =
                findViewById(R.id.tvConnectorError);

        tvNoConnectors =
                findViewById(R.id.tvNoConnectors);

        rvConnectors =
                findViewById(R.id.rvConnectors);
    }

    private void setupRecyclerView() {

        connectorAdapter = new ConnectorAdapter(
                this::onConnectorSelected
        );

        rvConnectors.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvConnectors.setAdapter(connectorAdapter);
    }

    private void setupListeners() {

        findViewById(R.id.ivConnectorBack)
                .setOnClickListener(view -> finish());

        btnConnectorRetry.setOnClickListener(view -> {

            if (stationId > 0
                    && chargerId > 0
                    && !requestInProgress
                    && !navigatingToLogin) {

                validateSession(true);
            }
        });
    }

    /**
     * Validates the encrypted session on a worker thread.
     */
    private void validateSession(boolean loadConnectors) {

        if (!isActivityAvailable()
                || requestInProgress) {

            return;
        }

        requestInProgress = true;

        if (loadConnectors) {
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

                if (loadConnectors) {

                    fetchChargerDetails();
                }
            });
        });
    }

    /**
     * Retrieves the selected charger and verifies
     * that it belongs to the selected station.
     */
    private void fetchChargerDetails() {

        if (!isActivityAvailable()
                || requestInProgress) {

            return;
        }

        cancelRequests();

        requestInProgress = true;
        connectorsLoaded = false;

        showLoading();

        connectorAdapter.setConnectors(null);

        chargerCall =
                chargerRepository.getChargerById(chargerId);

        chargerCall.enqueue(
                new Callback<ChargerResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<ChargerResponse> call,
                            @NonNull Response<ChargerResponse> response
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != chargerCall) {

                            return;
                        }

                        chargerCall = null;

                        if (!response.isSuccessful()) {

                            handleHttpError(response.code());
                            return;
                        }

                        ChargerResponse charger =
                                response.body();

                        if (charger == null
                                || charger.getId() == null
                                || charger.getId() != chargerId
                                || charger.getChargingStationId() == null
                                || charger.getChargingStationId()
                                != stationId) {

                            showError(
                                    R.string.connector_invalid_selection,
                                    false
                            );

                            return;
                        }

                        bindChargerDetails(charger);

                        fetchConnectors();
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ChargerResponse> call,
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
     * Displays current charger information from the backend.
     */
    private void bindChargerDetails(
            ChargerResponse charger
    ) {

        tvSelectedChargerType.setText(
                safeText(charger.getChargerType())
        );

        tvSelectedChargerRating.setText(
                getString(
                        R.string.charger_power_format,
                        safeText(charger.getChargerRating())
                )
        );

        if (charger.getPricePerKwh() != null) {

            tvSelectedChargerPrice.setText(
                    getString(
                            R.string.charger_price_format,
                            charger.getPricePerKwh()
                                    .toPlainString()
                    )
            );

        } else {

            tvSelectedChargerPrice.setText(
                    R.string.charger_price_unavailable
            );
        }
    }

    /**
     * Retrieves connectors belonging to the selected charger.
     */
    private void fetchConnectors() {

        if (!isActivityAvailable()) {
            return;
        }

        connectorCall =
                connectorRepository
                        .getConnectorsByChargerId(chargerId);

        connectorCall.enqueue(
                new Callback<List<ConnectorResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<ConnectorResponse>> call,
                            @NonNull Response<List<ConnectorResponse>> response
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != connectorCall) {

                            return;
                        }

                        connectorCall = null;
                        requestInProgress = false;

                        if (!response.isSuccessful()) {

                            handleHttpError(response.code());
                            return;
                        }

                        List<ConnectorResponse> connectors =
                                response.body();

                        if (connectors == null) {

                            showError(
                                    R.string.station_invalid_response,
                                    true
                            );

                            return;
                        }

                        List<ConnectorResponse> validConnectors =
                                new ArrayList<>();

                        for (ConnectorResponse connector : connectors) {

                            if (connector == null
                                    || connector.getId() == null
                                    || connector.getId() <= 0
                                    || connector.getChargerId() == null
                                    || connector.getChargerId() != chargerId
                                    || connector.getConnectorNumber() == null
                                    || connector.getConnectorNumber() <= 0) {

                                showError(
                                        R.string.station_invalid_response,
                                        true
                                );

                                return;
                            }

                            validConnectors.add(connector);
                        }

                        // Display connectors in numerical order.
                        validConnectors.sort(
                                (first, second) -> Integer.compare(
                                        first.getConnectorNumber(),
                                        second.getConnectorNumber()
                                )
                        );

                        connectorAdapter.setConnectors(
                                validConnectors
                        );

                        connectorsLoaded = true;

                        showContent();
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<ConnectorResponse>> call,
                            @NonNull Throwable throwable
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != connectorCall) {

                            return;
                        }

                        connectorCall = null;

                        handleNetworkFailure(throwable);
                    }
                });
    }

    /**
     * Opens the booking screen for a valid connector.
     */
    private void onConnectorSelected(
            ConnectorResponse connector
    ) {

        if (!isActivityAvailable()
                || requestInProgress
                || !connectorsLoaded
                || connector == null
                || connector.getId() == null
                || connector.getId() <= 0
                || connector.getChargerId() == null
                || connector.getChargerId() != chargerId
                || connector.getConnectorNumber() == null
                || connector.getConnectorNumber() <= 0) {

            return;
        }

        String status = connector.getStatus();

        boolean supportedStatus =
                "AVAILABLE".equalsIgnoreCase(status)
                        || "OCCUPIED".equalsIgnoreCase(status);

        if (!supportedStatus) {

            return;
        }

        startActivity(
                SlotBookingActivity.createIntent(
                        this,
                        stationId,
                        chargerId,
                        connector.getId()
                )
        );
    }

    /**
     * Handles backend HTTP errors.
     */
    private void handleHttpError(int statusCode) {

        connectorsLoaded = false;
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
                    R.string.connector_not_found,
                    false
            );

        } else {

            showError(
                    R.string.connector_load_error,
                    true
            );
        }
    }

    /**
     * Rechecks the current session after HTTP 401.
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

        layoutConnectorLoading.setVisibility(View.VISIBLE);
        layoutConnectorContent.setVisibility(View.GONE);
        layoutConnectorError.setVisibility(View.GONE);
    }

    private void showContent() {

        layoutConnectorLoading.setVisibility(View.GONE);
        layoutConnectorError.setVisibility(View.GONE);
        layoutConnectorContent.setVisibility(View.VISIBLE);

        boolean empty =
                connectorAdapter.getItemCount() == 0;

        tvNoConnectors.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );

        rvConnectors.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );
    }

    private void showError(
            int messageResId,
            boolean showRetry
    ) {

        requestInProgress = false;
        connectorsLoaded = false;

        connectorAdapter.setConnectors(null);

        layoutConnectorLoading.setVisibility(View.GONE);
        layoutConnectorContent.setVisibility(View.GONE);
        layoutConnectorError.setVisibility(View.VISIBLE);

        tvConnectorError.setText(messageResId);

        btnConnectorRetry.setVisibility(
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

    private void navigateToLogin() {

        if (!isActivityAvailable()) {
            return;
        }

        navigatingToLogin = true;

        cancelRequests();

        connectorAdapter.setConnectors(null);

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

        if (chargerCall != null) {

            chargerCall.cancel();
            chargerCall = null;
        }

        if (connectorCall != null) {

            connectorCall.cancel();
            connectorCall = null;
        }
    }

    @Override
    protected void onDestroy() {

        cancelRequests();

        sessionExecutor.shutdownNow();

        super.onDestroy();
    }
}