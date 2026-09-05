package cn.lineai.data.grok;

import android.content.Context;
import cn.lineai.security.SimpleHttpClient;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Fetches the live Grok Build model catalog for the signed-in account. */
public final class GrokModelsRepository {
    private GrokModelsRepository() {
    }

    public static List<String> fetchModelIds(Context context) throws Exception {
        GrokAuthManager auth = new GrokAuthManager(context);
        String token = auth.getValidAccessToken();
        if (token == null || token.length() == 0) {
            throw new GrokApiException(401, "Grok is not authenticated");
        }
        try {
            return fetch(auth, token, "/models-v2");
        } catch (GrokApiException first) {
            if (first.isUnauthorized()) {
                String refreshed = auth.refreshAccessTokenNow();
                if (refreshed != null && refreshed.length() > 0) {
                    return fetch(auth, refreshed, "/models-v2");
                }
            }
            // Older proxy deployments used /models; keep a compatibility fallback
            // only for endpoint-not-found responses, not for auth failures.
            if (first.getStatusCode() == 404) {
                return fetch(auth, token, "/models");
            }
            throw first;
        }
    }

    private static List<String> fetch(GrokAuthManager auth, String token, String path) throws Exception {
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
            throw new GrokApiException(response.code, "Grok model catalog request failed");
        }
        JSONObject root = new JSONObject(response.body);
        JSONArray data = root.optJSONArray("data");
        if (data == null) {
            data = root.optJSONArray("models");
        }
        Set<String> unique = new LinkedHashSet<>();
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                Object raw = data.opt(i);
                if (raw instanceof String) {
                    add(unique, (String) raw);
                    continue;
                }
                JSONObject item = data.optJSONObject(i);
                if (item == null || item.optBoolean("hidden", false)) {
                    continue;
                }
                if (item.has("supportedInApi") && !item.optBoolean("supportedInApi", true)) {
                    continue;
                }
                if (item.has("supported_in_api") && !item.optBoolean("supported_in_api", true)) {
                    continue;
                }
                String id = firstNonBlank(
                        item.optString("model", ""),
                        firstNonBlank(item.optString("modelId", ""), item.optString("id", ""))
                );
                JSONObject meta = item.optJSONObject("_meta");
                if (id.length() == 0 && meta != null) {
                    id = firstNonBlank(meta.optString("model", ""),
                            firstNonBlank(meta.optString("modelId", ""), meta.optString("id", "")));
                }
                add(unique, id);
            }
        }
        return new ArrayList<>(unique);
    }

    private static void add(Set<String> target, String value) {
        if (value != null && value.trim().length() > 0) {
            target.add(value.trim());
        }
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && first.trim().length() > 0 ? first.trim() : (fallback == null ? "" : fallback.trim());
    }
}
