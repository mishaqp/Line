package cn.lineai.tool;

import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.model.McpSettingsState;
import cn.lineai.model.McpToolConfig;
import cn.lineai.model.WebSearchConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 可编程的 {@link ToolSettingsStore} 测试替身：只关心权限模式与执行目标，
 * 其余契约方法返回空值。用于在 JVM 上验证 root 目标与完全访问模式的行为。
 */
final class FakeToolSettingsStore implements ToolSettingsStore {

    private String permissionMode = PERMISSION_AUTO;
    private String executionMode = EXECUTION_LOCAL;
    private final Set<String> enabledTools = new HashSet<>();

    FakeToolSettingsStore permissionMode(String mode) {
        this.permissionMode = mode;
        return this;
    }

    FakeToolSettingsStore executionMode(String mode) {
        this.executionMode = mode;
        return this;
    }

    FakeToolSettingsStore enable(String... toolNames) {
        Collections.addAll(enabledTools, toolNames);
        return this;
    }

    @Override
    public String getPermissionMode() {
        return permissionMode;
    }

    @Override
    public void setPermissionMode(String mode) {
        this.permissionMode = mode;
    }

    @Override
    public String getExecutionMode() {
        return executionMode;
    }

    @Override
    public void setExecutionMode(String mode) {
        this.executionMode = mode;
    }

    @Override
    public List<McpToolConfig> getConfigs() {
        return Collections.emptyList();
    }

    @Override
    public McpSettingsState getMcpSettingsState() {
        return new McpSettingsState(executionMode, Collections.emptyList());
    }

    @Override
    public WebSearchConfig getWebSearchConfig() {
        return new WebSearchConfig();
    }

    @Override
    public void setWebSearchConfig(WebSearchConfig config) {
    }

    @Override
    public String getImageUnderstandingModelId() {
        return "";
    }

    @Override
    public void setImageUnderstandingModelId(String modelId) {
    }

    @Override
    public String getImageGenerationModelId() {
        return "";
    }

    @Override
    public void setImageGenerationModelId(String modelId) {
    }

    @Override
    public void setMcpEnabled(String id, boolean enabled) {
    }

    @Override
    public Set<String> getEnabledToolNames() {
        return new HashSet<>(enabledTools);
    }

    @Override
    public Set<String> getEnabledToolNames(Collection<ToolInfo> implementedTools) {
        return getEnabledToolNames();
    }

    @Override
    public PermissionResult canExecuteTool(String toolName, ToolCategory category) {
        return PermissionResult.allowed();
    }

    @Override
    public boolean needsConfirmation(String toolName) {
        return false;
    }

    @Override
    public String buildToolPrompt(Set<String> implementedToolNames) {
        return "";
    }

    @Override
    public String buildToolPrompt(Set<String> implementedToolNames, boolean nativeToolProtocol) {
        return "";
    }

    @Override
    public String buildToolPrompt(Collection<ToolInfo> implementedTools, boolean nativeToolProtocol) {
        return "";
    }
}
