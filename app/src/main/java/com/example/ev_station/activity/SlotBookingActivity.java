package com.example.ev_station.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ev_station.R;
import com.example.ev_station.model.request.BookingRequest;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import androidx.appcompat.app.AlertDialog;

import com.example.ev_station.model.response.ChargingStationResponse;
import com.example.ev_station.model.response.ChargerResponse;
import com.example.ev_station.model.response.ConnectorResponse;

import com.example.ev_station.repository.StationRepository;
import com.example.ev_station.repository.ChargerRepository;
import com.example.ev_station.repository.ConnectorRepository;
import com.example.ev_station.repository.BookingRepository;
import com.example.ev_station.model.response.BookingPreviewResponse;
import com.example.ev_station.model.response.BookingResponse;

import com.example.ev_station.session.SessionManager;
import com.example.ev_station.utils.SessionNavigator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
/**
 * Displays the booking screen and manages date-time selection
 * for the selected charging connector.
 */
public class SlotBookingActivity extends AppCompatActivity {

    public static final String EXTRA_STATION_ID = "station_id";
    public static final String EXTRA_CHARGER_ID = "charger_id";
    public static final String EXTRA_CONNECTOR_ID = "connector_id";

    private static final long MIN_BOOKING_DURATION_MINUTES = 30L;
    private static final long MILLIS_PER_MINUTE = 60_000L;

    private long stationId;
    private long chargerId;
    private long connectorId;

    private MaterialButton btnBookingDate;
    private MaterialButton btnBookingStartTime;
    private MaterialButton btnBookingEndTime;
    private MaterialButton btnCheckAvailability;
    private MaterialButton btnProceedBooking;

    private TextView tvBookingAvailabilityMessage;

    private View cardBookingSummary;

    private Calendar selectedDate;
    private Calendar selectedStartTime;
    private Calendar selectedEndTime;
    // Booking detail views
    private TextView tvBookingStationName;
    private TextView tvBookingStationAddress;
    private TextView tvBookingChargerType;
    private TextView tvBookingConnectorNumber;
    private TextView tvBookingPricePerKwh;

    private View pbBooking;

    // Repositories
    private StationRepository stationRepository;
    private ChargerRepository chargerRepository;
    private ConnectorRepository connectorRepository;

    // Session management
    private SessionManager sessionManager;

    private final ExecutorService sessionExecutor =
            Executors.newSingleThreadExecutor();

    // Active API requests
    private Call<ChargingStationResponse> stationCall;
    private Call<ChargerResponse> chargerCall;
    private Call<ConnectorResponse> connectorCall;

    // Loaded booking details
    private ChargingStationResponse selectedStation;
    private ChargerResponse selectedCharger;
    private ConnectorResponse selectedConnector;

    // Screen state
    private boolean detailsLoaded = false;
    private boolean detailsLoading = false;
    private boolean navigatingToLogin = false;

    // Booking preview views
    private TextView tvBookingDuration;
    private TextView tvBookingEstimatedEnergy;
    private TextView tvBookingEstimatedAmount;
    private TextView tvBookingAdvanceAmount;

    // Booking preview repository and request
    private BookingRepository bookingRepository;
    private Call<BookingPreviewResponse> previewCall;

    // Verified booking preview
    private BookingPreviewResponse bookingPreview;

    // Preview loading state
    private boolean previewLoading = false;
    private boolean creationLoading = false;
    private boolean creationLocked = false;
    private Call<BookingResponse> creationCall;

    /**
     * Creates an intent containing the selected booking identifiers.
     */
    public static Intent createIntent(
            Context context,
            long stationId,
            long chargerId,
            long connectorId
    ) {

        Intent intent = new Intent(
                context,
                SlotBookingActivity.class
        );

        intent.putExtra(EXTRA_STATION_ID, stationId);
        intent.putExtra(EXTRA_CHARGER_ID, chargerId);
        intent.putExtra(EXTRA_CONNECTOR_ID, connectorId);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_slot_booking);

        stationId = getIntent().getLongExtra(
                EXTRA_STATION_ID,
                -1L
        );

        chargerId = getIntent().getLongExtra(
                EXTRA_CHARGER_ID,
                -1L
        );

        connectorId = getIntent().getLongExtra(
                EXTRA_CONNECTOR_ID,
                -1L
        );

        if (!hasValidSelection()) {

            Toast.makeText(
                    this,
                    R.string.booking_invalid_selection,
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        initializeViews();
        initializeListeners();
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        if (!creationLoading) finish();
                    }
                });

        resetBookingPreview();
        updateAvailabilityButton();

        if (savedInstanceState != null) {
            creationLocked = savedInstanceState.getBoolean("creation_locked", false);
        }
        initializeRepositories();
        validateSessionAndLoadDetails();
    }

    /**
     * Validates the received booking identifiers.
     */
    private boolean hasValidSelection() {

        return stationId > 0
                && chargerId > 0
                && connectorId > 0;
    }

    /**
     * Initializes booking screen views.
     */
    private void initializeViews() {

        btnBookingDate = findViewById(
                R.id.btnBookingDate
        );

        btnBookingStartTime = findViewById(
                R.id.btnBookingStartTime
        );

        btnBookingEndTime = findViewById(
                R.id.btnBookingEndTime
        );

        btnCheckAvailability = findViewById(
                R.id.btnCheckAvailability
        );

        btnProceedBooking = findViewById(
                R.id.btnProceedBooking
        );

        tvBookingAvailabilityMessage = findViewById(
                R.id.tvBookingAvailabilityMessage
        );

        cardBookingSummary = findViewById(
                R.id.cardBookingSummary
        );

        tvBookingStationName = findViewById(
                R.id.tvBookingStationName
        );

        tvBookingStationAddress = findViewById(
                R.id.tvBookingStationAddress
        );

        tvBookingChargerType = findViewById(
                R.id.tvBookingChargerType
        );

        tvBookingConnectorNumber = findViewById(
                R.id.tvBookingConnectorNumber
        );

        tvBookingPricePerKwh = findViewById(
                R.id.tvBookingPricePerKwh
        );

        pbBooking = findViewById(
                R.id.pbBooking
        );

        tvBookingDuration = findViewById(
                R.id.tvBookingDuration
        );

        tvBookingEstimatedEnergy = findViewById(
                R.id.tvBookingEstimatedEnergy
        );

        tvBookingEstimatedAmount = findViewById(
                R.id.tvBookingEstimatedAmount
        );

        tvBookingAdvanceAmount = findViewById(
                R.id.tvBookingAdvanceAmount
        );
    }

    /**
     * Registers booking screen click listeners.
     */
    private void initializeListeners() {

        findViewById(R.id.ivBookingBack)
                .setOnClickListener(view -> { if (!creationLoading) finish(); });

        btnBookingDate.setOnClickListener(
                view -> showDatePicker()
        );

        btnBookingStartTime.setOnClickListener(
                view -> showStartTimePicker()
        );

        btnBookingEndTime.setOnClickListener(
                view -> showEndTimePicker()
        );

        btnCheckAvailability.setOnClickListener(
                view -> checkSlotAvailability()
        );

        btnProceedBooking.setOnClickListener(view -> confirmBookingCreation());
    }


    /**
     * Displays a date picker that prevents past date selection.
     */
    private void showDatePicker() {

        Calendar initialDate = selectedDate != null
                ? selectedDate
                : Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    Calendar date = Calendar.getInstance();

                    date.clear();

                    date.set(
                            year,
                            month,
                            dayOfMonth,
                            0,
                            0,
                            0
                    );

                    selectedDate = date;

                    btnBookingDate.setText(
                            formatDisplayDate(selectedDate)
                    );

                    // Reset times when the selected date changes.
                    selectedStartTime = null;
                    selectedEndTime = null;

                    btnBookingStartTime.setText(
                            R.string.booking_start_time
                    );

                    btnBookingEndTime.setText(
                            R.string.booking_end_time
                    );

                    onSlotSelectionChanged();
                },
                initialDate.get(Calendar.YEAR),
                initialDate.get(Calendar.MONTH),
                initialDate.get(Calendar.DAY_OF_MONTH)
        );

        Calendar today = Calendar.getInstance();

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        dialog.getDatePicker().setMinDate(
                today.getTimeInMillis()
        );

        dialog.show();
    }

    /**
     * Displays the booking start time picker.
     */
    private void showStartTimePicker() {

        if (selectedDate == null) {

            showBookingMessage(
                    getString(R.string.booking_date_required),
                    true
            );

            return;
        }

        Calendar initialTime = selectedStartTime != null
                ? selectedStartTime
                : Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {

                    Calendar startTime = createDateTime(
                            selectedDate,
                            hourOfDay,
                            minute
                    );

                    if (startTime.getTimeInMillis()
                            <= System.currentTimeMillis()) {

                        selectedStartTime = null;
                        selectedEndTime = null;

                        btnBookingStartTime.setText(
                                R.string.booking_start_time
                        );

                        btnBookingEndTime.setText(
                                R.string.booking_end_time
                        );

                        resetBookingPreview();
                        updateAvailabilityButton();

                        showBookingMessage(
                                getString(R.string.booking_start_future),
                                true
                        );

                        updateAvailabilityButton();
                        return;
                    }

                    selectedStartTime = startTime;

                    btnBookingStartTime.setText(
                            formatDisplayTime(selectedStartTime)
                    );

                    // Reset end time when start time changes.
                    selectedEndTime = null;

                    btnBookingEndTime.setText(
                            R.string.booking_end_time
                    );

                    onSlotSelectionChanged();
                },
                initialTime.get(Calendar.HOUR_OF_DAY),
                initialTime.get(Calendar.MINUTE),
                false
        );

        dialog.show();
    }

    /**
     * Displays the booking end time picker.
     */
    private void showEndTimePicker() {

        if (selectedDate == null) {

            showBookingMessage(
                    getString(R.string.booking_date_required),
                    true
            );

            return;
        }

        if (selectedStartTime == null) {

            showBookingMessage(
                    getString(R.string.booking_start_required),
                    true
            );

            return;
        }

        Calendar initialTime;

        if (selectedEndTime != null) {

            initialTime = selectedEndTime;

        } else {

            initialTime = (Calendar) selectedStartTime.clone();

            initialTime.add(
                    Calendar.MINUTE,
                    (int) MIN_BOOKING_DURATION_MINUTES
            );
        }

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {

                    Calendar endTime = createDateTime(
                            selectedDate,
                            hourOfDay,
                            minute
                    );

                    if (!endTime.after(selectedStartTime)) {

                        selectedEndTime = null;

                        btnBookingEndTime.setText(
                                R.string.booking_end_time
                        );

                        resetBookingPreview();
                        updateAvailabilityButton();

                        showBookingMessage(
                                getString(R.string.booking_end_after_start),
                                true
                        );

                        return;
                    }

                    long durationMinutes =
                            calculateDurationMinutes(
                                    selectedStartTime,
                                    endTime
                            );

                    if (durationMinutes
                            < MIN_BOOKING_DURATION_MINUTES) {

                        selectedEndTime = null;

                        btnBookingEndTime.setText(
                                R.string.booking_end_time
                        );

                        resetBookingPreview();
                        updateAvailabilityButton();

                        showBookingMessage(
                                getString(R.string.booking_duration_invalid),
                                true
                        );

                        return;
                    }

                    selectedEndTime = endTime;

                    btnBookingEndTime.setText(
                            formatDisplayTime(selectedEndTime)
                    );

                    onSlotSelectionChanged();
                },
                initialTime.get(Calendar.HOUR_OF_DAY),
                initialTime.get(Calendar.MINUTE),
                false
        );

        dialog.show();
    }

    /**
     * Combines a selected date with the chosen hour and minute.
     */
    private Calendar createDateTime(
            Calendar date,
            int hour,
            int minute
    ) {

        Calendar dateTime = (Calendar) date.clone();

        dateTime.set(Calendar.HOUR_OF_DAY, hour);
        dateTime.set(Calendar.MINUTE, minute);
        dateTime.set(Calendar.SECOND, 0);
        dateTime.set(Calendar.MILLISECOND, 0);

        return dateTime;
    }

    /**
     * Invalidates any previous preview when the slot changes.
     */
    private void onSlotSelectionChanged() {

        resetBookingPreview();

        updateAvailabilityButton();

        if (validateSelectedSlot(false)) {

            long durationMinutes = calculateDurationMinutes(
                    selectedStartTime,
                    selectedEndTime
            );

            showBookingMessage(
                    getString(
                            R.string.booking_slot_valid,
                            durationMinutes
                    ),
                    false
            );
        }
    }

    /**
     * Validates the selected booking slot.
     */
    private boolean validateSelectedSlot(boolean showError) {

        int errorMessage = 0;

        if (selectedDate == null) {

            errorMessage = R.string.booking_date_required;

        } else if (selectedStartTime == null) {

            errorMessage = R.string.booking_start_required;

        } else if (selectedEndTime == null) {

            errorMessage = R.string.booking_end_required;

        } else if (selectedStartTime.getTimeInMillis()
                <= System.currentTimeMillis()) {

            errorMessage = R.string.booking_start_future;

        } else if (!selectedEndTime.after(selectedStartTime)) {

            errorMessage = R.string.booking_end_after_start;

        } else if (calculateDurationMinutes(
                selectedStartTime,
                selectedEndTime
        ) < MIN_BOOKING_DURATION_MINUTES) {

            errorMessage = R.string.booking_duration_invalid;
        }

        if (errorMessage != 0) {

            if (showError) {

                showBookingMessage(
                        getString(errorMessage),
                        true
                );
            }

            return false;
        }

        return true;
    }

    /**
     * Calculates the selected booking duration in minutes.
     */
    private long calculateDurationMinutes(
            Calendar startTime,
            Calendar endTime
    ) {

        return (
                endTime.getTimeInMillis()
                        - startTime.getTimeInMillis()
        ) / MILLIS_PER_MINUTE;
    }
    /**
     * Enables availability checking only when
     * booking details and the selected slot are valid.
     */
    private void updateAvailabilityButton() {

        btnCheckAvailability.setEnabled(
                detailsLoaded
                        && !detailsLoading
                        && !previewLoading
                        && !creationLoading
                        && !creationLocked
                        && validateSelectedSlot(false)
        );

        btnProceedBooking.setEnabled(
                detailsLoaded
                        && !detailsLoading
                        && !previewLoading
                        && !creationLoading
                        && !creationLocked
                        && bookingPreview != null
                        && bookingPreview.isAvailable()
                        && validateSelectedSlot(false)
        );
    }


    /**
     * Initializes repositories using the shared Retrofit client.
     */
    private void initializeRepositories() {

        stationRepository = new StationRepository(this);

        chargerRepository = new ChargerRepository(this);

        connectorRepository = new ConnectorRepository(this);

        bookingRepository = new BookingRepository(this);

        sessionManager = new SessionManager(this);
    }

    /**
     * Checks the authenticated session before loading details.
     */
    private void validateSessionAndLoadDetails() {

        setDetailsLoading(true);

        sessionExecutor.execute(() -> {

            boolean loggedIn = sessionManager.isLoggedIn();

            String role = loggedIn
                    ? sessionManager.getRole()
                    : null;

            runOnUiThread(() -> {

                if (!isActivityAvailable()) {
                    return;
                }

                if (!loggedIn || role == null) {

                    navigateToLogin();
                    return;
                }

                if (!"USER".equals(role)) {

                    Toast.makeText(
                            this,
                            R.string.booking_access_denied,
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                    return;
                }

                loadStationDetails();
            });
        });
    }

    /**
     * Checks whether the activity can safely receive UI updates.
     */
    private boolean isActivityAvailable() {

        return !isFinishing()
                && !isDestroyed()
                && !navigatingToLogin;
    }

    /**
     * Navigates to login when the session is invalid.
     */
    private void navigateToLogin() {

        if (!isActivityAvailable()) {
            return;
        }

        navigatingToLogin = true;

        cancelPreviewRequest();

        cancelDetailRequests();

        Toast.makeText(
                this,
                R.string.booking_session_expired,
                Toast.LENGTH_SHORT
        ).show();

        SessionNavigator.navigateToLogin(this);
    }

    /**
     * Updates the loading state of the booking screen.
     */
    private void setDetailsLoading(boolean loading) {

        detailsLoading = loading;

        pbBooking.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnBookingDate.setEnabled(
                !loading && detailsLoaded && !creationLocked && !creationLoading
        );

        btnBookingStartTime.setEnabled(
                !loading && detailsLoaded && !creationLocked && !creationLoading
        );

        btnBookingEndTime.setEnabled(
                !loading && detailsLoaded && !creationLocked && !creationLoading
        );

        updateAvailabilityButton();
    }


    /**
     * Loads the selected station from the backend.
     */
    private void loadStationDetails() {

        if (!isActivityAvailable()) {
            return;
        }

        cancelDetailRequests();

        detailsLoaded = false;

        selectedStation = null;
        selectedCharger = null;
        selectedConnector = null;

        resetBookingPreview();

        setDetailsLoading(true);

        tvBookingStationName.setText(
                R.string.booking_loading
        );

        stationCall = stationRepository.getStationById(
                stationId
        );

        stationCall.enqueue(
                new Callback<ChargingStationResponse>() {

                    @Override
                    public void onResponse(
                            Call<ChargingStationResponse> call,
                            Response<ChargingStationResponse> response
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != stationCall) {
                            return;
                        }

                        if (!response.isSuccessful()) {

                            handleDetailsHttpError(response.code());
                            return;
                        }

                        ChargingStationResponse station =
                                response.body();

                        if (station == null
                                || station.getId() == null
                                || station.getId() != stationId) {

                            showDetailsError(
                                    R.string.booking_details_mismatch,
                                    false
                            );

                            return;
                        }

                        selectedStation = station;

                        loadChargerDetails();
                    }

                    @Override
                    public void onFailure(
                            Call<ChargingStationResponse> call,
                            Throwable throwable
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != stationCall) {
                            return;
                        }

                        showDetailsError(
                                R.string.booking_details_network_error,
                                true
                        );
                    }
                });
    }


    /**
     * Loads the charger and verifies station ownership.
     */
    private void loadChargerDetails() {

        if (!isActivityAvailable()) {
            return;
        }

        chargerCall = chargerRepository.getChargerById(
                chargerId
        );

        chargerCall.enqueue(
                new Callback<ChargerResponse>() {

                    @Override
                    public void onResponse(
                            Call<ChargerResponse> call,
                            Response<ChargerResponse> response
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != chargerCall) {
                            return;
                        }

                        if (!response.isSuccessful()) {

                            handleDetailsHttpError(response.code());
                            return;
                        }

                        ChargerResponse charger = response.body();

                        if (charger == null
                                || charger.getId() == null
                                || charger.getId() != chargerId
                                || charger.getChargingStationId() == null
                                || charger.getChargingStationId() != stationId) {

                            showDetailsError(
                                    R.string.booking_details_mismatch,
                                    false
                            );

                            return;
                        }

                        selectedCharger = charger;

                        loadConnectorDetails();
                    }

                    @Override
                    public void onFailure(
                            Call<ChargerResponse> call,
                            Throwable throwable
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != chargerCall) {
                            return;
                        }

                        showDetailsError(
                                R.string.booking_details_network_error,
                                true
                        );
                    }
                });
    }


    /**
     * Loads the connector and verifies its charger relationship.
     */
    private void loadConnectorDetails() {

        if (!isActivityAvailable()) {
            return;
        }

        connectorCall = connectorRepository.getConnectorById(
                connectorId
        );

        connectorCall.enqueue(
                new Callback<ConnectorResponse>() {

                    @Override
                    public void onResponse(
                            Call<ConnectorResponse> call,
                            Response<ConnectorResponse> response
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != connectorCall) {
                            return;
                        }

                        if (!response.isSuccessful()) {

                            handleDetailsHttpError(response.code());
                            return;
                        }

                        ConnectorResponse connector = response.body();

                        if (connector == null
                                || connector.getId() == null
                                || connector.getId() != connectorId
                                || connector.getChargerId() == null
                                || connector.getChargerId() != chargerId
                                || connector.getConnectorNumber() == null
                                || connector.getConnectorNumber() <= 0) {

                            showDetailsError(
                                    R.string.booking_details_mismatch,
                                    false
                            );

                            return;
                        }

                        String status = connector.getStatus();

                        boolean supportedStatus =
                                "AVAILABLE".equalsIgnoreCase(status)
                                        || "OCCUPIED".equalsIgnoreCase(status);

                        if (!supportedStatus) {

                            showDetailsError(
                                    R.string.booking_details_load_error,
                                    false
                            );

                            return;
                        }

                        selectedConnector = connector;

                        displayBookingDetails();
                    }

                    @Override
                    public void onFailure(
                            Call<ConnectorResponse> call,
                            Throwable throwable
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != connectorCall) {
                            return;
                        }

                        showDetailsError(
                                R.string.booking_details_network_error,
                                true
                        );
                    }
                });
    }


    /** Displays verified booking details. */
    private void displayBookingDetails() {
        tvBookingStationName.setText(safeText(selectedStation.getCpoName()));
        tvBookingStationAddress.setText(safeText(selectedStation.getLocation()));
        tvBookingChargerType.setText(getString(
                R.string.booking_charger_format,
                safeText(selectedCharger.getChargerType())));
        tvBookingConnectorNumber.setText(getString(
                R.string.booking_connector_status_format,
                selectedConnector.getConnectorNumber(),
                safeText(selectedConnector.getStatus())));

        BigDecimal price = selectedCharger.getPricePerKwh();
        if (price != null) {
            tvBookingPricePerKwh.setText(getString(
                    R.string.booking_price_format,
                    price.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        } else {
            tvBookingPricePerKwh.setText(R.string.booking_price_placeholder);
        }
        detailsLoaded = true;
        setDetailsLoading(false);
        if (creationLocked) showBookingMessage(getString(R.string.booking_create_uncertain), true);
    }

    /** Provides a fallback for missing display values. */
    private String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return getString(R.string.booking_unknown);
        }
        return value.trim();
    }

    /** Handles HTTP errors while loading details. */
    private void handleDetailsHttpError(int statusCode) {
        if (statusCode == 401) {
            sessionExecutor.execute(() -> {
                boolean loggedIn = sessionManager.isLoggedIn();
                runOnUiThread(() -> {
                    if (!isActivityAvailable()) {
                        return;
                    }
                    if (!loggedIn) {
                        navigateToLogin();
                    } else {
                        showDetailsError(R.string.booking_details_load_error, true);
                    }
                });
            });
        } else if (statusCode == 403) {
            showDetailsError(R.string.booking_access_denied, false);
        } else if (statusCode == 404) {
            showDetailsError(R.string.booking_details_load_error, false);
        } else {
            showDetailsError(R.string.booking_details_load_error, true);
        }
    }

    /** Displays a recoverable or terminal loading error. */
    private void showDetailsError(int messageRes, boolean retryAllowed) {
        if (!isActivityAvailable()) {
            return;
        }
        detailsLoaded = false;
        resetBookingPreview();
        setDetailsLoading(false);
        tvBookingStationName.setText(messageRes);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(messageRes)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> finish());
        if (retryAllowed) {
            builder.setPositiveButton(R.string.booking_details_retry,
                    (dialog, which) -> validateSessionAndLoadDetails());
        }
        builder.show();
    }

    /** Cancels active detail requests. */
    private void cancelDetailRequests() {
        if (stationCall != null) {
            stationCall.cancel();
            stationCall = null;
        }
        if (chargerCall != null) {
            chargerCall.cancel();
            chargerCall = null;
        }
        if (connectorCall != null) {
            connectorCall.cancel();
            connectorCall = null;
        }
    }


    /**
     * Requests actual slot availability and pricing
     * from the Spring Boot backend.
     */
    private void checkSlotAvailability() {

        if (!isActivityAvailable()
                || !detailsLoaded
                || detailsLoading
                || previewLoading || creationLoading || creationLocked) {
            return;
        }

        if (!validateSelectedSlot(true)) {
            return;
        }

        BookingRequest request = buildBookingRequest();

        if (request == null) {
            return;
        }

        resetBookingPreview();

        setPreviewLoading(true);

        showBookingMessage(
                getString(R.string.booking_preview_loading),
                false
        );

        previewCall = bookingRepository.previewBooking(request);

        previewCall.enqueue(
                new Callback<BookingPreviewResponse>() {

                    @Override
                    public void onResponse(
                            Call<BookingPreviewResponse> call,
                            Response<BookingPreviewResponse> response
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != previewCall) {
                            return;
                        }

                        previewCall = null;

                        setPreviewLoading(false);

                        if (!response.isSuccessful()) {

                            handlePreviewHttpError(response.code());
                            return;
                        }

                        BookingPreviewResponse preview =
                                response.body();

                        if (preview == null
                                || preview.getConnectorId() == null
                                || preview.getConnectorId() != connectorId
                                || !request.getStartTime().equals(
                                preview.getStartTime())
                                || !request.getEndTime().equals(
                                preview.getEndTime())
                                || !validateSelectedSlot(false)) {

                            showBookingMessage(
                                    getString(
                                            R.string.booking_preview_invalid
                                    ),
                                    true
                            );

                            return;
                        }

                        if (!preview.isAvailable()) {

                            bookingPreview = null;

                            cardBookingSummary.setVisibility(View.GONE);

                            String message = preview.getMessage();

                            showBookingMessage(
                                    message == null || message.trim().isEmpty()
                                            ? getString(
                                            R.string.booking_preview_unavailable
                                    )
                                            : message,
                                    true
                            );

                            updateAvailabilityButton();

                            return;
                        }

                        if (!hasValidPreviewAmounts(preview)) {

                            showBookingMessage(
                                    getString(
                                            R.string.booking_preview_invalid
                                    ),
                                    true
                            );

                            return;
                        }

                        bookingPreview = preview;

                        displayBookingPreview(preview);
                    }

                    @Override
                    public void onFailure(
                            Call<BookingPreviewResponse> call,
                            Throwable throwable
                    ) {

                        if (!isActivityAvailable()
                                || call.isCanceled()
                                || call != previewCall) {
                            return;
                        }

                        previewCall = null;

                        setPreviewLoading(false);

                        showBookingMessage(
                                getString(
                                        R.string.booking_details_network_error
                                ),
                                true
                        );
                    }
                });
    }

    /**
     * Validates the required backend pricing fields.
     */
    private boolean hasValidPreviewAmounts(
            BookingPreviewResponse preview
    ) {

        return preview.getDurationMinutes()
                == calculateDurationMinutes(
                selectedStartTime,
                selectedEndTime
        )
                && preview.getPricePerKwh() != null
                && preview.getPricePerKwh().signum() >= 0
                && preview.getEstimatedEnergy() != null
                && preview.getEstimatedEnergy().signum() > 0
                && preview.getEstimatedAmount() != null
                && preview.getEstimatedAmount().signum() >= 0
                && preview.getAdvanceAmount() != null
                && preview.getAdvanceAmount().signum() >= 0
                && preview.getAdvanceAmount().compareTo(
                preview.getEstimatedAmount()
        ) <= 0;
    }

    /**
     * Displays the actual backend booking estimate.
     */
    private void displayBookingPreview(
            BookingPreviewResponse preview
    ) {

        tvBookingPricePerKwh.setText(
                getString(
                        R.string.booking_price_format,
                        formatBookingDecimal(
                                preview.getPricePerKwh()
                        )
                )
        );

        tvBookingDuration.setText(
                getString(
                        R.string.booking_duration_format,
                        preview.getDurationMinutes()
                )
        );

        tvBookingEstimatedEnergy.setText(
                getString(
                        R.string.booking_energy_format,
                        formatBookingDecimal(
                                preview.getEstimatedEnergy()
                        )
                )
        );

        tvBookingEstimatedAmount.setText(
                getString(
                        R.string.booking_amount_format,
                        formatBookingDecimal(
                                preview.getEstimatedAmount()
                        )
                )
        );

        tvBookingAdvanceAmount.setText(
                getString(
                        R.string.booking_advance_format,
                        formatBookingDecimal(
                                preview.getAdvanceAmount()
                        )
                )
        );

        cardBookingSummary.setVisibility(View.VISIBLE);

        showBookingMessage(
                getString(R.string.booking_preview_available),
                false
        );

        tvBookingAvailabilityMessage.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.ev_success
                )
        );

        updateAvailabilityButton();
    }

    /**
     * Formats backend decimal values for display.
     */
    private String formatBookingDecimal(BigDecimal value) {

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        ).toPlainString();
    }

    /**
     * Prevents duplicate preview requests and slot
     * changes while the preview request is running.
     */
    private void setPreviewLoading(boolean loading) {

        previewLoading = loading;

        pbBooking.setVisibility(
                loading || detailsLoading
                        ? View.VISIBLE
                        : View.GONE
        );

        boolean selectionEnabled =
                detailsLoaded
                        && !detailsLoading
                        && !previewLoading;

        btnBookingDate.setEnabled(selectionEnabled);

        btnBookingStartTime.setEnabled(selectionEnabled);

        btnBookingEndTime.setEnabled(selectionEnabled);

        updateAvailabilityButton();
    }

    /**
     * Handles unsuccessful booking preview responses.
     */
    private void handlePreviewHttpError(int statusCode) {

        if (statusCode == 401) {

            sessionExecutor.execute(() -> {

                boolean loggedIn = sessionManager.isLoggedIn();

                runOnUiThread(() -> {

                    if (!isActivityAvailable()) {
                        return;
                    }

                    if (!loggedIn) {

                        navigateToLogin();

                    } else {

                        showBookingMessage(
                                getString(
                                        R.string.booking_preview_error
                                ),
                                true
                        );
                    }
                });
            });

        } else if (statusCode == 403) {

            showBookingMessage(
                    getString(R.string.booking_access_denied),
                    true
            );

        } else {

            showBookingMessage(
                    getString(R.string.booking_preview_error),
                    true
            );
        }
    }

    /**
     * Cancels any active booking preview request.
     */
    private void cancelPreviewRequest() {

        if (previewCall != null) {

            previewCall.cancel();

            previewCall = null;
        }

        previewLoading = false;
    }


    private void confirmBookingCreation() {
        if (!isActivityAvailable() || creationLoading || creationLocked
                || !detailsLoaded || detailsLoading || previewLoading
                || bookingPreview == null || !bookingPreview.isAvailable()
                || !validateSelectedSlot(true)) return;
        BookingRequest request = buildBookingRequest();
        if (request == null || !request.getStartTime().equals(bookingPreview.getStartTime())
                || !request.getEndTime().equals(bookingPreview.getEndTime())) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.booking_create_title)
                .setMessage(getString(R.string.booking_create_confirm,
                        formatBookingDecimal(bookingPreview.getAdvanceAmount())))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.booking_create_action, (dialog, which) -> createPendingBooking(request))
                .show();
    }

    private void createPendingBooking(BookingRequest request) {
        if (!isActivityAvailable() || creationLoading || creationLocked
                || bookingPreview == null || !validateSelectedSlot(true)) return;
        creationLocked = true;
        setCreationLoading(true);
        showBookingMessage(getString(R.string.booking_create_loading), false);
        creationCall = bookingRepository.createBooking(request);
        creationCall.enqueue(new Callback<BookingResponse>() {
            @Override public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                if (!isActivityAvailable() || call.isCanceled() || call != creationCall) return;
                creationCall = null;
                if (!response.isSuccessful()) {
                    int code = response.code();
                    if (code == 400 || code == 401 || code == 403 || code == 404 || code == 409) {
                        creationLocked = false;
                        resetBookingPreview();
                        setCreationLoading(false);
                        if (code == 401 || code == 403) handlePreviewHttpError(code);
                        else showBookingMessage(getString(R.string.booking_create_rejected), true);
                    } else showUncertainCreation();
                    return;
                }
                BookingResponse result = response.body();
                if (result == null || result.getId() == null || result.getId() <= 0
                        || result.getConnectorId() == null || result.getConnectorId() != connectorId
                        || !request.getStartTime().equals(result.getStartTime())
                        || !request.getEndTime().equals(result.getEndTime())
                        || !"PENDING".equals(result.getStatus()) || !"PENDING".equals(result.getPaymentStatus())
                        || result.getEstimatedAmount() == null || result.getAdvanceAmount() == null
                        || result.getPaymentExpiresAt() == null || result.getPaymentExpiresAt().trim().isEmpty()) {
                    showUncertainCreation();
                    return;
                }
                setCreationLoading(false);
                showBookingMessage(getString(R.string.booking_create_success, result.getId()), false);
                tvBookingAvailabilityMessage.setTextColor(ContextCompat.getColor(SlotBookingActivity.this, R.color.ev_success));
                new AlertDialog.Builder(SlotBookingActivity.this)
                        .setTitle(R.string.booking_create_success_title)
                        .setMessage(getString(R.string.booking_create_result, result.getId(),
                                formatBookingDecimal(result.getEstimatedAmount()),
                                formatBookingDecimal(result.getAdvanceAmount()), result.getStatus(),
                                result.getPaymentStatus(), result.getPaymentExpiresAt().replace('T', ' ')))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                        .show();
            }
            @Override public void onFailure(Call<BookingResponse> call, Throwable throwable) {
                if (!isActivityAvailable() || call.isCanceled() || call != creationCall) return;
                creationCall = null;
                showUncertainCreation();
            }
        });
    }

    private void showUncertainCreation() {
        creationLocked = true;
        setCreationLoading(false);
        showBookingMessage(getString(R.string.booking_create_uncertain), true);
        new AlertDialog.Builder(this).setMessage(R.string.booking_create_uncertain)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void setCreationLoading(boolean loading) {
        creationLoading = loading;
        pbBooking.setVisibility(loading ? View.VISIBLE : View.GONE);
        boolean enabled = detailsLoaded && !detailsLoading && !previewLoading && !loading && !creationLocked;
        btnBookingDate.setEnabled(enabled);
        btnBookingStartTime.setEnabled(enabled);
        btnBookingEndTime.setEnabled(enabled);
        updateAvailabilityButton();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("creation_locked", creationLocked || creationLoading);
        super.onSaveInstanceState(outState);
    }

    /**
     * Releases network and background resources.
     */
    @Override
    protected void onDestroy() {

        cancelPreviewRequest();
        if (creationCall != null) {
            creationCall.cancel();
            creationCall = null;
        }

        cancelDetailRequests();

        sessionExecutor.shutdownNow();

        super.onDestroy();
    }
    /**
     * Clears previous availability and pricing
     * whenever the selected booking slot changes.
     */
    private void resetBookingPreview() {

        cancelPreviewRequest();

        bookingPreview = null;

        cardBookingSummary.setVisibility(View.GONE);

        btnProceedBooking.setEnabled(false);

        tvBookingAvailabilityMessage.setText("");

        tvBookingAvailabilityMessage.setVisibility(View.GONE);
    }

    /**
     * Creates the backend-compatible booking request.
     */
    private BookingRequest buildBookingRequest() {

        if (!validateSelectedSlot(false)) {
            return null;
        }

        return new BookingRequest(
                connectorId,
                formatApiDateTime(selectedStartTime),
                formatApiDateTime(selectedEndTime)
        );
    }

    /**
     * Formats a date for display.
     */
    private String formatDisplayDate(Calendar date) {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.ENGLISH
                );

        return formatter.format(date.getTime());
    }

    /**
     * Formats a time for display.
     */
    private String formatDisplayTime(Calendar time) {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "hh:mm a",
                        Locale.ENGLISH
                );

        return formatter.format(time.getTime());
    }

    /**
     * Formats a date-time for Spring Boot LocalDateTime.
     */
    private String formatApiDateTime(Calendar dateTime) {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        Locale.US
                );

        return formatter.format(dateTime.getTime());
    }

    /**
     * Displays validation or booking information.
     */
    private void showBookingMessage(
            String message,
            boolean isError
    ) {

        tvBookingAvailabilityMessage.setText(message);

        tvBookingAvailabilityMessage.setTextColor(
                ContextCompat.getColor(
                        this,
                        isError
                                ? R.color.ev_error
                                : R.color.ev_text_secondary
                )
        );

        tvBookingAvailabilityMessage.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (btnCheckAvailability != null) {

            updateAvailabilityButton();
        }
    }

}
