package cn.lineai.data.grok;

import android.content.Context;
import cn.lineai.security.SimpleHttpClient;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONObject;

/** Reads Grok Build subscription and current credit usage from the CLI proxy. */
public final class GrokUsageRepository {
    private GrokUsageRepository() {
    }

    public static GrokAccountStatus fetch(Context context) throws Exception {
        GrokAuthManager auth = new GrokAuthManager(context);
        String token = auth.getValidAccessToken();
        if (token == null || token.length() == 0) {
            throw new GrokApiException(401, "Grok is not authenticated");
        }
        try {
            return fetchWithToken(auth, token);
        } catch (GrokApiException first) {
            if (!first.isUnauthorized()) {
                throw first;
            }
            String refreshed = auth.refreshAccessTokenNow();
            if (refreshed == null || refreshed.length() == 0) {
                throw first;
            }
            return fetchWithToken(auth, refreshed);
        }
    }

    private static GrokAccountStatus fetchWithToken(GrokAuthManager auth, String token) throws Exception {
        if (auth.getUserId().length() == 0) {
            auth.refreshIdentityFromProxy(token);
        }
        JSONObject billing = getJson(auth, token, "/billing?format=credits");
        JSONObject config = billing.optJSONObject("config");
        if (config == null) {
            config = new JSONObject();
        }

        double usedPercent = clampPercent(config.optDouble("creditUsagePercent", -1.0));
        JSONObject period = config.optJSONObject("currentPeriod");
        String periodType = period == null ? "" : period.optString("type", "");
        long periodStart = parseTimestamp(period == null ? "" : period.optString("start", ""));
        long periodEnd = parseTimestamp(period == null ? "" : period.optString("end", ""));
        if (periodStart <= 0L) {
            periodStart = parseTimestamp(config.optString("billingPeriodStart", ""));
        }
        if (periodEnd <= 0L) {
            periodEnd = parseTimestamp(config.optString("billingPeriodEnd", ""));
        }

        String plan = firstNonBlank(
                billing.optString("subscriptionTier", ""),
                billing.optString("subscription_tier", "")
        );
        try {
            JSONObject settings = getJson(auth, token, "/settings");
            JSONObject data = settings.optJSONObject("settings");
            if (data == null) {
                data = settings.optJSONObject("data");
            }
            if (data == null) {
                data = settings;
            }
            plan = firstNonBlank(
                    data.optString("subscription_tier_display", ""),
                    firstNonBlank(data.optString("subscriptionTierDisplay", ""),
                            firstNonBlank(data.optString("subscription_tier", ""),
                                    data.optString("subscriptionTier", plan)))
            );
        } catch (Exception ignored) {
            // Usage is more important than a cosmetic tier label.
        }

        if (plan.length() > 0) {
            auth.saveIdentity(auth.getUserId(), plan, auth.getEmail());
        }
        return new GrokAccountStatus(
                plan,
                new GrokUsageWindow(usedPercent, periodStart, periodEnd, periodType)
        );
    }

    private static JSONObject getJson(GrokAuthManager auth, String token, String path) throws Exception {
        SimpleHttpClient.Request request = new SimpleHttpClient.Request(
                GrokAuthManager.API_BASE_URL + path,
                "GET",
                null
        );
        request.connectTimeoutMs = 15000;
        request.readTimeoutMs = 25000;
        request.headers.put("Accept", "application/json");
        GrokAuthManager.applyClientHeaders(request.headers, token, auth.getUserId(), auth.getEmail());
        SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
        if (response.code < 200 || response.code >= 300) {
            throw new GrokApiException(response.code, "Grok account request failed");
        }
        return new JSONObject(response.body);
    }

    private static double clampPercent(double value) {
        if (Double.isNaN(value) || value < 0.0) {
            return -1.0;
        }
        return Math.max(0.0, Math.min(100.0, value));
    }

    private static long parseTimestamp(String value) {
        if (value == null || value.trim().length() == 0) {
            return 0L;
        }
        String text = value.trim();
        try {
            long numeric = Long.parseLong(text);
            if (numeric > 0L) {
                return numeric > 100000000000L ? numeric / 1000L : numeric;
            }
        } catch (NumberFormatException ignored) {
        }
        String[] patterns = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                return format.parse(text).getTime() / 1000L;
            } catch (ParseException ignored) {
            }
        }
        return 0L;
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && first.trim().length() > 0 ? first.trim() : (fallback == null ? "" : fallback.trim());
    }

    public static final class GrokAccountStatus {
        private final String planType;
        private final GrokUsageWindow usage;

        GrokAccountStatus(String planType, GrokUsageWindow usage) {
            this.planType = planType == null ? "" : planType;
            this.usage = usage;
        }

        public String getPlanType() {
            return planType;
        }

        public GrokUsageWindow getUsage() {
            return usage;
        }
    }

    public static final class GrokUsageWindow {
        private final double usedPercent;
        private final long periodStartEpochSeconds;
        private final long resetsAtEpochSeconds;
        private final String periodType;

        GrokUsageWindow(double usedPercent, long periodStartEpochSeconds,
                        long resetsAtEpochSeconds, String periodType) {
            this.usedPercent = usedPercent;
            this.periodStartEpochSeconds = periodStartEpochSeconds;
            this.resetsAtEpochSeconds = resetsAtEpochSeconds;
            this.periodType = periodType == null ? "" : periodType;
        }

        public boolean hasUsagePercent() {
            return usedPercent >= 0.0;
        }

        public double getUsedPercent() {
            return Math.max(0.0, usedPercent);
        }

        public double getRemainingPercent() {
            return hasUsagePercent() ? Math.max(0.0, 100.0 - usedPercent) : 0.0;
        }

        public long getPeriodStartEpochSeconds() {
            return periodStartEpochSeconds;
        }

        public long getResetsAtEpochSeconds() {
            return resetsAtEpochSeconds;
        }

        public String getPeriodType() {
            return periodType;
        }
    }
}
