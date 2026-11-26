package com.example.ev_station.utils;

import android.app.Activity;
import android.content.Intent;

import com.example.ev_station.activity.HomeActivity;
import com.example.ev_station.activity.LoginActivity;

/**
 * Central navigation utility for authenticated users.
 *
 * Dashboard navigation is determined by the user role.
 * Backend authorization remains the security authority.
 */
public final class SessionNavigator {

    private SessionNavigator() {
    }

    /**
     * Navigate to the dashboard supported by the role.
     *
     * Returns false when the dashboard is unavailable.
     */
    public static boolean navigateToDashboard(
            Activity activity,
            String role
    ) {

        if ("USER".equals(role)) {

            navigateAndClearTask(
                    activity,
                    HomeActivity.class
            );

            return true;
        }

        /*
         * OWNER and ADMIN dashboards will be
         * integrated in their respective modules.
         */
        return false;
    }

    /**
     * Navigate to login and clear the previous task.
     */
    public static void navigateToLogin(
            Activity activity
    ) {

        navigateAndClearTask(
                activity,
                LoginActivity.class
        );
    }

    /**
     * Prevent navigation back to authentication screens
     * after successful login or logout.
     */
    private static void navigateAndClearTask(
            Activity activity,
            Class<?> destination
    ) {

        Intent intent = new Intent(
                activity,
                destination
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        activity.startActivity(intent);
        activity.finish();
    }
}
