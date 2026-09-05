package cn.lineai.model;

public enum ModelProtocolType {
    OPENAI_COMPATIBLE("OpenAI", true),
    CODEX_RESPONSES("Codex", true),
    GROK_RESPONSES("Grok", true),
    ANTHROPIC_MESSAGES("Anthropic", false),
    LOCAL_GGUF("Local", false);

    private final String label;
    private final boolean dedicatedCompression;

    ModelProtocolType(String label, boolean dedicatedCompression) {
        this.label = label;
        this.dedicatedCompression = dedicatedCompression;
    }

    public String getLabel() {
        return label;
    }

    /** 该协议类型是否支持独立压缩模型。 */
    public boolean supportsDedicatedCompression() {
        return dedicatedCompression;
    }

    public static ModelProtocolType fromStorage(String value) {
        if (value == null) {
            return OPENAI_COMPATIBLE;
        }
        String normalized = value.trim();
        if ("openai".equalsIgnoreCase(normalized) || "openai_compatible".equalsIgnoreCase(normalized)) {
            return OPENAI_COMPATIBLE;
        }
        if ("codex".equalsIgnoreCase(normalized) || "codex_responses".equalsIgnoreCase(normalized)) {
            return CODEX_RESPONSES;
        }
        if ("grok".equalsIgnoreCase(normalized) || "grok_responses".equalsIgnoreCase(normalized)
                || "xai".equalsIgnoreCase(normalized)) {
            return GROK_RESPONSES;
        }
        if ("anthropic".equalsIgnoreCase(normalized) || "claude".equalsIgnoreCase(normalized)
                || "anthropic_messages".equalsIgnoreCase(normalized)) {
            return ANTHROPIC_MESSAGES;
        }
        if ("local".equalsIgnoreCase(normalized) || "gguf".equalsIgnoreCase(normalized)
                || "local_gguf".equalsIgnoreCase(normalized)) {
            return LOCAL_GGUF;
        }
        for (ModelProtocolType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return OPENAI_COMPATIBLE;
    }
}
