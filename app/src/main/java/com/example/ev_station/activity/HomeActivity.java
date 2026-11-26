package com.example.ev_station.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.example.ev_station.R;
import com.example.ev_station.session.SessionManager;
import com.example.ev_station.utils.SessionNavigator;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Authenticated USER dashboard.
 *
 * Handles session validation, charging station navigation
 * and secure logout.
 */
public class HomeActivity extends AppCompatActivity {

    // Secure session management.
    private SessionManager sessionManager;

    // Dashboard views.
    private TextView tvUserGreeting;
    private ImageView ivMenu;

    // Charging station navigation views.
    private TextView tvViewAllStations;
    private LinearLayout layoutBrowseStations;
    private TextView navStations;
    private LinearLayout layoutNearbyStations;

    // Prevent blocking the main UI thread.
    private final ExecutorService sessionExecutor =
            Executors.newSingleThreadExecutor();

    // Dashboard state.
    private boolean isLoggingOut = false;
    private boolean isSessionValid = false;
    private boolean isOpeningStations = false;

    // Prevent outdated session validation results.
    private int validationGeneration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(
                getApplicationContext()
        );

        initializeViews();

        setupListeners();

        setDashboardActionsEnabled(false);
    }

    /**
     * Initialize dashboard and navigation views.
     */
    private void initializeViews() {

        tvUserGreeting =
                findViewById(R.id.tvUserGreeting);

        ivMenu =
                findViewById(R.id.ivMenu);

        tvViewAllStations =
                findViewById(R.id.tvViewAllStations);

        layoutBrowseStations =
                findViewById(R.id.layoutBrowseStations);

        navStations =
                findViewById(R.id.navStations);

        layoutNearbyStations =
                findViewById(R.id.layoutNearbyStations);
    }

    /**
     * Configure dashboard menu and station navigation.
     */
    private void setupListeners() {

        ivMenu.setOnClickListener(
                this::showHomeMenu
        );

        tvViewAllStations.setOnClickListener(
                view -> openStationList()
        );

        layoutBrowseStations.setOnClickListener(
                view -> openStationList()
        );

        navStations.setOnClickListener(
                view -> openStationList()
        );

        layoutNearbyStations.setOnClickListener(
                view -> openMap()
        );
    }

    /**
     * Validate the session whenever the dashboard
     * becomes active.
     */
    @Override
    protected void onResume() {

        super.onResume();

        isOpeningStations = false;

        if (!isLoggingOut) {

            validateSession();
        }
    }

    /**
     * Disable dashboard actions while the activity
     * is not in the foreground.
     */
    @Override
    protected void onPause() {

        super.onPause();

        validationGeneration++;

        isSessionValid = false;

        setDashboardActionsEnabled(false);
    }

    /**
     * Validate the authenticated USER session
     * outside the main UI thread.
     */
    private void validateSession() {

        if (isFinishing()
                || isDestroyed()
                || isLoggingOut) {

            return;
        }

        isSessionValid = false;

        setDashboardActionsEnabled(false);

        final int currentGeneration =
                ++validationGeneration;

        sessionExecutor.execute(() -> {

            String role = sessionManager.getRole();

            String name = sessionManager.getName();

            boolean validSession =
                    "USER".equals(role)
                            && name != null
                            && !name.trim().isEmpty();

            runOnUiThread(() -> {

                if (isFinishing()
                        || isDestroyed()
                        || isLoggingOut
                        || currentGeneration
                        != validationGeneration) {

                    return;
                }

                if (!validSession) {

                    SessionNavigator.navigateToLogin(
                            HomeActivity.this
                    );

                    return;
                }

                isSessionValid = true;

                tvUserGreeting.setText(
                        getString(
                                R.string.home_user_greeting,
                                name
                        )
                );

                setDashboardActionsEnabled(true);
            });
        });
    }

    /**
     * Enable or disable authenticated dashboard actions.
     */
    private void setDashboardActionsEnabled(
            boolean enabled

    ) {

        ivMenu.setEnabled(enabled);

        tvViewAllStations.setEnabled(enabled);

        layoutBrowseStations.setEnabled(enabled);

        navStations.setEnabled(enabled);

        layoutNearbyStations.setEnabled(enabled);
    }

    /**
     * Open the charging station map after session validation.
     */
    private void openMap() {

        if (!isSessionValid
                || isLoggingOut
                || isOpeningStations
                || isFinishing()
                || isDestroyed()) {

            return;
        }

        isOpeningStations = true;

        setDashboardActionsEnabled(false);

        Intent intent = new Intent(
                HomeActivity.this,
                MapActivity.class
        );

        startActivity(intent);
    }
    /**
     * Open the charging station list.
     *
     * Prevent duplicate navigation and restrict access
     * until the USER session has been validated.
     */
    private void openStationList() {

        if (!isSessionValid
                || isLoggingOut
                || isOpeningStations
                || isFinishing()
                || isDestroyed()) {

            return;
        }

        isOpeningStations = true;

        setDashboardActionsEnabled(false);

        Intent intent = new Intent(
                HomeActivity.this,
                StationListActivity.class
        );

        startActivity(intent);
    }

    /**
     * Display dashboard popup menu.
     */
    private void showHomeMenu(View anchor) {

        if (!isSessionValid
                || isLoggingOut
                || isOpeningStations) {

            return;
        }

        PopupMenu popupMenu =
                new PopupMenu(this, anchor);

        popupMenu.getMenuInflater().inflate(
                R.menu.home_menu,
                popupMenu.getMenu()
        );

        popupMenu.setOnMenuItemClickListener(item -> {

            if (item.getItemId()
                    == R.id.action_logout) {

                showLogoutConfirmation();

                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    /**
     * Confirm logout before clearing the session.
     */
    private void showLogoutConfirmation() {

        if (!isSessionValid
                || isLoggingOut
                || isOpeningStations) {

            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setNegativeButton(
                        R.string.logout_cancel,
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        R.string.logout,
                        (dialog, which) -> performLogout()
                )
                .show();
    }

    /**
     * Clear encrypted session data and return
     * to the authentication screen.
     */
    private void performLogout() {

        if (isLoggingOut
                || isOpeningStations
                || isFinishing()
                || isDestroyed()) {

            return;
        }

        isLoggingOut = true;

        isSessionValid = false;

        validationGeneration++;

        setDashboardActionsEnabled(false);

        sessionExecutor.execute(() -> {

            boolean sessionCleared =
                    sessionManager.clearSession();

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (!sessionCleared) {

                    isLoggingOut = false;

                    Toast.makeText(
                            HomeActivity.this,
                            R.string.logout_failed,
                            Toast.LENGTH_SHORT
                    ).show();

                    validateSession();

                    return;
                }

                Toast.makeText(
                        HomeActivity.this,
                        R.string.logout_success,
                        Toast.LENGTH_SHORT
                ).show();

                SessionNavigator.navigateToLogin(
                        HomeActivity.this
                );
            });
        });
    }

    /**
     * Release background session resources.
     */
    @Override
    protected void onDestroy() {

        validationGeneration++;

        sessionExecutor.shutdown();

        super.onDestroy();
    }
}
