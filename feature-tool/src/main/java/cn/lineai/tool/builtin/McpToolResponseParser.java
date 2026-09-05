package cn.lineai.tool.builtin;

import cn.lineai.model.tool.ToolResult;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Decodes one tools/call response; SSE events are separate JSON-RPC messages. */
final class McpToolResponseParser {
    private McpToolResponseParser() {}

    static ToolResult parse(String toolName, String body, String requestId) {
        try {
            JSONObject response = null;
            for (String payload : payloads(body)) {
                if ("[DONE]".equals(payload.trim())) continue;
                JSONTokener tokener = new JSONTokener(payload);
                Object value = tokener.nextValue();
                if (!(value instanceof JSONObject) || tokener.nextClean() != 0) {
                    throw new IllegalArgumentException("Invalid JSON message");
                }
                JSONObject message = (JSONObject) value;
                if (!"2.0".equals(message.optString("jsonrpc"))) {
                    throw new IllegalArgumentException("Missing JSON-RPC version");
                }
                if (!message.has("id") && message.opt("method") instanceof String
                        && !message.has("result") && !message.has("error")) {
                    continue; // Progress notifications are not tool results.
                }
                if (!requestId.equals(message.opt("id"))) continue;
                if (response != null) throw new IllegalArgumentException("Duplicate response");
                response = message;
            }
            if (response == null) throw new IllegalArgumentException("No matching tool response");
            boolean hasResult = response.has("result");
            boolean hasError = response.has("error");
            if (hasResult == hasError) throw new IllegalArgumentException("Invalid response envelope");
            if (hasError) {
                JSONObject error = response.getJSONObject("error");
                return ToolResult.of("", toolName, error.toString(), true);
            }
            JSONObject result = response.getJSONObject("result");
            if (result.has("isError") && !(result.get("isError") instanceof Boolean)) {
                throw new IllegalArgumentException("Invalid isError flag");
            }
            JSONArray content = result.getJSONArray("content");
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                JSONObject block = content.getJSONObject(i);
                if (!(block.opt("type") instanceof String)) {
                    throw new IllegalArgumentException("Invalid content block");
                }
                String text;
                if ("text".equals(block.getString("type"))) {
                    if (!(block.opt("text") instanceof String)) {
                        throw new IllegalArgumentException("Invalid text block");
                    }
                    text = block.getString("text");
                } else {
                    text = block.toString(); // Preserve images, resources and unknown block types.
                }
                if (output.length() > 0) output.append('\n');
                output.append(text);
            }
            if (result.has("structuredContent")) {
                if (output.length() > 0) output.append('\n');
                output.append(result.getJSONObject("structuredContent").toString());
            }
            boolean failed = result.optBoolean("isError", false);
            if (output.length() == 0) {
                output.append(failed ? "MCP tool reported an error without details." : "MCP tool completed.");
            }
            return ToolResult.of("", toolName, output.toString(), failed);
        } catch (Exception e) {
            return ToolResult.of("", toolName, "Invalid MCP response: " + e.getMessage(), true);
        }
    }

    private static List<String> payloads(String body) {
        String text = body == null ? "" : body.trim();
        List<String> payloads = new ArrayList<>();
        if (text.startsWith("{") || text.startsWith("[")) {
            payloads.add(text);
            return payloads;
        }
        StringBuilder event = new StringBuilder();
        boolean hasData = false;
        for (String line : (body == null ? "" : body).split("\\r\\n|\\r|\\n", -1)) {
            if (line.isEmpty()) {
                if (hasData) payloads.add(event.toString());
                event.setLength(0);
                hasData = false;
            } else if (line.equals("data") || line.startsWith("data:")) {
                String data = line.equals("data") ? "" : line.substring(5);
                if (data.startsWith(" ")) data = data.substring(1);
                if (hasData) event.append('\n');
                event.append(data);
                hasData = true;
            } else if (!(line.startsWith(":") || line.startsWith("event:")
                    || line.startsWith("id:") || line.startsWith("retry:"))) {
                throw new IllegalArgumentException("Invalid SSE field");
            }
        }
        if (hasData) payloads.add(event.toString());
        return payloads;
    }
}
