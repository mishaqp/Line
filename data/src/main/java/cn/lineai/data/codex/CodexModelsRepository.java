package cn.lineai.data.codex;

import android.content.Context;
import cn.lineai.security.SimpleHttpClient;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Authenticated Codex model catalog.
 */
public final class CodexModelsRepository {
    public static final String MODELS_URL = "https://chatgpt.com/backend-api/codex/models";
    public static final String CLIENT_VERSION = CodexAuthManager.CODEX_CLIENT_VERSION;

    private CodexModelsRepository() {
    }

    public static List<String> fetchModelIds(Context context) throws Exception {
        CodexAuthManager auth = new CodexAuthManager(context);
        String token = auth.getValidAccessToken();
        if (token == null || token.length() == 0) {
            throw new CodexApiException(401, "Codex is not signed in");
        }
        String accountId = auth.getAccountId();
        if (accountId == null || accountId.length() == 0) {
            throw new CodexApiException(401, "Codex account identity is unavailable");
        }
        SimpleHttpClient.Request request = new SimpleHttpClient.Request(
                MODELS_URL + "?client_version=" + CLIENT_VERSION,
                "GET",
                null
        );
        request.connectTimeoutMs = 20000;
        request.readTimeoutMs = 30000;
        request.headers.put("Authorization", "Bearer " + token);
        request.headers.put("ChatGPT-Account-Id", accountId);
        request.headers.put("OpenAI-Beta", "responses=experimental");
        request.headers.put("originator", "codex_cli_rs");
        request.headers.put("User-Agent", "codex_cli_rs/" + CLIENT_VERSION + " (Android; LineCode)");
        request.headers.put("Accept", "application/json");
        SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
        if (response.code == 401) {
            auth.logout();
            throw new CodexApiException(401, "Codex model catalog is unauthorized");
        }
        if (response.code < 200 || response.code >= 300) {
            throw new CodexApiException(response.code, "Codex model catalog request failed");
        }
        return parseModelIds(response.body);
    }

    public static List<String> parseModelIds(String raw) throws Exception {
        return parseModelIds(new JSONObject(raw));
    }

    public static List<String> parseModelIds(JSONObject body) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (body == null) {
            return new ArrayList<>();
        }
        JSONArray models = body.optJSONArray("models");
        if (models == null) {
            models = body.optJSONArray("data");
        }
        if (models == null) {
            return new ArrayList<>();
        }
        for (int i = 0; i < models.length(); i++) {
            JSONObject item = models.optJSONObject(i);
            if (item == null) {
                continue;
            }
            if (item.has("visibility") && !"list".equals(item.optString("visibility"))) {
                continue;
            }
            String id = item.optString("slug", "");
            if (id.length() == 0) {
                id = item.optString("id", "");
            }
            if (id.length() > 0) {
                result.add(id);
            }
        }
        return new ArrayList<>(result);
    }
}
