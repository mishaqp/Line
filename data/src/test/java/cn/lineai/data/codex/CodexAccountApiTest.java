package cn.lineai.data.codex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public final class CodexAccountApiTest {
    @Test
    public void usageParserReadsBothWindowsAndNormalizesResetTime() throws Exception {
        long resetMillis = 1_800_000_000_000L;
        JSONObject json = new JSONObject()
                .put("rate_limit", new JSONObject()
                        .put("primary_window", new JSONObject()
                                .put("used_percent", 25.0)
                                .put("limit_window_seconds", 18000)
                                .put("reset_at", resetMillis))
                        .put("secondary_window", new JSONObject()
                                .put("used_percent", 110.0)
                                .put("limit_window_seconds", 86400)
                                .put("reset_at", 1_800_000_100L)));

        CodexUsageRepository.CodexUsageSnapshot snapshot =
                CodexUsageRepository.parse(json);
        CodexUsageRepository.CodexUsageWindow primary = snapshot.getPrimary();
        CodexUsageRepository.CodexUsageWindow secondary = snapshot.getSecondary();

        assertNotNull(primary);
        assertNotNull(secondary);
        assertEquals(25.0, primary.getUsedPercent(), 0.001);
        assertEquals(75.0, primary.getRemainingPercent(), 0.001);
        assertEquals(300L, primary.getWindowMinutes());
        assertEquals(1_800_000_000L, primary.getResetsAtEpochSeconds());
        assertEquals(100.0, secondary.getUsedPercent(), 0.001);
        assertEquals(0.0, secondary.getRemainingPercent(), 0.001);
    }

    @Test
    public void usageParserToleratesMissingRateLimitWindows() {
        CodexUsageRepository.CodexUsageSnapshot snapshot =
                CodexUsageRepository.parse(new JSONObject());
        assertNull(snapshot.getPrimary());
        assertNull(snapshot.getSecondary());
    }

    @Test
    public void modelParserUsesSlugFiltersPrivateModelsAndKeepsOrder() throws Exception {
        JSONArray models = new JSONArray()
                .put(new JSONObject().put("slug", "gpt-5-codex").put("visibility", "list"))
                .put(new JSONObject().put("id", "internal").put("visibility", "hidden"))
                .put(new JSONObject().put("id", "gpt-5.1-codex"))
                .put(new JSONObject().put("slug", "gpt-5-codex"));
        List<String> ids = CodexModelsRepository.parseModelIds(
                new JSONObject().put("models", models));
        assertEquals(Arrays.asList("gpt-5-codex", "gpt-5.1-codex"), ids);
    }
}
