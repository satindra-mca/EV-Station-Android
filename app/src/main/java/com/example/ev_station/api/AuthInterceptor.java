package com.example.ev_station.api;

import com.example.ev_station.session.SessionManager;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Attaches JWT authentication to protected backend requests.
 *
 * Public authentication endpoints do not require a JWT.
 */
public final class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;
    private final HttpUrl backendUrl;

    public AuthInterceptor(SessionManager sessionManager) {

        this.sessionManager = sessionManager;
        this.backendUrl = HttpUrl.get(ApiConfig.BASE_URL);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request originalRequest = chain.request();

        HttpUrl requestUrl = originalRequest.url();

        /*
         * Never attach authentication credentials
         * to an unrelated server.
         */
        if (!isBackendRequest(requestUrl)) {

            Request safeRequest = originalRequest.newBuilder()
                    .removeHeader("Authorization")
                    .build();

            return chain.proceed(safeRequest);
        }

        /*
         * Login and registration are public endpoints.
         * Remove any previously attached authorization header.
         */
        if (isPublicAuthEndpoint(requestUrl)) {

            Request publicRequest = originalRequest.newBuilder()
                    .removeHeader("Authorization")
                    .build();

            return chain.proceed(publicRequest);
        }

        /*
         * SessionManager returns null for missing,
         * malformed, expired or unreadable sessions.
         */
        String token = sessionManager.getToken();

        Request.Builder requestBuilder =
                originalRequest.newBuilder()
                        .removeHeader("Authorization");

        if (token != null && !token.trim().isEmpty()) {

            requestBuilder.header(
                    "Authorization",
                    "Bearer " + token
            );
        }

        Request authenticatedRequest =
                requestBuilder.build();

        Response response =
                chain.proceed(authenticatedRequest);

        /*
         * Clear only the session associated with the
         * rejected authentication token.
         */
        if (response.code() == 401 && token != null) {

            sessionManager.clearSessionIfTokenMatches(token);
        }

        return response;
    }

    /**
     * Verify that the request targets the configured backend.
     */
    private boolean isBackendRequest(HttpUrl requestUrl) {

        return backendUrl.scheme().equals(requestUrl.scheme())
                && backendUrl.host().equals(requestUrl.host())
                && backendUrl.port() == requestUrl.port();
    }

    /**
     * Identify public authentication endpoints.
     */
    private boolean isPublicAuthEndpoint(HttpUrl requestUrl) {

        String path = requestUrl.encodedPath();

        return "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path);
    }
}
