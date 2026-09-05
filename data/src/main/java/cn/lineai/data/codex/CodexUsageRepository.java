package cn.lineai.data.codex;

import android.content.Context;
import cn.lineai.security.SimpleHttpClient;
import org.json.JSONObject;

/**
 * Read-only view of the primary and secondary Codex rate-limit windows.
 */
public final class CodexUsageRepository {
    public static final String USAGE_URL = "https://chatgpt.com/backend-api/wham/usage";
    private static final String ORIGINATOR = "codex_cli_rs";

    private CodexUsageRepository() {
    }

    public static CodexAccountStatus fetch(Context context) throws Exception {
        CodexAuthManager auth = new CodexAuthManager(context);
        String token = auth.getValidAccessToken();
        if (token == null || token.length() == 0) {
            throw new CodexApiException(401, "Codex is not signed in");
        }
        String accountId = auth.getAccountId();
        if (accountId == null || accountId.length() == 0) {
            throw new CodexApiException(401, "Codex account identity is unavailable");
        }

        SimpleHttpClient.Response response = execute(token, accountId);
        if (response.code == 401) {
            String refreshed = auth.refreshAccessTokenNow();
            if (refreshed != null && refreshed.length() > 0) {
                accountId = auth.getAccountId();
                response = execute(refreshed, accountId);
            }
        }
        if (response.code == 401) {
            auth.logout();
            throw new CodexApiException(401, "Codex usage session is unauthorized");
        }
        if (response.code < 200 || response.code >= 300) {
            throw new CodexApiException(response.code, "Codex usage request failed");
        }
        return new CodexAccountStatus(
                accountId,
                auth.getPlanType(),
                auth.getEmail(),
                parse(response.body)
        );
    }

    private static SimpleHttpClient.Response execute(String token, String accountId) throws Exception {
        SimpleHttpClient.Request request = new SimpleHttpClient.Request(USAGE_URL, "GET", null);
        request.connectTimeoutMs = 20000;
        request.readTimeoutMs = 30000;
        request.headers.put("Authorization", "Bearer " + token);
        if (accountId != null && accountId.length() > 0) {
            request.headers.put("ChatGPT-Account-Id", accountId);
        }
        request.headers.put("originator", ORIGINATOR);
        request.headers.put("User-Agent", ORIGINATOR + "/" + CodexAuthManager.CODEX_CLIENT_VERSION + " (Android; LineCode)");
        request.headers.put("Accept", "application/json");
        return SimpleHttpClient.execute(request);
    }

    public static CodexUsageSnapshot parse(String raw) throws Exception {
        return parse(new JSONObject(raw));
    }

    public static CodexUsageSnapshot parse(JSONObject body) {
        JSONObject rateLimit = body == null ? null : body.optJSONObject("rate_limit");
        return new CodexUsageSnapshot(
                window(rateLimit == null ? null : rateLimit.optJSONObject("primary_window")),
                window(rateLimit == null ? null : rateLimit.optJSONObject("secondary_window"))
        );
    }

    private static CodexUsageWindow window(JSONObject object) {
        if (object == null || !object.has("used_percent")) {
            return null;
        }
        double used = object.optDouble("used_percent", Double.NaN);
        if (Double.isNaN(used)) {
            return null;
        }
        long seconds = object.optLong("limit_window_seconds", 0L);
        long resetAt = object.optLong("reset_at", 0L);
        if (resetAt > 10000000000L) {
            resetAt /= 1000L;
        }
        return new CodexUsageWindow(
                clampPercent(used),
                seconds > 0L ? seconds / 60L : 0L,
                resetAt > 0L ? resetAt : 0L
        );
    }

    private static double clampPercent(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    public static final class CodexAccountStatus {
        private final String accountId;
        private final String planType;
        private final String email;
        private final CodexUsageSnapshot usage;

        public CodexAccountStatus(String accountId, String planType, String email, CodexUsageSnapshot usage) {
            this.accountId = accountId == null ? "" : accountId;
            this.planType = planType == null ? "" : planType;
            this.email = email == null ? "" : email;
            this.usage = usage == null ? new CodexUsageSnapshot(null, null) : usage;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getPlanType() {
            return planType;
        }

        public String getEmail() {
            return email;
        }

        public CodexUsageSnapshot getUsage() {
            return usage;
        }
    }

    public static final class CodexUsageSnapshot {
        private final CodexUsageWindow primary;
        private final CodexUsageWindow secondary;

        public CodexUsageSnapshot(CodexUsageWindow primary, CodexUsageWindow secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        public CodexUsageWindow getPrimary() {
            return primary;
        }

        public CodexUsageWindow getSecondary() {
            return secondary;
        }
    }

    public static final class CodexUsageWindow {
        private final double usedPercent;
        private final long windowMinutes;
        private final long resetsAtEpochSeconds;

        public CodexUsageWindow(double usedPercent, long windowMinutes, long resetsAtEpochSeconds) {
            this.usedPercent = clampPercent(usedPercent);
            this.windowMinutes = Math.max(0L, windowMinutes);
            this.resetsAtEpochSeconds = Math.max(0L, resetsAtEpochSeconds);
        }

        public double getUsedPercent() {
            return usedPercent;
        }

        public double getRemainingPercent() {
            return Math.max(0.0, Math.min(100.0, 100.0 - usedPercent));
        }

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public long getResetsAtEpochSeconds() {
            return resetsAtEpochSeconds;
        }
    }
}
