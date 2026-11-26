package com.example.ev_station.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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

public class HomeActivity extends AppCompatActivity {

    // Secure session management.
    private SessionManager sessionManager;

    // Dashboard views.
    private TextView tvUserGreeting;
    private ImageView ivMenu;

    // Prevent blocking the main UI thread.
    private final ExecutorService sessionExecutor =
            Executors.newSingleThreadExecutor();

    // Prevent duplicate logout operations.
    private boolean isLoggingOut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(
                getApplicationContext()
        );

        initializeViews();

        setupListeners();
    }

    /**
     * Initialize dashboard views.
     */
    private void initializeViews() {

        tvUserGreeting =
                findViewById(R.id.tvUserGreeting);

        ivMenu =
                findViewById(R.id.ivMenu);
    }

    /**
     * Configure dashboard menu.
     */
    private void setupListeners() {

        ivMenu.setOnClickListener(
                this::showHomeMenu
        );
    }

    /**
     * Validate the session whenever the dashboard
     * becomes active.
     */
    @Override
    protected void onResume() {

        super.onResume();

        if (!isLoggingOut) {

            validateSession();
        }
    }

    /**
     * Validate the authenticated USER session
     * outside the main UI thread.
     */
    private void validateSession() {

        ivMenu.setEnabled(false);

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
                        || isLoggingOut) {

                    return;
                }

                if (!validSession) {

                    SessionNavigator.navigateToLogin(
                            HomeActivity.this
                    );

                    return;
                }

                tvUserGreeting.setText(
                        getString(
                                R.string.home_user_greeting,
                                name
                        )
                );

                ivMenu.setEnabled(true);
            });
        });
    }

    /**
     * Display dashboard popup menu.
     */
    private void showHomeMenu(View anchor) {

        if (isLoggingOut) {
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

        if (isLoggingOut) {
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

        if (isLoggingOut) {
            return;
        }

        isLoggingOut = true;

        ivMenu.setEnabled(false);

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

        sessionExecutor.shutdown();

        super.onDestroy();
    }
}
