package cn.lineai.data.codex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.json.JSONObject;
import cn.lineai.security.SimpleHttpClient;

/**
 * Native OpenAI/ChatGPT OAuth PKCE manager for the Codex backend.
 *
 * Tokens are kept in CodexAuthStore and are never returned to the UI. The
 * public UI callback exposes only success and a safe human-readable message.
 */
public final class CodexAuthManager {
    public static final String AUTHORIZATION_URL = "https://auth.openai.com/oauth/authorize";
    public static final String TOKEN_URL = "https://auth.openai.com/oauth/token";
    public static final String CHATGPT_BACKEND_URL = "https://chatgpt.com/backend-api/codex";
    public static final String REDIRECT_URI = "http://localhost:1455/auth/callback";
    public static final String CALLBACK_PATH = "/auth/callback";
    public static final String CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann";
    public static final String SCOPES = "openid profile email offline_access api.connectors.read api.connectors.invoke";
    public static final String CODEX_CLIENT_VERSION = "0.153.4";

    private static final int CALLBACK_PORT = 1455;
    private static final long REFRESH_WINDOW_MILLIS = 5L * 60L * 1000L;
    private static final Object TOKEN_REFRESH_LOCK = new Object();

    public interface LoginCallback {
        void onComplete(boolean success, String message);
    }

    private final Context context;
    private final CodexAuthStore store;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object loginLock = new Object();
    private CodexCallbackServer callbackServer;
    private String codeVerifier;
    private String expectedState;
    private boolean loginInProgress;

    public CodexAuthManager(Context context) {
        this.context = context.getApplicationContext() == null
                ? context
                : context.getApplicationContext();
        this.store = new CodexAuthStore(this.context);
    }

    public static boolean isAuthenticated(Context context) {
        return new CodexAuthManager(context).isAuthenticated();
    }

    public boolean isAuthenticated() {
        return store.hasAccessToken();
    }

    public String getAccountId() {
        return store.getAccountId();
    }

    public String getPlanType() {
        return store.getPlanType();
    }

    public String getEmail() {
        return store.getEmail();
    }

    public void logout() {
        synchronized (loginLock) {
            if (callbackServer != null) {
                callbackServer.stop();
                callbackServer = null;
            }
            loginInProgress = false;
            codeVerifier = null;
            expectedState = null;
        }
        store.clear();
    }

    /**
     * Start the browser-based PKCE login. The callback is delivered on the
     * main thread and contains no token or raw HTTP response.
     */
    public void startLogin(final LoginCallback callback) {
        synchronized (loginLock) {
            if (loginInProgress) {
                deliver(callback, false, "Codex login is already in progress.");
                return;
            }
            loginInProgress = true;
            codeVerifier = randomUrlToken(64);
            expectedState = randomUrlToken(32);
            callbackServer = new CodexCallbackServer(CALLBACK_PORT,
                    (code, state, error) -> onCallback(callback, code, state, error));
            if (!callbackServer.start()) {
                finish(callback, false, "Codex callback port 1455 is unavailable.");
                return;
            }
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, buildAuthorizationUri());
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            finish(callback, false, "Unable to open the OpenAI sign-in page.");
        }
    }

    public String getValidAccessToken() {
        return getAccessToken(false);
    }

    /** Force one OAuth refresh, used after an authenticated backend request returns 401. */
    public String refreshAccessTokenNow() {
        return getAccessToken(true);
    }

    private String getAccessToken(boolean forceRefresh) {
        synchronized (TOKEN_REFRESH_LOCK) {
            String accessToken = store.getAccessToken();
            if (accessToken.length() == 0) {
                return null;
            }
            long expiresAt = store.getExpiresAtMillis();
            long now = System.currentTimeMillis();
            if (!forceRefresh && (expiresAt <= 0L || expiresAt - now > REFRESH_WINDOW_MILLIS)) {
                return accessToken;
            }
            String refreshToken = store.getRefreshToken();
            if (refreshToken.length() == 0) {
                if (forceRefresh) {
                    return null;
                }
                return expiresAt <= 0L || now < expiresAt ? accessToken : null;
            }
            try {
                String refreshed = refreshAccessToken(refreshToken);
                if (refreshed != null && refreshed.length() > 0) {
                    return refreshed;
                }
            } catch (Exception ignored) {
                // Keep a still-live token usable if the network is temporarily
                // unavailable. An expired token is cleared below.
            }
            if (expiresAt > 0L && now >= expiresAt) {
                store.clear();
                return null;
            }
            return forceRefresh ? null : accessToken;
        }
    }

    private String refreshAccessToken(String refreshToken) throws Exception {
        JSONObject body = new JSONObject();
        body.put("grant_type", "refresh_token");
        body.put("refresh_token", refreshToken);
        body.put("client_id", CLIENT_ID);
        SimpleHttpClient.Request request = new SimpleHttpClient.Request(
                TOKEN_URL,
                "POST",
                body.toString()
        );
        request.connectTimeoutMs = 20000;
        request.readTimeoutMs = 30000;
        request.headers.put("Content-Type", "application/json");
        request.headers.put("Accept", "application/json");
        SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
        if (response.code == 400 || response.code == 401) {
            store.clear();
            return null;
        }
        if (response.code < 200 || response.code >= 300) {
            throw new CodexApiException(response.code, "Codex token refresh failed");
        }
        JSONObject json = new JSONObject(response.body);
        store.saveTokenResponse(json);
        updateIdentityFromIdToken(json.optString("id_token", ""));
        return store.getAccessToken();
    }

    private void onCallback(LoginCallback callback, String code, String state, String error) {
        synchronized (loginLock) {
            if (!loginInProgress) {
                return;
            }
        }
        if (error != null && error.length() > 0) {
            finish(callback, false, "OpenAI authorization was cancelled or denied.");
            return;
        }
        if (code == null || code.length() == 0) {
            finish(callback, false, "OpenAI authorization did not return a code.");
            return;
        }
        synchronized (loginLock) {
            if (expectedState == null || !expectedState.equals(state)) {
                finish(callback, false, "OpenAI authorization state validation failed.");
                return;
            }
            if (callbackServer != null) {
                callbackServer.stop();
                callbackServer = null;
            }
        }

        final String verifier;
        synchronized (loginLock) {
            verifier = codeVerifier;
        }
        new Thread(() -> {
            try {
                exchangeAuthorizationCode(code, verifier);
                finish(callback, true, "");
            } catch (Exception e) {
                finish(callback, false, safeExchangeError(e));
            }
        }, "linecode-codex-token-exchange").start();
    }

    private void exchangeAuthorizationCode(String code, String verifier) throws Exception {
        SimpleHttpClient.Request request = new SimpleHttpClient.Request(
                TOKEN_URL,
                "POST",
                buildAuthorizationCodeBody(code, verifier)
        );
        request.connectTimeoutMs = 20000;
        request.readTimeoutMs = 30000;
        request.headers.put("Content-Type", "application/x-www-form-urlencoded");
        request.headers.put("Accept", "application/json");
        SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
        if (response.code < 200 || response.code >= 300) {
            throw new CodexApiException(response.code, "Codex token exchange failed");
        }
        JSONObject json = new JSONObject(response.body);
        store.saveTokenResponse(json);
        updateIdentityFromIdToken(json.optString("id_token", ""));
    }

    static String buildAuthorizationCodeBody(String code, String verifier) {
        return "grant_type=authorization_code"
                + "&code=" + formEncode(code)
                + "&redirect_uri=" + formEncode(REDIRECT_URI)
                + "&client_id=" + formEncode(CLIENT_ID)
                + "&code_verifier=" + formEncode(verifier == null ? "" : verifier);
    }

    private static String formEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode OAuth form", e);
        }
    }

    private void updateIdentityFromIdToken(String idToken) throws Exception {
        if (idToken == null || idToken.length() == 0) {
            return;
        }
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            return;
        }
        String payload = new String(
                Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
        );
        JSONObject claims = new JSONObject(payload);
        JSONObject authClaims = claims.optJSONObject("https://api.openai.com/auth");
        String accountId = firstNonBlank(
                authClaims == null ? "" : authClaims.optString("chatgpt_account_id", ""),
                claims.optString("chatgpt_account_id", "")
        );
        String planType = firstNonBlank(
                authClaims == null ? "" : authClaims.optString("chatgpt_plan_type", ""),
                claims.optString("chatgpt_plan_type", "")
        );
        String email = firstNonBlank(
                claims.optString("email", ""),
                authClaims == null ? "" : authClaims.optString("email", "")
        );
        if (accountId.length() == 0 && planType.length() == 0 && email.length() == 0) {
            return;
        }
        store.saveIdentity(accountId, planType, email);
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && first.length() > 0 ? first : (fallback == null ? "" : fallback);
    }

    private Uri buildAuthorizationUri() {
        return Uri.parse(AUTHORIZATION_URL)
                .buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", SCOPES)
                .appendQueryParameter("state", expectedState)
                .appendQueryParameter("code_challenge", sha256Base64Url(codeVerifier))
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("codex_cli_simplified_flow", "true")
                .appendQueryParameter("originator", "codex_cli_rs")
                .appendQueryParameter("id_token_add_organizations", "true")
                .build();
    }

    private void finish(final LoginCallback callback, final boolean success, final String message) {
        CodexCallbackServer server;
        synchronized (loginLock) {
            server = callbackServer;
            callbackServer = null;
            loginInProgress = false;
            codeVerifier = null;
            expectedState = null;
        }
        if (server != null) {
            server.stop();
        }
        deliver(callback, success, message);
    }

    private void deliver(final LoginCallback callback, final boolean success, final String message) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onComplete(success, message == null ? "" : message));
    }

    private String safeExchangeError(Exception e) {
        if (e instanceof CodexApiException) {
            CodexApiException api = (CodexApiException) e;
            if (api.isUnauthorized()) {
                return "OpenAI rejected the authorization exchange.";
            }
            return "OpenAI authorization failed (HTTP " + api.getStatusCode() + ").";
        }
        return "Unable to complete Codex authorization. Check the network and try again.";
    }

    private String randomUrlToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Base64Url(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create PKCE challenge", e);
        }
    }
}
