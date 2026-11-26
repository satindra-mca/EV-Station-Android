package com.example.ev_station.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ev_station.R;
import com.example.ev_station.model.request.LoginRequest;
import com.example.ev_station.model.response.ApiErrorResponse;
import com.example.ev_station.model.response.LoginResponse;
import com.example.ev_station.repository.AuthRepository;
import com.example.ev_station.utils.ApiErrorParser;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.net.SocketTimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.ev_station.session.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.ev_station.utils.SessionNavigator;

public class LoginActivity extends AppCompatActivity {

    // Input containers
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;

    // Input fields
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;

    // Action views
    private MaterialButton btnLogin;
    private TextView tvRegister;
    private CircularProgressIndicator progressLogin;

    // Authentication repository
    private AuthRepository authRepository;

    // Secure authentication session manager.
    private SessionManager sessionManager;

    // Execute session encryption outside the main UI thread.
    private final ExecutorService sessionExecutor =
            Executors.newSingleThreadExecutor();

    // Active login request
    private Call<LoginResponse> loginCall;

    // Prevent duplicate login requests
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();

        // Initialize authentication repository.
        authRepository = new AuthRepository(this);

        // Initialize secure session management.
        sessionManager = new SessionManager(
                getApplicationContext()
        );

        setupListeners();
        setupErrorListeners();
        setupFocusValidation();
    }

    /**
     * Initialize all login screen views.
     */
    private void initializeViews() {

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressLogin = findViewById(R.id.progressLogin);
    }

    /**
     * Configure login screen event listeners.
     */
    private void setupListeners() {

        // Handle login button click.
        btnLogin.setOnClickListener(view -> handleLogin());

        // Submit using the keyboard Done action.
        etPassword.setOnEditorActionListener(
                (view, actionId, event) -> {

                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        handleLogin();
                        return true;
                    }

                    return false;
                }
        );

        // Enable registration navigation.
        tvRegister.setEnabled(true);

        // Navigate to the registration screen.
        tvRegister.setOnClickListener(view -> {

            if (isLoading) {
                return;
            }

            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);
        });
    }

    /**
     * Read and validate login credentials.
     */
    private void handleLogin() {

        // Prevent duplicate API requests.
        if (isLoading) {
            return;
        }

        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim()
                : "";

        String password = etPassword.getText() != null
                ? etPassword.getText().toString()
                : "";

        if (!validateInputs(email, password)) {
            return;
        }

        // Create the authentication request.
        LoginRequest request = new LoginRequest(
                email,
                password
        );

        // Execute the login API.
        executeLogin(request);
    }

    /**
     * Executes the login API asynchronously.
     */
    private void executeLogin(LoginRequest request) {

        setLoading(true);

        loginCall = authRepository.login(request);

        loginCall.enqueue(new Callback<LoginResponse>() {


            @Override
            public void onResponse(
                    Call<LoginResponse> call,
                    Response<LoginResponse> response
            ) {

                // Ignore callbacks after Activity destruction.
                if (call.isCanceled()
                        || isFinishing()
                        || isDestroyed()) {

                    return;
                }

                loginCall = null;

                if (response.isSuccessful()) {

                    LoginResponse loginResponse = response.body();

                    if (isValidLoginResponse(loginResponse)) {

                        /*
                         * Keep the loading state active until
                         * secure session storage is completed.
                         */
                        handleLoginSuccess(loginResponse);

                    } else {

                        setLoading(false);

                        showMessage(
                                getString(
                                        R.string.login_invalid_response
                                )
                        );
                    }

                } else {

                    setLoading(false);

                    handleApiError(response);
                }
            }


            @Override
            public void onFailure(
                    Call<LoginResponse> call,
                    Throwable throwable
            ) {

                // Ignore cancelled requests.
                if (call.isCanceled() ||
                        isFinishing() ||
                        isDestroyed()) {
                    return;
                }

                loginCall = null;

                setLoading(false);

                handleNetworkFailure(throwable);
            }
        });
    }

    /**
     * Validates the required successful login response fields.
     */
    private boolean isValidLoginResponse(
            LoginResponse response
    ) {

        return response != null
                && response.getToken() != null
                && !response.getToken().trim().isEmpty()
                && response.getUserId() != null
                && response.getUserId() > 0
                && response.getName() != null
                && !response.getName().trim().isEmpty()
                && response.getEmail() != null
                && !response.getEmail().trim().isEmpty()
                && response.getRole() != null
                && !response.getRole().trim().isEmpty();
    }



    /**
     * Persist the authenticated session and navigate
     * to the dashboard supported by the user role.
     */
    private void handleLoginSuccess(
            LoginResponse response
    ) {

        // Remove the password from the input field.
        etPassword.setText("");

        sessionExecutor.execute(() -> {

            String role = response.getRole();

            /*
             * Only the USER dashboard is currently available.
             * Do not persist unsupported dashboard sessions.
             */
            if (!"USER".equals(role)) {

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    setLoading(false);

                    showMessage(
                            getString(
                                    R.string.dashboard_not_available
                            )
                    );
                });

                return;
            }

            boolean sessionSaved =
                    sessionManager.saveSession(
                            response.getToken(),
                            response.getUserId(),
                            response.getName(),
                            response.getEmail(),
                            role
                    );

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);

                if (!sessionSaved) {

                    showMessage(
                            getString(
                                    R.string.login_session_save_failed
                            )
                    );

                    return;
                }

                showMessage(
                        getString(R.string.login_success)
                );

                SessionNavigator.navigateToDashboard(
                        LoginActivity.this,
                        role
                );
            });
        });
    }



    /**
     * Handles unsuccessful HTTP responses.
     */
    private void handleApiError(
            Response<LoginResponse> response
    ) {

        ApiErrorResponse apiError =
                ApiErrorParser.parseError(response);

        int statusCode = response.code();

        // Handle invalid login credentials.
        if (statusCode == 401) {

            tilPassword.setError(
                    getString(
                            R.string.login_invalid_credentials
                    )
            );

            etPassword.requestFocus();

            return;
        }

        // Handle other backend API errors.
        String message = ApiErrorParser.getErrorMessage(
                statusCode,
                apiError
        );

        showMessage(message);
    }

    /**
     * Handles network and unexpected request failures.
     */
    private void handleNetworkFailure(Throwable throwable) {

        if (throwable instanceof SocketTimeoutException) {

            showMessage(
                    getString(R.string.api_timeout_error)
            );

        } else if (throwable instanceof IOException) {

            showMessage(
                    getString(R.string.api_network_error)
            );

        } else {

            showMessage(
                    getString(R.string.api_unexpected_error)
            );
        }
    }

    /**
     * Updates the login screen loading state.
     */
    private void setLoading(boolean loading) {

        isLoading = loading;

        // Disable input fields during the API request.
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);

        // Disable duplicate submissions and navigation.
        btnLogin.setEnabled(!loading);
        tvRegister.setEnabled(!loading);

        // Show or hide the progress indicator.
        progressLogin.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        // Update the login button text.
        btnLogin.setText(
                loading
                        ? R.string.login_loading
                        : R.string.login_button
        );
    }

    /**
     * Displays a user-friendly message.
     */
    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * Validate email and password inputs.
     *
     * @param email User email address
     * @param password User password
     * @return true if all inputs are valid
     */
    private boolean validateInputs(
            String email,
            String password
    ) {

        // Clear previous validation errors.
        tilEmail.setError(null);
        tilPassword.setError(null);

        boolean isValid = true;

        // Validate email address.
        if (email.isEmpty()) {

            tilEmail.setError(
                    getString(R.string.auth_email_required)
            );

            isValid = false;

        } else if (!Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {

            tilEmail.setError(
                    getString(R.string.auth_email_invalid)
            );

            isValid = false;
        }

        // Validate password.
        if (password.isEmpty()) {

            tilPassword.setError(
                    getString(R.string.auth_password_required)
            );

            isValid = false;
        }

        // Focus the first invalid field.
        if (tilEmail.getError() != null) {

            etEmail.requestFocus();

        } else if (tilPassword.getError() != null) {

            etPassword.requestFocus();
        }

        return isValid;
    }

    /**
     * Configure login input error clearing listeners.
     */
    private void setupErrorListeners() {

        clearErrorOnTyping(etEmail, tilEmail);
        clearErrorOnTyping(etPassword, tilPassword);
    }

    /**
     * Clear the existing field error when the user
     * edits its value.
     */
    private void clearErrorOnTyping(
            TextInputEditText editText,
            TextInputLayout inputLayout
    ) {

        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {

                if (inputLayout.getError() != null) {
                    inputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Validate login fields when they lose focus.
     */
    private void setupFocusValidation() {

        // Email validation.
        etEmail.setOnFocusChangeListener((view, hasFocus) -> {

            if (!hasFocus && !isLoading) {

                String email = etEmail.getText() != null
                        ? etEmail.getText().toString().trim()
                        : "";

                if (email.isEmpty()) {

                    tilEmail.setError(
                            getString(R.string.auth_email_required)
                    );

                } else if (!Patterns.EMAIL_ADDRESS
                        .matcher(email).matches()) {

                    tilEmail.setError(
                            getString(R.string.auth_email_invalid)
                    );

                } else {

                    tilEmail.setError(null);
                }
            }
        });

        // Password validation.
        etPassword.setOnFocusChangeListener((view, hasFocus) -> {

            if (!hasFocus && !isLoading) {

                String password = etPassword.getText() != null
                        ? etPassword.getText().toString()
                        : "";

                if (password.isEmpty()) {

                    tilPassword.setError(
                            getString(
                                    R.string.auth_password_required
                            )
                    );

                } else {

                    tilPassword.setError(null);
                }
            }
        });
    }

    /**
     * Release authentication resources when the
     * Activity is destroyed.
     */
    @Override
    protected void onDestroy() {

        if (loginCall != null) {

            loginCall.cancel();
            loginCall = null;
        }

        // Release the executor after submitted tasks finish.
        sessionExecutor.shutdown();

        super.onDestroy();
    }
}
