package cn.lineai.ai.protocol;

import android.content.Context;
import cn.lineai.ai.ModelCancellationToken;
import cn.lineai.ai.ModelCompletionException;
import cn.lineai.ai.ModelCompletionResponse;
import cn.lineai.ai.ModelRequestOptions;
import cn.lineai.ai.ModelStreamCallback;
import cn.lineai.ai.message.ModelMessage;
import cn.lineai.data.grok.GrokAuthManager;
import cn.lineai.model.ModelConfig;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/** Authenticated Grok Build transport over the current xAI Responses endpoint. */
public final class GrokResponsesProtocol extends AbstractHttpModelProtocol {
    private final Context context;
    private final GrokRequestBuilder requestBuilder = new GrokRequestBuilder();
    private final CodexOutputMerger outputMerger = new CodexOutputMerger();

    public GrokResponsesProtocol() {
        this(null);
    }

    public GrokResponsesProtocol(Context context) {
        this.context = context;
    }

    @Override
    public boolean supportsNativeTools(ModelConfig model) {
        return true;
    }

    @Override
    public boolean supportsDedicatedCompression() {
        return true;
    }

    @Override
    public boolean supportsImageUnderstanding() {
        return true;
    }

    @Override
    public ModelCompletionResponse complete(ModelConfig config, List<ModelMessage> messages) throws ModelCompletionException {
        return stream(config, messages, null, new ModelCancellationToken(), ModelRequestOptions.defaults());
    }

    @Override
    public ModelCompletionResponse stream(
            ModelConfig config,
            List<ModelMessage> messages,
            ModelStreamCallback callback,
            ModelCancellationToken cancellationToken,
            ModelRequestOptions options
    ) throws ModelCompletionException {
        try {
            ModelRequestOptions requestOptions = options == null ? ModelRequestOptions.defaults() : options;
            JSONObject body = requestBuilder.buildRequestBody(config, messages, requestOptions);
            GrokRequestAuth auth = resolveAuth(config);
            HashMap<String, String> headers = requestBuilder.headers(auth.accessToken, auth.userId, auth.email);

            StringBuilder text = new StringBuilder();
            StringBuilder reasoning = new StringBuilder();
            LinkedHashMap<String, CodexOutputMerger.ToolCallBuilder> toolCallBuilders = new LinkedHashMap<>();
            HashMap<String, StringBuilder> customToolInputs = new HashMap<>();
            final int[] usageInputTokens = new int[1];
            final int[] usageOutputTokens = new int[1];

            SseEventHandler handler = (eventType, data) -> handleSseEvent(
                    eventType, data, callback, text, reasoning,
                    toolCallBuilders, customToolInputs, usageInputTokens, usageOutputTokens);

            try {
                postJsonSse(requestBuilder.responsesEndpoint(), body, headers, cancellationToken, handler);
            } catch (ModelCompletionException first) {
                if (!auth.oauth || context == null || !isUnauthorized(first)) {
                    throw first;
                }
                GrokAuthManager authManager = new GrokAuthManager(context);
                String refreshed = authManager.refreshAccessTokenNow();
                if (refreshed == null || refreshed.length() == 0) {
                    throw first;
                }
                HashMap<String, String> retryHeaders = requestBuilder.headers(
                        refreshed, authManager.getUserId(), authManager.getEmail());
                postJsonSse(requestBuilder.responsesEndpoint(), body, retryHeaders, cancellationToken, handler);
            }

            return new ModelCompletionResponse(
                    text.toString(), reasoning.toString(),
                    outputMerger.buildToolCalls(toolCallBuilders),
                    usageInputTokens[0], usageOutputTokens[0]
            );
        } catch (ModelCompletionException e) {
            throw e;
        } catch (Exception e) {
            throw new ModelCompletionException("Grok Responses protocol failed: " + e.getMessage(), e);
        }
    }

    private GrokRequestAuth resolveAuth(ModelConfig config) throws ModelCompletionException {
        String configuredToken = config == null ? "" : config.getApiKey();
        if (configuredToken != null && configuredToken.trim().length() > 0) {
            return new GrokRequestAuth(configuredToken.trim(), "", "", false);
        }
        if (context != null) {
            GrokAuthManager authManager = new GrokAuthManager(context);
            String token = authManager.getValidAccessToken();
            if (token != null && token.length() > 0) {
                return new GrokRequestAuth(
                        token,
                        authManager.getUserId(),
                        authManager.getEmail(),
                        true
                );
            }
        }
        throw new ModelCompletionException(
                "Grok is not authenticated. Sign in with Grok or provide an xAI token.");
    }

    private boolean isUnauthorized(ModelCompletionException error) {
        String message = error == null ? null : error.getMessage();
        return message != null && (message.startsWith("HTTP 401:") || message.startsWith("HTTP 403:"));
    }

    private void handleSseEvent(
            String eventType,
            String data,
            ModelStreamCallback callback,
            StringBuilder text,
            StringBuilder reasoning,
            LinkedHashMap<String, CodexOutputMerger.ToolCallBuilder> toolCallBuilders,
            HashMap<String, StringBuilder> customToolInputs,
            int[] usageInputTokens,
            int[] usageOutputTokens
    ) throws Exception {
        if (data == null || data.trim().length() == 0 || "[DONE]".equals(data.trim())) {
            return;
        }
        JSONObject event = new JSONObject(data);
        if (event.has("error")) {
            throw new ModelCompletionException("Grok stream error: " + event.opt("error"));
        }
        String type = event.optString("type");
        if (type.length() == 0) {
            type = eventType == null ? "" : eventType;
        }

        if ("response.custom_tool_call_input.delta".equals(type) && event.has("delta")) {
            appendCustomToolInput(customToolInputs, event.optString("item_id"), event.optString("delta"));
            appendCustomToolInput(customToolInputs, event.optString("call_id"), event.optString("delta"));
            return;
        }
        if ("response.function_call_arguments.delta".equals(type) && event.has("delta")) {
            appendFunctionArgumentsDelta(toolCallBuilders, event, event.optString("delta"));
            return;
        }
        if ("response.output_text.delta".equals(type) && event.has("delta")) {
            String delta = event.optString("delta");
            text.append(delta);
            if (callback != null) {
                callback.onTextDelta(delta);
            }
            return;
        }
        if ("response.output_text.done".equals(type)) {
            outputMerger.appendFinalIfMissing(text,
                    event.optString("text", event.optString("delta")), false, callback);
            return;
        }
        if (("response.reasoning_summary_text.delta".equals(type)
                || "response.reasoning_text.delta".equals(type)) && event.has("delta")) {
            String delta = event.optString("delta");
            reasoning.append(delta);
            if (callback != null) {
                callback.onReasoningDelta(delta);
            }
            return;
        }
        if ("response.completed".equals(type)) {
            JSONObject response = event.optJSONObject("response");
            if (response != null) {
                outputMerger.mergeOutputArray(response.optJSONArray("output"), text, reasoning,
                        toolCallBuilders, customToolInputs, callback);
                JSONObject usage = response.optJSONObject("usage");
                if (usage != null) {
                    usageInputTokens[0] = Math.max(usageInputTokens[0], usage.optInt("input_tokens", 0));
                    usageOutputTokens[0] = Math.max(usageOutputTokens[0], usage.optInt("output_tokens", 0));
                }
            }
            throw new SseStreamCompleteException();
        }
        if (("response.output_item.added".equals(type) || "response.output_item.done".equals(type))
                && event.has("item")) {
            outputMerger.mergeOutputItem(event.optJSONObject("item"), text, reasoning,
                    toolCallBuilders, customToolInputs, callback);
            return;
        }
        if ("response.failed".equals(type)) {
            throw new ModelCompletionException("Grok response.failed: " + event.toString());
        }
        if ("response.incomplete".equals(type)) {
            JSONObject response = event.optJSONObject("response");
            JSONObject details = response == null ? null : response.optJSONObject("incomplete_details");
            String reason = details == null ? "unknown" : details.optString("reason", "unknown");
            throw new ModelCompletionException("Grok response.incomplete: " + reason);
        }
    }

    private void appendCustomToolInput(Map<String, StringBuilder> customToolInputs, String id, String delta) {
        if (id == null || id.length() == 0 || delta == null || delta.length() == 0) {
            return;
        }
        StringBuilder builder = customToolInputs.get(id);
        if (builder == null) {
            builder = new StringBuilder();
            customToolInputs.put(id, builder);
        }
        builder.append(delta);
    }

    private void appendFunctionArgumentsDelta(
            LinkedHashMap<String, CodexOutputMerger.ToolCallBuilder> toolCallBuilders,
            JSONObject event,
            String delta
    ) {
        if (delta == null || delta.length() == 0) {
            return;
        }
        String callId = event.optString("call_id");
        if (callId.length() == 0) {
            callId = event.optString("item_id", event.optString("output_index"));
        }
        if (callId.length() == 0) {
            callId = "call_" + toolCallBuilders.size();
        }
        CodexOutputMerger.ToolCallBuilder builder = toolCallBuilders.get(callId);
        if (builder == null) {
            builder = new CodexOutputMerger.ToolCallBuilder(callId);
            toolCallBuilders.put(callId, builder);
        }
        builder.arguments.append(delta);
    }

    private static final class GrokRequestAuth {
        final String accessToken;
        final String userId;
        final String email;
        final boolean oauth;

        GrokRequestAuth(String accessToken, String userId, String email, boolean oauth) {
            this.accessToken = accessToken == null ? "" : accessToken;
            this.userId = userId == null ? "" : userId;
            this.email = email == null ? "" : email;
            this.oauth = oauth;
        }
    }
}
