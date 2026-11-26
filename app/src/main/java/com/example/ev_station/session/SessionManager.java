package com.example.ev_station.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SessionManager {

    private static final String PREF_NAME =
            "secure_session";

    private static final String KEY_SESSION =
            "encrypted_session";

    private static final String KEY_IV =
            "session_iv";

    private static final String KEYSTORE_PROVIDER =
            "AndroidKeyStore";

    private static final String KEY_ALIAS =
            "ev_station_session_key";

    private static final String TRANSFORMATION =
            "AES/GCM/NoPadding";

    private static final int GCM_TAG_LENGTH = 128;

    private static final Object LOCK = new Object();

    private final SharedPreferences preferences;

    public SessionManager(Context context) {

        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }

    /*
     * Retrieve the existing encryption key or generate
     * a new AES-256 key inside Android Keystore.
     */
    private SecretKey getOrCreateSecretKey()
            throws Exception {

        KeyStore keyStore =
                KeyStore.getInstance(KEYSTORE_PROVIDER);

        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {

            return (SecretKey) keyStore.getKey(
                    KEY_ALIAS,
                    null
            );
        }

        KeyGenerator keyGenerator =
                KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        KEYSTORE_PROVIDER
                );

        KeyGenParameterSpec keySpec =
                new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT
                                | KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(
                                KeyProperties.BLOCK_MODE_GCM
                        )
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_NONE
                        )
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true)
                        .build();

        keyGenerator.init(keySpec);

        return keyGenerator.generateKey();
    }

    /*
     * Retrieve the existing key without creating a new one.
     * Missing keys invalidate previously encrypted sessions.
     */
    private SecretKey getExistingSecretKey()
            throws Exception {

        KeyStore keyStore =
                KeyStore.getInstance(KEYSTORE_PROVIDER);

        keyStore.load(null);

        SecretKey secretKey =
                (SecretKey) keyStore.getKey(
                        KEY_ALIAS,
                        null
                );

        if (secretKey == null) {

            throw new IllegalStateException(
                    "Session encryption key unavailable"
            );
        }

        return secretKey;
    }

    /*
     * Extract the JWT expiration timestamp.
     * The exp claim represents Unix time in seconds.
     *
     * This does not verify the JWT signature.
     */
    private long extractJwtExpiry(String token) {

        if (token == null || token.trim().isEmpty()) {
            return -1;
        }

        try {

            String[] parts = token.split("\\.", -1);

            if (parts.length != 3
                    || parts[0].isEmpty()
                    || parts[1].isEmpty()
                    || parts[2].isEmpty()) {

                return -1;
            }

            // Check that the JWT header is valid JSON.
            byte[] headerBytes = Base64.decode(
                    parts[0],
                    Base64.URL_SAFE
                            | Base64.NO_WRAP
                            | Base64.NO_PADDING
            );

            JSONObject header = new JSONObject(
                    new String(
                            headerBytes,
                            StandardCharsets.UTF_8
                    )
            );

            String algorithm =
                    header.optString("alg", "");

            if (algorithm.isEmpty()
                    || "none".equalsIgnoreCase(algorithm)) {

                return -1;
            }

            // Check that the signature segment is decodable.
            byte[] signatureBytes = Base64.decode(
                    parts[2],
                    Base64.URL_SAFE
                            | Base64.NO_WRAP
                            | Base64.NO_PADDING
            );

            if (signatureBytes.length == 0) {
                return -1;
            }

            byte[] payloadBytes = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE
                            | Base64.NO_WRAP
                            | Base64.NO_PADDING
            );

            JSONObject payload = new JSONObject(
                    new String(
                            payloadBytes,
                            StandardCharsets.UTF_8
                    )
            );

            Object expiryValue = payload.opt("exp");

            if (!(expiryValue instanceof Number)) {
                return -1;
            }

            double expiry =
                    ((Number) expiryValue).doubleValue();

            if (Double.isNaN(expiry)
                    || Double.isInfinite(expiry)
                    || expiry <= 0
                    || expiry >= Long.MAX_VALUE) {

                return -1;
            }

            return (long) expiry;

        } catch (Exception exception) {

            return -1;
        }
    }

    /*
     * Check whether a supported application role was returned.
     * Backend authorization remains mandatory.
     */
    private boolean isSupportedRole(String role) {

        return "USER".equals(role)
                || "OWNER".equals(role)
                || "ADMIN".equals(role);
    }

    /*
     * Check local JWT expiration.
     */
    private boolean isExpired(long expirySeconds) {

        long currentTimeSeconds =
                System.currentTimeMillis() / 1000L;

        return expirySeconds <= currentTimeSeconds;
    }

    /*
     * Encrypt and persist the authenticated user session.
     *
     * Passwords and backend JWT secrets must never
     * be stored in this class.
     */
    public boolean saveSession(
            String token,
            long userId,
            String name,
            String email,
            String role
    ) {

        synchronized (LOCK) {

            long expiry = extractJwtExpiry(token);

            if (expiry <= 0
                    || isExpired(expiry)
                    || userId <= 0
                    || name == null
                    || name.trim().isEmpty()
                    || email == null
                    || email.trim().isEmpty()
                    || !isSupportedRole(role)) {

                clearSession();

                return false;
            }

            try {

                JSONObject session = new JSONObject();

                session.put("token", token);
                session.put("userId", userId);
                session.put("name", name);
                session.put("email", email);
                session.put("role", role);
                session.put("expiry", expiry);

                Cipher cipher =
                        Cipher.getInstance(TRANSFORMATION);

                cipher.init(
                        Cipher.ENCRYPT_MODE,
                        getOrCreateSecretKey()
                );

                byte[] encryptedBytes = cipher.doFinal(
                        session.toString().getBytes(
                                StandardCharsets.UTF_8
                        )
                );

                byte[] iv = cipher.getIV();

                boolean saved = preferences.edit()
                        .putString(
                                KEY_SESSION,
                                Base64.encodeToString(
                                        encryptedBytes,
                                        Base64.NO_WRAP
                                )
                        )
                        .putString(
                                KEY_IV,
                                Base64.encodeToString(
                                        iv,
                                        Base64.NO_WRAP
                                )
                        )
                        .commit();

                if (!saved) {

                    clearSession();

                    return false;
                }

                return true;

            } catch (Exception exception) {

                clearSession();

                return false;
            }
        }
    }

    /*
     * Decrypt the stored session and validate its contents.
     * Invalid, expired or unreadable sessions are cleared.
     */
    private JSONObject readSession() {

        synchronized (LOCK) {

            try {

                String encryptedSession =
                        preferences.getString(
                                KEY_SESSION,
                                null
                        );

                String encodedIv =
                        preferences.getString(
                                KEY_IV,
                                null
                        );

                if (encryptedSession == null
                        || encodedIv == null) {

                    if (encryptedSession != null
                            || encodedIv != null) {

                        clearSession();
                    }

                    return null;
                }

                byte[] encryptedBytes = Base64.decode(
                        encryptedSession,
                        Base64.NO_WRAP
                );

                byte[] iv = Base64.decode(
                        encodedIv,
                        Base64.NO_WRAP
                );

                if (iv.length != 12) {

                    clearSession();

                    return null;
                }

                Cipher cipher =
                        Cipher.getInstance(TRANSFORMATION);

                GCMParameterSpec parameterSpec =
                        new GCMParameterSpec(
                                GCM_TAG_LENGTH,
                                iv
                        );

                cipher.init(
                        Cipher.DECRYPT_MODE,
                        getExistingSecretKey(),
                        parameterSpec
                );

                byte[] decryptedBytes =
                        cipher.doFinal(encryptedBytes);

                JSONObject session = new JSONObject(
                        new String(
                                decryptedBytes,
                                StandardCharsets.UTF_8
                        )
                );

                String token =
                        session.getString("token");

                long storedExpiry =
                        session.getLong("expiry");

                long actualExpiry =
                        extractJwtExpiry(token);

                long userId =
                        session.getLong("userId");

                String name =
                        session.getString("name");

                String email =
                        session.getString("email");

                String role =
                        session.getString("role");

                if (actualExpiry <= 0
                        || storedExpiry != actualExpiry
                        || isExpired(actualExpiry)
                        || userId <= 0
                        || name.trim().isEmpty()
                        || email.trim().isEmpty()
                        || !isSupportedRole(role)) {

                    clearSession();

                    return null;
                }

                return session;

            } catch (Exception exception) {

                // Fail closed without logging sensitive data.
                clearSession();

                return null;
            }
        }
    }

    /*
     * Check whether a locally valid session exists.
     */
    public boolean isLoggedIn() {

        return readSession() != null;
    }

    public String getToken() {

        JSONObject session = readSession();

        return session == null
                ? null
                : session.optString("token", null);
    }

    public long getUserId() {

        JSONObject session = readSession();

        return session == null
                ? -1L
                : session.optLong("userId", -1L);
    }

    public String getName() {

        JSONObject session = readSession();

        return session == null
                ? null
                : session.optString("name", null);
    }

    public String getEmail() {

        JSONObject session = readSession();

        return session == null
                ? null
                : session.optString("email", null);
    }

    public String getRole() {

        JSONObject session = readSession();

        return session == null
                ? null
                : session.optString("role", null);
    }

    public long getExpiry() {

        JSONObject session = readSession();

        return session == null
                ? -1L
                : session.optLong("expiry", -1L);
    }

    /*
     * Clear the session only if the rejected token still
     * belongs to the current session.
     *
     * Prevent an older HTTP 401 response from clearing
     * a newer authenticated session.
     */
    public boolean clearSessionIfTokenMatches(
            String rejectedToken
    ) {

        synchronized (LOCK) {

            if (rejectedToken == null) {
                return false;
            }

            JSONObject session = readSession();

            if (session == null) {
                return false;
            }

            String currentToken =
                    session.optString("token", null);

            if (!rejectedToken.equals(currentToken)) {
                return false;
            }

            return clearSession();
        }
    }

    /*
     * Remove the encrypted session and its encryption key.
     * A new key will be generated during the next login.
     */
    public boolean clearSession() {

        synchronized (LOCK) {

            boolean preferencesCleared =
                    preferences.edit().clear().commit();

            boolean keyDeleted = false;

            try {

                KeyStore keyStore =
                        KeyStore.getInstance(
                                KEYSTORE_PROVIDER
                        );

                keyStore.load(null);

                if (keyStore.containsAlias(KEY_ALIAS)) {

                    keyStore.deleteEntry(KEY_ALIAS);
                }

                keyDeleted = true;

            } catch (Exception exception) {

                // Do not log sensitive session information.
            }

            return preferencesCleared && keyDeleted;
        }
    }
}