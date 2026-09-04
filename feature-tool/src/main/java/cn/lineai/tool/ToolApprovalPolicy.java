package cn.lineai.tool;

import cn.lineai.data.repository.ToolSettingsStore;

/**
 * Shared approval gate for every tool execution path.
 *
 * <p>The model/provider only emits tool calls. Whether a call may run immediately or must wait for
 * user approval is decided here from the global {@link ToolSettingsStore}. Keeping this decision
 * provider-agnostic makes main chat, Agent and Agent Pipeline follow the same permission mode.</p>
 */
public final class ToolApprovalPolicy {

    private ToolApprovalPolicy() {
    }

    public static boolean requiresConfirmation(ToolSettingsStore settingsStore, BaseTool tool) {
        if (settingsStore == null || tool == null) {
            return false;
        }
        PermissionResult permission = settingsStore.canExecuteTool(tool.getName(), tool.getCategory());
        return permission.isAllowed() && settingsStore.needsConfirmation(tool.getName());
    }
}
