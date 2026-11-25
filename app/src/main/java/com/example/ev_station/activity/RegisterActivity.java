package com.example.ev_station.activity;

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
import com.example.ev_station.model.request.RegisterRequest;
import com.example.ev_station.model.response.RegisterResponse;
import com.example.ev_station.model.response.ApiErrorResponse;
import com.example.ev_station.repository.AuthRepository;
import com.example.ev_station.utils.ApiErrorParser;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    // Input layouts
    private TextInputLayout tilName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilMobile;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;

    // Input fields
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etMobile;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;

    // Action components
    private MaterialButton btnRegister;
    private TextView tvBack;
    private TextView tvLogin;
    private CircularProgressIndicator progressRegister;

    // Authentication repository
    private AuthRepository authRepository;

    // Active registration request
    private Call<RegisterResponse> registerCall;

    // Prevent duplicate registration requests
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();

        // Initialize authentication repository.
        authRepository = new AuthRepository();

        setupListeners();
        setupErrorListeners();
        setupFocusValidation();
    }

    /**
     * Initialize registration screen components.
     */
    private void initializeViews() {

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilMobile = findViewById(R.id.tilMobile);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvBack = findViewById(R.id.tvBack);
        tvLogin = findViewById(R.id.tvLogin);
        progressRegister = findViewById(R.id.progressRegister);
    }

    /**
     * Configure registration and navigation listeners.
     */
    private void setupListeners() {

        btnRegister.setOnClickListener(
                view -> handleRegistration()
        );

        // Return to the existing LoginActivity.
        tvBack.setOnClickListener(view -> {
            if (!isLoading) {
                finish();
            }
        });

        tvLogin.setOnClickListener(view -> {
            if (!isLoading) {
                finish();
            }
        });

        // Submit the form using the keyboard Done action.
        etConfirmPassword.setOnEditorActionListener(
                (view, actionId, event) -> {

                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        handleRegistration();
                        return true;
                    }

                    return false;
                }
        );
    }

    /**
     * Read and validate registration form inputs.
     */
    private void handleRegistration() {

        // Prevent duplicate API requests.
        if (isLoading) {
            return;
        }

        String name = getInputValue(etName).trim();
        String email = getInputValue(etEmail).trim();
        String mobile = getInputValue(etMobile).trim();

        // Preserve passwords exactly as entered.
        String password = getInputValue(etPassword);
        String confirmPassword = getInputValue(etConfirmPassword);

        if (!validateInputs(
                name,
                email,
                mobile,
                password,
                confirmPassword
        )) {
            return;
        }

        // Create the registration request.
        // Confirm password is only used for local validation.
        RegisterRequest request = new RegisterRequest(
                name,
                email,
                password,
                mobile
        );

        executeRegistration(request);
    }

    /**
     * Executes the registration API asynchronously.
     */
    private void executeRegistration(RegisterRequest request) {

        setLoading(true);

        registerCall = authRepository.register(request);

        registerCall.enqueue(new Callback<RegisterResponse>() {

            @Override
            public void onResponse(
                    Call<RegisterResponse> call,
                    Response<RegisterResponse> response
            ) {

                // Ignore callbacks after Activity destruction.
                if (call.isCanceled()
                        || isFinishing()
                        || isDestroyed()) {
                    return;
                }

                registerCall = null;

                setLoading(false);

                if (response.isSuccessful()) {

                    RegisterResponse registerResponse =
                            response.body();

                    if (isValidRegisterResponse(registerResponse)) {

                        handleRegistrationSuccess();

                    } else {

                        showMessage(
                                getString(
                                        R.string.register_invalid_response
                                )
                        );
                    }

                } else {

                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(
                    Call<RegisterResponse> call,
                    Throwable throwable
            ) {

                // Ignore cancelled requests.
                if (call.isCanceled()
                        || isFinishing()
                        || isDestroyed()) {
                    return;
                }

                registerCall = null;

                setLoading(false);

                handleNetworkFailure(throwable);
            }
        });
    }

    /**
     * Validates the required registration response fields.
     */
    private boolean isValidRegisterResponse(
            RegisterResponse response
    ) {

        return response != null
                && response.getId() != null
                && response.getId() > 0
                && response.getName() != null
                && !response.getName().trim().isEmpty()
                && response.getEmail() != null
                && !response.getEmail().trim().isEmpty()
                && response.getMobile() != null
                && !response.getMobile().trim().isEmpty()
                && "USER".equals(response.getRole());
    }

    /**
     * Handles successful account registration.
     */
    private void handleRegistrationSuccess() {

        // Prevent focus validation during screen exit.
        isLoading = true;

        // Clear sensitive input fields.
        etPassword.setText("");
        etConfirmPassword.setText("");

        showMessage(
                getString(R.string.register_success)
        );

        // Return to the existing LoginActivity.
        // Registration does not automatically authenticate the user.
        finish();
    }

    /**
     * Handles unsuccessful HTTP responses.
     */
    private void handleApiError(
            Response<RegisterResponse> response
    ) {

        ApiErrorResponse apiError =
                ApiErrorParser.parseError(response);

        int statusCode = response.code();

        // Handle duplicate email registration.
        if (statusCode == 409) {

            etEmail.requestFocus();

            tilEmail.setError(
                    getString(R.string.register_email_exists)
            );

            return;
        }

        // Handle backend field validation errors.
        if (statusCode == 400
                && apiError != null
                && apiError.getErrors() != null
                && !apiError.getErrors().isEmpty()) {

            boolean fieldErrorDisplayed =
                    displayFieldErrors(apiError.getErrors());

            if (fieldErrorDisplayed) {
                focusFirstInvalidField();
                return;
            }
        }

        // Handle remaining API errors.
        String message = ApiErrorParser.getErrorMessage(
                statusCode,
                apiError
        );

        showMessage(message);
    }

    /**
     * Maps backend validation errors to registration fields.
     */
    private boolean displayFieldErrors(
            Map<String, String> errors
    ) {

        boolean fieldErrorDisplayed = false;

        for (Map.Entry<String, String> entry : errors.entrySet()) {

            String field = entry.getKey();
            String message = entry.getValue();

            if (field == null
                    || message == null
                    || message.trim().isEmpty()) {
                continue;
            }

            switch (field) {

                case "name":
                    tilName.setError(message);
                    fieldErrorDisplayed = true;
                    break;

                case "email":
                    tilEmail.setError(message);
                    fieldErrorDisplayed = true;
                    break;

                case "mobile":
                    tilMobile.setError(message);
                    fieldErrorDisplayed = true;
                    break;

                case "password":
                    tilPassword.setError(message);
                    fieldErrorDisplayed = true;
                    break;

                default:
                    // Unknown fields are not mapped to the UI.
                    break;
            }
        }

        return fieldErrorDisplayed;
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
     * Updates the registration screen loading state.
     */
    private void setLoading(boolean loading) {

        isLoading = loading;

        // Disable input containers and their child views.
        tilName.setEnabled(!loading);
        tilEmail.setEnabled(!loading);
        tilMobile.setEnabled(!loading);
        tilPassword.setEnabled(!loading);
        tilConfirmPassword.setEnabled(!loading);

        // Prevent duplicate submissions and navigation.
        btnRegister.setEnabled(!loading);
        tvBack.setEnabled(!loading);
        tvLogin.setEnabled(!loading);

        // Show or hide the progress indicator.
        progressRegister.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        // Update the registration button text.
        btnRegister.setText(
                loading
                        ? R.string.register_loading
                        : R.string.register_button
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
     * Validate all registration fields.
     */
    private boolean validateInputs(
            String name,
            String email,
            String mobile,
            String password,
            String confirmPassword
    ) {

        clearErrors();

        boolean isValid = true;

        // Full name validation.
        if (name.isEmpty()) {

            tilName.setError(
                    getString(R.string.auth_name_required)
            );

            isValid = false;
        }

        // Email validation.
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

        // Mobile number validation.
        if (mobile.isEmpty()) {

            tilMobile.setError(
                    getString(R.string.auth_mobile_required)
            );

            isValid = false;

        }  else if (!mobile.matches("[6-9][0-9]{9}")) {

            tilMobile.setError(
                    getString(R.string.auth_mobile_invalid)
            );

            isValid = false;
        }

        // Password validation.
        if (password.trim().isEmpty()) {

            tilPassword.setError(
                    getString(R.string.auth_password_required)
            );

            isValid = false;

        } else if (password.length() < 6) {

            tilPassword.setError(
                    getString(R.string.auth_password_min_length)
            );

            isValid = false;
        }

        // Confirm password validation.
        if (confirmPassword.isEmpty()) {

            tilConfirmPassword.setError(
                    getString(R.string.auth_confirm_password_required)
            );

            isValid = false;

        } else if (!password.equals(confirmPassword)) {

            tilConfirmPassword.setError(
                    getString(R.string.auth_password_mismatch)
            );

            isValid = false;
        }

        // Focus the first invalid input field.
        if (!isValid) {
            focusFirstInvalidField();
        }

        return isValid;
    }

    /**
     * Clear previous validation errors.
     */
    private void clearErrors() {

        tilName.setError(null);
        tilEmail.setError(null);
        tilMobile.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    /**
     * Move focus to the first invalid input field.
     */
    private void focusFirstInvalidField() {

        if (tilName.getError() != null) {
            etName.requestFocus();

        } else if (tilEmail.getError() != null) {
            etEmail.requestFocus();

        } else if (tilMobile.getError() != null) {
            etMobile.requestFocus();

        } else if (tilPassword.getError() != null) {
            etPassword.requestFocus();

        } else if (tilConfirmPassword.getError() != null) {
            etConfirmPassword.requestFocus();
        }
    }

    /**
     * Safely retrieve text from an input field.
     */
    private String getInputValue(TextInputEditText editText) {

        return editText.getText() != null
                ? editText.getText().toString()
                : "";
    }

    /**
     * Configure registration input error clearing listeners.
     */
    private void setupErrorListeners() {

        clearErrorOnTyping(etName, tilName);
        clearErrorOnTyping(etEmail, tilEmail);
        clearErrorOnTyping(etMobile, tilMobile);
        clearErrorOnTyping(etPassword, tilPassword);
        clearErrorOnTyping(etConfirmPassword, tilConfirmPassword);
    }

    /**
     * Clear the field error when the user edits its value.
     */
    private void clearErrorOnTyping(
            TextInputEditText editText,
            TextInputLayout inputLayout
    ) {

        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count
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
     * Validate individual fields when they lose focus.
     */
    private void setupFocusValidation() {

        // Full name validation.
        etName.setOnFocusChangeListener((view, hasFocus) -> {

            if (!hasFocus && !isLoading) {

                String name = getInputValue(etName).trim();

                if (name.isEmpty()) {

                    tilName.setError(
                            getString(R.string.auth_name_required)
                    );

                } else {

                    tilName.setError(null);
                }
            }
        });

        // Email validation.
        etEmail.setOnFocusChangeListener((view, hasFocus) -> {

            if (!hasFocus && !isLoading) {

                String email = getInputValue(etEmail).trim();

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

                    // Preserve server-side errors until input changes.
                }
            }
        });

        // Mobile number validation.
        etMobile.setOnFocusChangeListener((view, hasFocus) -> {

            if (!hasFocus && !isLoading) {

                String mobile = getInputValue(etMobile).trim();

                if (mobile.isEmpty()) {

                    tilMobile.setError(
                            getString(R.string.auth_mobile_required)
                    );

                }  else if (!mobile.matches("[6-9][0-9]{9}")) {

                    tilMobile.setError(
                            getString(R.string.auth_mobile_invalid)
                    );

                } else {

                    tilMobile.setError(null);
                }
            }
        });

        // Password validation.
        etPassword.setOnFocusChangeListener((view, hasFocus) -> {

            if (!hasFocus && !isLoading) {

                String password = getInputValue(etPassword);

                if (password.trim().isEmpty()) {

                    tilPassword.setError(
                            getString(R.string.auth_password_required)
                    );

                } else if (password.length() < 6) {

                    tilPassword.setError(
                            getString(R.string.auth_password_min_length)
                    );

                } else {

                    tilPassword.setError(null);
                }

                // Recheck confirmation if already entered.
                if (!getInputValue(etConfirmPassword).isEmpty()) {
                    validateConfirmPasswordField();
                }
            }
        });

        // Confirm password validation.
        etConfirmPassword.setOnFocusChangeListener(
                (view, hasFocus) -> {

                    if (!hasFocus && !isLoading) {
                        validateConfirmPasswordField();
                    }
                }
        );
    }

    /**
     * Validate password confirmation.
     */
    private void validateConfirmPasswordField() {

        String password = getInputValue(etPassword);
        String confirmPassword = getInputValue(etConfirmPassword);

        if (confirmPassword.isEmpty()) {

            tilConfirmPassword.setError(
                    getString(R.string.auth_confirm_password_required)
            );

        } else if (!password.equals(confirmPassword)) {

            tilConfirmPassword.setError(
                    getString(R.string.auth_password_mismatch)
            );

        } else {

            tilConfirmPassword.setError(null);
        }
    }

    /**
     * Cancel the active registration request when the
     * Activity is destroyed.
     */
    @Override
    protected void onDestroy() {

        if (registerCall != null) {

            registerCall.cancel();
            registerCall = null;
        }

        super.onDestroy();
    }
}
