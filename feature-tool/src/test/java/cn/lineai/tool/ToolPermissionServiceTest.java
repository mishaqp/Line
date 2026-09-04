package cn.lineai.tool;

import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.model.McpSettingsState;
import cn.lineai.model.McpToolConfig;
import cn.lineai.model.WebSearchConfig;
import cn.lineai.model.tool.ToolResult;
import cn.lineai.tool.builtin.ShellExecuteTool;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public final class ToolPermissionServiceTest {

    @Test
    public void fullAccessDisablesConfirmationForDangerousTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellExecuteTool(null));
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, false);
        ToolPermissionService service = new ToolPermissionService(settings, registry);

        Assert.assertTrue(service.needsConfirmation(ShellExecuteTool.NAME));

        settings.setFullAccessEnabled(true);
        Assert.assertFalse(service.needsConfirmation(ShellExecuteTool.NAME));
    }

    @Test
    public void askModeKeepsConfirmationForDangerousTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellExecuteTool(null));
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, false);
        ToolPermissionService service = new ToolPermissionService(settings, registry);

        Assert.assertTrue(service.needsConfirmation(ShellExecuteTool.NAME));
    }

    @Test
    public void autoModeKeepsConfirmationForDangerousToolWithoutFullAccess() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellExecuteTool(null));
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_AUTO, false);
        ToolPermissionService service = new ToolPermissionService(settings, registry);

        Assert.assertTrue(service.needsConfirmation(ShellExecuteTool.NAME));
    }

    @Test
    public void fullAccessDisablesConfirmationForRegularTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new RecordingTool("file_read", ToolCategory.READ));
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, false);
        settings.setFullAccessEnabled(true);
        ToolPermissionService service = new ToolPermissionService(settings, registry);

        Assert.assertFalse(service.needsConfirmation("file_read"));
    }

    @Test
    public void confirmModeRequiresConfirmationForRegularToolWithoutFullAccess() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new RecordingTool("file_read", ToolCategory.READ));
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, false);
        ToolPermissionService service = new ToolPermissionService(settings, registry);

        Assert.assertTrue(service.needsConfirmation("file_read"));
    }

    @Test
    public void autoModeSkipsConfirmationForRegularTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new RecordingTool("file_read", ToolCategory.READ));
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_AUTO, false);
        ToolPermissionService service = new ToolPermissionService(settings, registry);

        Assert.assertFalse(service.needsConfirmation("file_read"));
    }

    @Test
    public void fullAccessDoesNotBypassReadonlyExecutionRestriction() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ShellExecuteTool(null));
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_READONLY, false);
        settings.setFullAccessEnabled(true);
        ToolPermissionService service = new ToolPermissionService(settings, registry);

        // Full Access only disables confirmation dialogs; the local read-only
        // execution restriction stays enforced.
        Assert.assertFalse(service.canExecuteTool(ShellExecuteTool.NAME, ToolCategory.SYSTEM).isAllowed());
    }

    private static final class RecordingTool extends BaseTool {
        private final String name;
        private final ToolCategory category;

        RecordingTool(String name, ToolCategory category) {
            this.name = name;
            this.category = category;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "test tool";
        }

        @Override
        public ToolCategory getCategory() {
            return category;
        }

        @Override
        public JSONObject getParameters() {
            return new JSONObject();
        }

        @Override
        public ToolResult execute(JSONObject input, ToolContext context) {
            return ToolResult.of("", name, "ok", false);
        }
    }

    private static final class TestSettings implements ToolSettingsStore {
        private final String permissionMode;
        private boolean fullAccessEnabled;

        TestSettings(String permissionMode, boolean fullAccessEnabled) {
            this.permissionMode = permissionMode;
            this.fullAccessEnabled = fullAccessEnabled;
        }

        @Override
        public String getPermissionMode() {
            return permissionMode;
        }

        @Override
        public void setPermissionMode(String mode) {
        }

        @Override
        public boolean isFullAccessEnabled() {
            return fullAccessEnabled;
        }

        @Override
        public void setFullAccessEnabled(boolean enabled) {
            this.fullAccessEnabled = enabled;
        }

        @Override
        public String getExecutionMode() {
            return EXECUTION_LOCAL;
        }

        @Override
        public void setExecutionMode(String mode) {
        }

        @Override
        public List<McpToolConfig> getConfigs() {
            return Collections.emptyList();
        }

        @Override
        public McpSettingsState getMcpSettingsState() {
            return null;
        }

        @Override
        public WebSearchConfig getWebSearchConfig() {
            return null;
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
            return Collections.singleton("shell_execute");
        }

        @Override
        public Set<String> getEnabledToolNames(Collection<ToolInfo> implementedTools) {
            return getEnabledToolNames();
        }

        @Override
        public PermissionResult canExecuteTool(String toolName, ToolCategory category) {
            if (PERMISSION_READONLY.equals(permissionMode)) {
                return PermissionResult.denied("readonly");
            }
            return PermissionResult.allowed();
        }

        @Override
        public boolean needsConfirmation(String toolName) {
            return PERMISSION_CONFIRM.equals(permissionMode);
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
}
