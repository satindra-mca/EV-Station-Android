
package com.example.ev_station.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.ev_station.R;
import com.example.ev_station.session.SessionManager;
import com.example.ev_station.utils.SessionNavigator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1500;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final ExecutorService sessionExecutor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Enable full-screen layout.
        WindowCompat.setDecorFitsSystemWindows(
                getWindow(), false
        );

        setContentView(R.layout.activity_splash_screen);

        // Preserve the existing splash appearance.
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        getWindow(),
                        getWindow().getDecorView()
                );

        controller.hide(
                WindowInsetsCompat.Type.statusBars()
        );

        // Restore the authenticated session.
        handler.postDelayed(
                this::restoreSession,
                SPLASH_DELAY
        );
    }

    /**
     * Read and validate the encrypted session
     * outside the main UI thread.
     */
    private void restoreSession() {

        sessionExecutor.execute(() -> {

            SessionManager sessionManager =
                    new SessionManager(
                            getApplicationContext()
                    );

            /*
             * getRole() returns null when the session
             * is missing, invalid or expired.
             */
            String role = sessionManager.getRole();

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                boolean navigated =
                        SessionNavigator.navigateToDashboard(
                                SplashActivity.this,
                                role
                        );

                if (!navigated) {

                    SessionNavigator.navigateToLogin(
                            SplashActivity.this
                    );
                }
            });
        });
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        sessionExecutor.shutdown();

        super.onDestroy();
    }
}
