package cn.lineai.ai.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import cn.lineai.ai.ModelRequestOptions;
import cn.lineai.ai.message.UserModelMessage;
import cn.lineai.data.grok.GrokAuthManager;
import cn.lineai.model.ModelConfig;
import cn.lineai.model.ModelProtocolType;
import java.util.Collections;
import org.json.JSONObject;
import org.junit.Test;

public final class GrokRequestBuilderTest {
    @Test
    public void toolLessRequestOmitsToolControls() throws Exception {
        ModelConfig model = ModelConfig.builder(
                "",
                "Grok",
                ModelProtocolType.GROK_RESPONSES,
                "Grok",
                GrokAuthManager.API_BASE_URL,
                "",
                "grok-4.6"
        ).build();

        JSONObject body = new GrokRequestBuilder().buildRequestBody(
                model,
                Collections.singletonList(new UserModelMessage("test")),
                ModelRequestOptions.defaults()
        );

        assertEquals("grok-4.6", body.getString("model"));
        assertTrue(body.getBoolean("stream"));
        assertFalse(body.getBoolean("store"));
        assertTrue(body.getJSONArray("input").length() > 0);
        assertFalse(body.has("tools"));
        assertFalse(body.has("tool_choice"));
        assertFalse(body.has("parallel_tool_calls"));
    }
}
