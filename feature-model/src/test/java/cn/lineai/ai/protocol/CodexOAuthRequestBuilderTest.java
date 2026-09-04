package cn.lineai.ai.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import org.junit.Test;

public final class CodexOAuthRequestBuilderTest {
    @Test
    public void oauthUsesChatGptResponsesEndpointAndAccountHeaders() {
        CodexRequestBuilder builder = new CodexRequestBuilder();
        HashMap<String, String> headers = builder.codexHeaders("oauth-token", "account-123", true);

        assertEquals(
                "https://chatgpt.com/backend-api/codex/responses",
                builder.oauthResponsesEndpoint()
        );
        assertEquals("Bearer oauth-token", headers.get("Authorization"));
        assertEquals("account-123", headers.get("ChatGPT-Account-Id"));
        assertEquals(CodexRequestBuilder.CODEX_ORIGINATOR, headers.get("originator"));
        assertEquals(CodexRequestBuilder.CODEX_OAUTH_CLIENT_VERSION, headers.get("version"));
        assertEquals("responses=experimental", headers.get("OpenAI-Beta"));
        assertTrue(headers.get("x-codex-window-id") != null
                && headers.get("x-codex-window-id").length() > 0);
    }
}
