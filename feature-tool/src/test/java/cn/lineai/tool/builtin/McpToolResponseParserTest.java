package cn.lineai.tool.builtin;

import cn.lineai.model.tool.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class McpToolResponseParserTest {
    private static JSONObject result(String text, boolean error) throws Exception {
        return new JSONObject().put("content", new JSONArray().put(
                new JSONObject().put("type", "text").put("text", text))).put("isError", error);
    }

    private static String response(JSONObject result) throws Exception {
        return new JSONObject().put("jsonrpc", "2.0").put("id", "call-1")
                .put("result", result).toString();
    }

    private static ToolResult parse(String body) {
        return McpToolResponseParser.parse("mcp_test", body, "call-1");
    }

    @Test public void successfulTextIsReadable() throws Exception {
        ToolResult value = parse(response(result("готово", false)));
        assertFalse(value.isError());
        assertEquals("готово", value.getContent());
        assertEquals("mcp_test", value.getToolName());
    }

    @Test public void toolFailureRemainsFailure() throws Exception {
        ToolResult value = parse(response(result("command failed", true)));
        assertTrue(value.isError());
        assertEquals("command failed", value.getContent());
    }

    @Test public void protocolErrorPreservesDetails() throws Exception {
        String body = new JSONObject().put("jsonrpc", "2.0").put("id", "call-1")
                .put("error", new JSONObject().put("code", -32602)
                        .put("message", "Invalid arguments").put("data", "missing path")).toString();
        ToolResult value = parse(body);
        assertTrue(value.isError());
        assertTrue(value.getContent().contains("Invalid arguments"));
        assertTrue(value.getContent().contains("missing path"));
    }

    @Test public void progressAndUnrelatedResponsesAreNotConcatenated() throws Exception {
        String progress = new JSONObject().put("jsonrpc", "2.0")
                .put("method", "notifications/progress").put("params", new JSONObject()).toString();
        String unrelated = new JSONObject(response(result("wrong", true))).put("id", "other").toString();
        ToolResult value = parse(": heartbeat\r\nevent: message\r\ndata: " + progress
                + "\r\n\r\ndata: " + unrelated + "\r\n\r\nid: event-3\r\ndata: "
                + response(result("correct", false)) + "\r\n\r\ndata: [DONE]\r\n\r\n");
        assertFalse(value.isError());
        assertEquals("correct", value.getContent());
    }

    @Test public void multilineEventDataKeepsNewlines() {
        String body = "data: {\"jsonrpc\":\"2.0\",\n"
                + "data: \"id\":\"call-1\",\n"
                + "data: \"result\":{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}}\n\n";
        assertFalse(parse(body).isError());
        assertEquals("ok", parse(body).getContent());
    }

    @Test public void finalEventWithoutBlankLineIsAccepted() throws Exception {
        assertFalse(parse("data:" + response(result("ok", false))).isError());
    }

    @Test public void streamingToolErrorRemainsFailure() throws Exception {
        assertTrue(parse("data: " + response(result("failed", true)) + "\n\n").isError());
    }

    @Test public void missingOrMalformedResponsesNeverSucceed() throws Exception {
        String[] bodies = {null, "", "   ", "<html>Bad gateway</html>", "{",
                "{}", "[]", "{\"content\":[]}", "data: [DONE]\n\n",
                response(result("ok", false)) + " garbage",
                new JSONObject(response(result("wrong", false))).put("id", "other").toString(),
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/progress\"}"};
        for (String body : bodies) assertTrue(String.valueOf(body), parse(body).isError());
    }

    @Test public void invalidResultShapesNeverSucceed() throws Exception {
        JSONObject[] invalid = {new JSONObject(), new JSONObject().put("content", "text"),
                new JSONObject().put("content", new JSONArray().put("text")),
                new JSONObject().put("content", new JSONArray().put(new JSONObject().put("type", "text"))),
                result("ok", false).put("isError", "false"),
                result("ok", false).put("structuredContent", "invalid")};
        for (JSONObject value : invalid) assertTrue(value.toString(), parse(response(value)).isError());
    }

    @Test public void duplicateOrAmbiguousResponsesFail() throws Exception {
        String body = response(result("ok", false));
        assertTrue(parse("data: " + body + "\n\ndata: " + body + "\n\n").isError());
        assertTrue(parse(new JSONObject(body).put("error", new JSONObject()).toString()).isError());
    }

    @Test public void contentAndStructuredDataArePreserved() throws Exception {
        JSONObject value = result("first", false);
        value.getJSONArray("content").put(new JSONObject().put("type", "text").put("text", "second"));
        value.getJSONArray("content").put(new JSONObject().put("type", "resource_link")
                .put("uri", "file:///report").put("name", "report"));
        value.put("structuredContent", new JSONObject().put("count", 2));
        ToolResult parsed = parse(response(value));
        assertFalse(parsed.isError());
        assertTrue(parsed.getContent().startsWith("first\nsecond\n"));
        assertTrue(parsed.getContent().contains("file:///report"));
        assertTrue(parsed.getContent().contains("\"count\":2"));
    }

    @Test public void emptyContentIsValidButErrorFlagStillApplies() throws Exception {
        JSONObject value = new JSONObject().put("content", new JSONArray());
        assertFalse(parse(response(value)).isError());
        assertTrue(parse(response(value.put("isError", true))).isError());
    }
}
