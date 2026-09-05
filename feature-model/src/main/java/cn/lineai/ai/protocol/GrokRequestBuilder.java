package cn.lineai.ai.protocol;

import cn.lineai.ai.ModelRequestOptions;
import cn.lineai.ai.message.ModelMessage;
import cn.lineai.model.ModelConfig;
import cn.lineai.model.ModelContextParser;
import cn.lineai.tool.ToolInfo;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Grok Build-compatible Responses API request builder. */
final class GrokRequestBuilder {

    JSONObject buildRequestBody(
            ModelConfig config,
            List<ModelMessage> messages,
            ModelRequestOptions requestOptions
    ) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", ModelContextParser.apiModelId(config));
        body.put("input", ResponsesInputBuilder.inputJson(messages));
        body.put("stream", true);
        body.put("store", false);
        body.put("parallel_tool_calls", true);
        body.put("tools", toolsJson(requestOptions == null ? null : requestOptions.getTools()));
        body.put("tool_choice", "auto");
        String instructions = ResponsesInputBuilder.instructions(messages);
        if (instructions.length() > 0) {
            body.put("instructions", instructions);
        }
        return body;
    }

    HashMap<String, String> headers(String accessToken, String userId, String email) {
        HashMap<String, String> headers = new HashMap<>();
        cn.lineai.data.grok.GrokAuthManager.applyClientHeaders(
                headers, accessToken, userId, email);
        headers.put("Accept", "text/event-stream");
        headers.put("Content-Type", "application/json");
        return headers;
    }

    String responsesEndpoint() {
        return cn.lineai.data.grok.GrokAuthManager.API_BASE_URL + "/responses";
    }

    private JSONArray toolsJson(List<ToolInfo> tools) throws Exception {
        JSONArray array = new JSONArray();
        if (tools == null || tools.isEmpty()) {
            return array;
        }
        JSONArray openAiTools = ToolInfo.toJsonArray(tools);
        for (int i = 0; i < openAiTools.length(); i++) {
            JSONObject tool = openAiTools.optJSONObject(i);
            if (tool == null) {
                continue;
            }
            JSONObject function = tool.optJSONObject("function");
            if ("function".equals(tool.optString("type")) && function != null) {
                array.put(new JSONObject()
                        .put("type", "function")
                        .put("name", function.optString("name"))
                        .put("description", function.optString("description"))
                        .put("strict", false)
                        .put("parameters", function.optJSONObject("parameters") == null
                                ? new JSONObject().put("type", "object").put("properties", new JSONObject())
                                : function.optJSONObject("parameters")));
            } else {
                array.put(tool);
            }
        }
        return array;
    }
}
