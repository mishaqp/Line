package cn.lineai.ai.prompt;

import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.tool.ToolCategory;
import cn.lineai.tool.ToolInfo;
import cn.lineai.tool.ToolPromptRenderer;
import cn.lineai.tool.ToolRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 工具提示词渲染服务，从 ToolSettingsRepository 中提取的单一职责类。
 * 负责构建发送给模型的工具列表提示词。
 */
public final class ToolPromptService {

    private final ToolSettingsStore toolSettingsStore;
    private final ToolRegistry toolRegistry;
    private final ToolPromptRenderer toolPromptRenderer;

    public ToolPromptService(ToolSettingsStore toolSettingsStore, ToolRegistry toolRegistry, ToolPromptRenderer toolPromptRenderer) {
        this.toolSettingsStore = toolSettingsStore;
        this.toolRegistry = toolRegistry;
        this.toolPromptRenderer = toolPromptRenderer;
    }

    public String buildToolPrompt(Set<String> implementedToolNames) {
        return buildToolPrompt(implementedToolNames, false);
    }

    public String buildToolPrompt(Set<String> implementedToolNames, boolean nativeToolProtocol) {
        Set<String> enabled = toolSettingsStore.getEnabledToolNames();
        if (implementedToolNames != null) {
            enabled.retainAll(implementedToolNames);
        }
        return toolPromptRenderer.renderToolPrompt(collectEnabledTools(enabled), nativeToolProtocol);
    }

    public String buildToolPrompt(Collection<ToolInfo> implementedTools, boolean nativeToolProtocol) {
        Set<String> enabled = toolSettingsStore.getEnabledToolNames();
        List<ToolInfo> enabledTools = new ArrayList<>();
        if (implementedTools != null) {
            for (ToolInfo tool : implementedTools) {
                if (tool != null && tool.getName().length() > 0) {
                    if (enabled.contains(tool.getName()) || isEnabledExtensionTool(tool.getName(), tool.getCategory())) {
                        enabledTools.add(tool);
                    }
                }
            }
        }
        return toolPromptRenderer.renderToolPrompt(enabledTools, nativeToolProtocol);
    }

    private boolean isEnabledExtensionTool(String toolName, ToolCategory category) {
        if (!ToolRegistry.isExtensionToolName(toolName)) {
            return false;
        }
        String executionMode = toolSettingsStore.getExecutionMode();
        if (ToolSettingsStore.isRemoteExecution(executionMode) && !ToolRegistry.isCustomMcpToolName(toolName)) {
            return false;
        }
        if (!ToolSettingsStore.isKnownExecution(executionMode)) {
            return false;
        }
        return !ToolSettingsStore.PERMISSION_READONLY.equals(toolSettingsStore.getPermissionMode()) || isReadonlyAllowed(category);
    }

    private static boolean isReadonlyAllowed(ToolCategory category) {
        return category == ToolCategory.READ || category == ToolCategory.GENERATE;
    }

    private List<ToolInfo> collectEnabledTools(Set<String> enabledNames) {
        List<ToolInfo> tools = new ArrayList<>();
        if (toolRegistry != null) {
            for (String name : enabledNames) {
                ToolInfo tool = toolRegistry.get(name);
                if (tool != null) {
                    tools.add(tool);
                }
            }
        }
        return tools;
    }
}
