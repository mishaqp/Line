package cn.lineai.tool.builtin;
import cn.lineai.model.tool.ToolResult;

import cn.lineai.model.ExtensionMcpConfig;
import cn.lineai.model.McpRequestHeader;
import cn.lineai.model.McpToolSummary;
import cn.lineai.security.SimpleHttpClient;
import cn.lineai.tool.BaseTool;
import cn.lineai.tool.R;
import cn.lineai.tool.ToolCategory;
import cn.lineai.tool.ToolContext;
import cn.lineai.tool.ToolDisplayCategory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

public final class CustomMcpHttpTool extends BaseTool {
    private static final String MCP_PROTOCOL_VERSION = "2025-03-26";
    private static final String HEADER_SESSION_ID = "Mcp-Session-Id";
    private static final String HEADER_PROTOCOL_VERSION = "Mcp-Protocol-Version";

    private final String name;
    private final ExtensionMcpConfig mcp;
    private final McpToolSummary tool;

    public CustomMcpHttpTool(String name, ExtensionMcpConfig mcp, McpToolSummary tool) {
        this.name = name == null ? "" : name;
        this.mcp = mcp;
        this.tool = tool;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        builder.append("Invoke the tool ").append(tool.getName()).append(" of the custom HTTP MCP \"").append(mcp.getName()).append("\".");
        if (tool.getDescription().length() > 0) {
            builder.append('\n').append(tool.getDescription());
        }
        builder.append("\nMCP address: ").append(mcp.getUrl());
        return builder.toString();
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.SYSTEM;
    }

    @Override
    public ToolDisplayCategory getDisplayCategory() {
        return ToolDisplayCategory.GENERIC;
    }

    @Override
    public JSONObject getParameters() throws org.json.JSONException {
        if (tool.getInputSchemaJson().length() > 0) {
            JSONObject schema = new JSONObject(tool.getInputSchemaJson());
            if ("object".equals(schema.optString("type"))) {
                return schema;
            }
        }
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("additionalProperties", true);
    }

    @Override
    public ToolResult execute(JSONObject input, ToolContext context) {
        try {
            String sessionId = initializeSession();
            JSONObject body = new JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("id", "linecode_" + System.currentTimeMillis())
                    .put("method", "tools/call")
                    .put("params", new JSONObject()
                            .put("name", tool.getName())
                            .put("arguments", input == null ? new JSONObject() : input));
            SimpleHttpClient.Request request = new SimpleHttpClient.Request(mcp.getUrl(), "POST", body.toString());
            request.connectTimeoutMs = 15000;
            request.readTimeoutMs = 60000;
            request.headers.putAll(baseHeaders(sessionId));
            SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
            if (response.code < 200 || response.code >= 300) {
                return error(response.code + ": " + response.body);
            }
            return McpToolResponseParser.parse(getName(), response.body, body.getString("id"));
        } catch (Exception e) {
            return error(context.getString(R.string.tool_mcp_call_failed, e.getMessage()));
        }
    }

    private String initializeSession() throws Exception {
        JSONObject init = new JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", "linecode_init_" + System.currentTimeMillis())
                .put("method", "initialize")
                .put("params", new JSONObject()
                        .put("protocolVersion", MCP_PROTOCOL_VERSION)
                        .put("capabilities", new JSONObject())
                        .put("clientInfo", new JSONObject()
                                .put("name", "linecode")
                                .put("version", "1.0")));
        SimpleHttpClient.Request request = new SimpleHttpClient.Request(mcp.getUrl(), "POST", init.toString());
        request.connectTimeoutMs = 15000;
        request.readTimeoutMs = 30000;
        request.headers.putAll(baseHeaders(""));
        SimpleHttpClient.Response response = SimpleHttpClient.execute(request);
        if (response.code < 200 || response.code >= 300) {
            // 服务端不支持 initialize（stateless 模式）时降级为无会话调用。
            return "";
        }
        String sessionId = response.headers.get(HEADER_SESSION_ID);
        return sessionId == null ? "" : sessionId;
    }

    private Map<String, String> baseHeaders(String sessionId) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json, text/event-stream");
        headers.put("Content-Type", "application/json");
        headers.put(HEADER_PROTOCOL_VERSION, MCP_PROTOCOL_VERSION);
        if (sessionId != null && sessionId.length() > 0) {
            headers.put(HEADER_SESSION_ID, sessionId);
        }
        for (McpRequestHeader header : mcp.getRequestHeaders()) {
            if (header.getName().length() > 0) {
                headers.put(header.getName(), header.getValue());
            }
        }
        return headers;
    }

}
