package cn.lineai.data.grok;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import cn.lineai.security.SimpleHttpClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.json.JSONObject;

/**
 * xAI/Grok OAuth manager using the official RFC 8628 device authorization flow.
 * The public client id, scopes and client fingerprint mirror Grok Build 1.0.13.
 */
public final class GrokAuthManager {
    public static final String OAUTH_ISSUER = "https://auth.x.ai";
    public static final String DEVICE_CODE_URL = OAUTH_ISSUER + "/oauth2/device/code";
    public static final String TOKEN_URL = OAUTH_ISSUER + "/oauth2/token";
    public static final String API_BASE_URL = "https://cli-chat-proxy.grok.com/v1";
    public static final String CLIENT_ID = "b1a00492-073a-47ea-816f-4c329264a828";
    public static final String CLIENT_VERSION = "1.0.13";
    public static final String TOKEN_AUTH_VALUE = "xai-grok-cli";
    public static final String SCOPES = "openid profile email offline_access grok-cli:access api:access conversations:read conversations:write workspaces:read workspaces:write";

    private static final String DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
    private static final long REFRESH_WINDOW_MILLIS = 5L * 60L * 1000L;
    private static final Object TOKEN_REFRESH_LOCK = new Object();

    public interface LoginCallback {
        void onUserCode(String userCode, String verificationUri);
        void onComplete(boolean success, String message);
    }

    private final Context context;
    private final GrokAuthStore store;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object loginLock = new Object();
    private boolean loginInProgress;

    public GrokAuthManager(Context context) {
        this.context = context.getApplicationContext() == null
                ? context
                : context.getApplicationContext();
        this.store = new GrokAuthStore(this.context);
    }

    public static boolean isAuthenticated(Context context) {
        return new GrokAuthManager(context).isAuthenticated();
    }

    public boolean isAuthenticated() {
        return store.hasAccessToken();
    }

    public String getUserId() {
        return store.getUserId();
    }

    public String getEmail() {
        return store.getEmail();
    }

    public String getPlanType() {
        return store.getPlanType();
    }

    public void saveIdentity(String userId, String planType, String email) throws Exception {
        store.saveIdentity(userId, planType, email);
    }

    public void logout() {
        synchronized (loginLock) {
            loginInProgress = false;
        }
        store.clear();
    }

    public void startLogin(final LoginCallback callback) {
        synchronized (loginLock) {
            if (loginInProgress) {
                deliverComplete(callback, false, "Grok login is already in progress.");
                return;
            }
            loginInProgress = true;
        }
        new Thread(() -> {
            try {
                DeviceCode deviceCode = requestDeviceCode();
                String openUrl = deviceCode.verificationUriComplete.length() > 0
                        ? deviceCode.verificationUriComplete
                        : deviceCode.verificationUri;
                deliverCode(callback, deviceCode.userCode, deviceCode.verificationUri);
                openBrowser(openUrl);
                JSONObject tokens = pollForToken(deviceCode);
                store.saveTokenResponse(tokens);
                updateIdentityFromIdToken(tokens.optString("id_token", ""));
                try {
                    refreshIdentityFromProxy(store.getAccessToken());
                } catch (Exception ignored) {
                    // Live account page refresh will retry identity/plan loading.
                }
                finish(callback, true, "");
            } catch (Exception e) {
                finish(callback, false, safeLoginError(e));
            }
        }, "linecode-grok-device-auth").start();
    }

    public String getValidAccessToken() {
        return getAccessToken(false);
    }

    public String refreshAccessTokenNow() {
        return getAccessToken(true);
    }

    private String getAccessToken(boolean forceRefresh) {
        synchronized (TOKEN_REFRESH_LOCK) {
            String token = store.getAccessToken();
            if (token.length() == 0) {
                return null;
            }
            long expiresAt = store.getExpiresAtMillis();
            long now = System.currentTimeMillis();
            if (!forceRefresh && (expiresAt <= 0L || expiresAt - now > REFRESH_WINDOW_MILLIS)) {
                return token;
            }
            String refreshToken = store.getRefreshToken();
            if (refreshToken.length() == 0) {
                if (expiresAt > 0L && now >= expiresAt) {
                    store.clear();
                    return null;
                }
                return forceRefresh ? null : token;
            }
            try {
                String refreshed = refreshAccessToken(refreshToken);
                if (refreshed != null && refreshed.length() > 0) {
                    return refreshed;
                }
            } catch (Exception ignored) {
            }
            if (expiresAt > 0L && now >= expiresAt) {
                store.clear();
                return null;
            }
            return forceRefresh ? null : token;
        }
    }

    private DeviceCode requestDeviceCode() throws Exception {
        String body = "client_id=" + formEncode(CLIENT_ID)
                + "&scope=" + formEncode(SCOPES)
                + "&referrer=" + formEncode("grok-build");
        SimpleHttpClient.Request request = new SimpleHttpClient.Request(DEVICE_CODE_URL, "POST", body);
        request.connectTimeoutMs = 20000;
        request.readTimeoutMs = 30000;
        request.headers.put("Content-Type", "application/x-www-form-urlencoded");
        request.headers.put("Accept", "application/json");
        request.headers.put("x-grok-client-version", CLIENT_VERSION);
        request.headers.put("x-grok-client-surface", "ui");
        SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
        if (response.code < 200 || response.code >= 300) {
            throw new GrokApiException(response.code, "Grok device authorization failed");
        }
        JSONObject json = new JSONObject(response.body);
        String deviceCode = json.optString("device_code", "");
        String userCode = json.optString("user_code", "");
        String verificationUri = json.optString("verification_uri", "");
        String verificationUriComplete = json.optString("verification_uri_complete", "");
        long expiresIn = Math.max(60L, json.optLong("expires_in", 600L));
        long interval = Math.max(1L, json.optLong("interval", 5L));
        if (deviceCode.length() == 0 || userCode.length() == 0 || verificationUri.length() == 0) {
            throw new IllegalStateException("Grok device authorization returned an incomplete response");
        }
        return new DeviceCode(deviceCode, userCode, verificationUri, verificationUriComplete, expiresIn, interval);
    }

    private JSONObject pollForToken(DeviceCode device) throws Exception {
        long deadline = System.currentTimeMillis() + Math.max(device.expiresIn, 600L) * 1000L;
        long intervalSeconds = device.interval;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Grok authorization was interrupted", e);
            }
            String body = "grant_type=" + formEncode(DEVICE_GRANT)
                    + "&device_code=" + formEncode(device.deviceCode)
                    + "&client_id=" + formEncode(CLIENT_ID);
            SimpleHttpClient.Request request = new SimpleHttpClient.Request(TOKEN_URL, "POST", body);
            request.connectTimeoutMs = 20000;
            request.readTimeoutMs = 30000;
            request.headers.put("Content-Type", "application/x-www-form-urlencoded");
            request.headers.put("Accept", "application/json");
            request.headers.put("x-grok-client-version", CLIENT_VERSION);
            request.headers.put("x-grok-client-surface", "ui");
            SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
            JSONObject json = response.body == null || response.body.length() == 0
                    ? new JSONObject()
                    : new JSONObject(response.body);
            if (response.code >= 200 && response.code < 300) {
                if (json.optString("access_token", "").length() == 0) {
                    throw new IllegalStateException("Grok token response has no access token");
                }
                return json;
            }
            String error = json.optString("error", "");
            if ("authorization_pending".equals(error)) {
                continue;
            }
            if ("slow_down".equals(error)) {
                intervalSeconds += 5L;
                continue;
            }
            if ("access_denied".equals(error)) {
                throw new IllegalStateException("Grok authorization was denied");
            }
            if ("expired_token".equals(error)) {
                throw new IllegalStateException("Grok authorization code expired");
            }
            throw new GrokApiException(response.code, "Grok token exchange failed");
        }
        throw new IllegalStateException("Grok authorization code expired");
    }

    private String refreshAccessToken(String refreshToken) throws Exception {
        String body = "grant_type=refresh_token"
                + "&refresh_token=" + formEncode(refreshToken)
                + "&client_id=" + formEncode(CLIENT_ID);
        SimpleHttpClient.Request request = new SimpleHttpClient.Request(TOKEN_URL, "POST", body);
        request.connectTimeoutMs = 20000;
        request.readTimeoutMs = 30000;
        request.headers.put("Content-Type", "application/x-www-form-urlencoded");
        request.headers.put("Accept", "application/json");
        request.headers.put("x-grok-client-version", CLIENT_VERSION);
        request.headers.put("x-grok-client-surface", "ui");
        SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
        if (response.code == 400 || response.code == 401) {
            store.clear();
            return null;
        }
        if (response.code < 200 || response.code >= 300) {
            throw new GrokApiException(response.code, "Grok token refresh failed");
        }
        JSONObject json = new JSONObject(response.body);
        store.saveTokenResponse(json);
        updateIdentityFromIdToken(json.optString("id_token", ""));
        return store.getAccessToken();
    }

    public void refreshIdentityFromProxy(String accessToken) throws Exception {
        if (accessToken == null || accessToken.length() == 0) {
            return;
        }
        SimpleHttpClient.Request request = new SimpleHttpClient.Request(API_BASE_URL + "/user", "GET", null);
        request.connectTimeoutMs = 15000;
        request.readTimeoutMs = 20000;
        applyClientHeaders(request.headers, accessToken, store.getUserId(), store.getEmail());
        SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
        if (response.code < 200 || response.code >= 300) {
            return;
        }
        JSONObject root = new JSONObject(response.body);
        JSONObject user = root.optJSONObject("user");
        if (user == null) {
            user = root;
        }
        String userId = firstNonBlank(
                user.optString("user_id", ""),
                firstNonBlank(user.optString("userId", ""),
                        firstNonBlank(user.optString("id", ""), user.optString("sub", "")))
        );
        String email = firstNonBlank(user.optString("email", ""), store.getEmail());
        store.saveIdentity(userId, store.getPlanType(), email);
    }

    public static void applyClientHeaders(Map<String, String> headers, String accessToken, String userId, String email) {
        if (headers == null) {
            return;
        }
        if (accessToken != null && accessToken.length() > 0) {
            headers.put("Authorization", "Bearer " + accessToken);
        }
        headers.put("X-XAI-Token-Auth", TOKEN_AUTH_VALUE);
        headers.put("x-grok-client-version", CLIENT_VERSION);
        headers.put("x-grok-client-mode", "interactive");
        headers.put("x-grok-client-identifier", "grok-shell");
        headers.put("User-Agent", "grok/" + CLIENT_VERSION + " (Android; LineCode)");
        if (userId != null && userId.length() > 0) {
            headers.put("x-userid", userId);
        }
        if (email != null && email.length() > 0) {
            headers.put("x-email", email);
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
        byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        JSONObject claims = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
        String userId = firstNonBlank(claims.optString("sub", ""), claims.optString("user_id", ""));
        String email = claims.optString("email", "");
        if (userId.length() > 0 || email.length() > 0) {
            store.saveIdentity(userId, store.getPlanType(), email);
        }
    }

    private void openBrowser(String url) {
        mainHandler.post(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (!(context instanceof Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        });
    }

    private void deliverCode(LoginCallback callback, String code, String uri) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onUserCode(code == null ? "" : code, uri == null ? "" : uri));
    }

    private void finish(LoginCallback callback, boolean success, String message) {
        synchronized (loginLock) {
            loginInProgress = false;
        }
        deliverComplete(callback, success, message);
    }

    private void deliverComplete(LoginCallback callback, boolean success, String message) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onComplete(success, message == null ? "" : message));
    }

    private String safeLoginError(Exception error) {
        if (error instanceof GrokApiException) {
            return "Grok authorization failed (HTTP " + ((GrokApiException) error).getStatusCode() + ").";
        }
        String message = error == null ? "" : error.getMessage();
        if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("denied")) {
            return "Grok authorization was denied.";
        }
        if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("expired")) {
            return "Grok authorization code expired. Try again.";
        }
        return "Unable to complete Grok authorization. Check the network and try again.";
    }

    private static String formEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode Grok OAuth form", e);
        }
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && first.trim().length() > 0 ? first.trim() : (fallback == null ? "" : fallback.trim());
    }

    private static final class DeviceCode {
        final String deviceCode;
        final String userCode;
        final String verificationUri;
        final String verificationUriComplete;
        final long expiresIn;
        final long interval;

        DeviceCode(String deviceCode, String userCode, String verificationUri,
                   String verificationUriComplete, long expiresIn, long interval) {
            this.deviceCode = deviceCode;
            this.userCode = userCode;
            this.verificationUri = verificationUri;
            this.verificationUriComplete = verificationUriComplete == null ? "" : verificationUriComplete;
            this.expiresIn = expiresIn;
            this.interval = interval;
        }
    }
}
