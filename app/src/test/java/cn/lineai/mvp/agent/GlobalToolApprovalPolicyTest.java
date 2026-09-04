package cn.lineai.mvp.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.model.McpSettingsState;
import cn.lineai.model.McpToolConfig;
import cn.lineai.model.WebSearchConfig;
import cn.lineai.model.tool.ToolCall;
import cn.lineai.model.tool.ToolResult;
import cn.lineai.mvp.ToolRunController;
import cn.lineai.tool.BaseTool;
import cn.lineai.tool.PermissionResult;
import cn.lineai.tool.ToolCategory;
import cn.lineai.tool.ToolContext;
import cn.lineai.tool.ToolExecutionCoordinator;
import cn.lineai.tool.ToolExecutor;
import cn.lineai.tool.ToolInfo;
import cn.lineai.tool.ToolPermissionService;
import cn.lineai.tool.ToolRegistry;
import cn.lineai.tool.builtin.AgentTool;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;
import org.junit.Test;

public final class GlobalToolApprovalPolicyTest {

    @Test
    public void confirmModeMovesSafeMainToolIntoSequentialApprovalQueue() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("safe_read", ToolCategory.READ, true);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, false, registry);
        ToolRunController controller = new ToolRunController(
                new ToolExecutionCoordinator(registry), registry, settings);

        ToolExecutionCoordinator.ToolExecutionPlan plan = controller.createPlan(
                Collections.singletonList(new ToolCall("call_1", tool.getName(), "{}")));

        assertTrue(controller.shouldPauseForConfirmation(new ToolCall("call_2", tool.getName(), "{}")));
        assertTrue(plan.getConcurrentTasks().isEmpty());
        assertEquals(1, plan.getSequentialTasks().size());
    }

    @Test
    public void autoModeKeepsSafeMainToolConcurrent() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("safe_read", ToolCategory.READ, true);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_AUTO, false, registry);
        ToolRunController controller = new ToolRunController(
                new ToolExecutionCoordinator(registry), registry, settings);

        ToolExecutionCoordinator.ToolExecutionPlan plan = controller.createPlan(
                Collections.singletonList(new ToolCall("call_1", tool.getName(), "{}")));

        assertFalse(controller.shouldPauseForConfirmation(new ToolCall("call_2", tool.getName(), "{}")));
        assertEquals(1, plan.getConcurrentTasks().size());
        assertTrue(plan.getSequentialTasks().isEmpty());
    }

    @Test
    public void askModeRequiresConfirmationForToolMarkedDangerous() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("dangerous_write", ToolCategory.WRITE, false, true);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, false, registry);
        ToolExecutor executor = new ToolExecutor(registry, settings, null, null, null, null, null);
        ToolCall call = new ToolCall("call_1", tool.getName(), "{}");

        ToolResult rejected = executor.execute(call, ToolContext.builder().homePath("").build());

        assertTrue(rejected.isError());
        assertEquals(0, tool.runCount);
        ToolResult accepted = executor.executeConfirmed(call, ToolContext.builder().homePath("").build());
        assertFalse(accepted.isError());
        assertEquals(1, tool.runCount);
    }

    @Test
    public void autoModeStillConfirmsDangerousToolWithoutFullAccess() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("dangerous_write", ToolCategory.WRITE, false, true);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_AUTO, false, registry);
        ToolExecutor executor = new ToolExecutor(registry, settings, null, null, null, null, null);

        ToolResult rejected = executor.execute(
                new ToolCall("call_1", tool.getName(), "{}"),
                ToolContext.builder().homePath("").build());

        assertTrue(rejected.isError());
        assertEquals(0, tool.runCount);
    }

    @Test
    public void fullAccessRunsDangerousToolWithoutConfirmation() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("dangerous_write", ToolCategory.WRITE, false, true);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, true, registry);
        ToolExecutor executor = new ToolExecutor(registry, settings, null, null, null, null, null);

        ToolResult result = executor.execute(
                new ToolCall("call_1", tool.getName(), "{}"),
                ToolContext.builder().homePath("").build());

        assertFalse(result.isError());
        assertEquals(1, tool.runCount);
    }

    @Test
    public void fullAccessSkipsConfirmationForRegularTool() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("safe_read", ToolCategory.READ, true, false);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, true, registry);
        ToolExecutor executor = new ToolExecutor(registry, settings, null, null, null, null, null);

        ToolResult result = executor.execute(
                new ToolCall("call_1", tool.getName(), "{}"),
                ToolContext.builder().homePath("").build());

        assertFalse(result.isError());
        assertEquals(1, tool.runCount);
    }

    @Test
    public void fullAccessToggleChangesConfirmationPolicy() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("dangerous_write", ToolCategory.WRITE, false, true);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_AUTO, false, registry);
        ToolRunController controller = new ToolRunController(
                new ToolExecutionCoordinator(registry), registry, settings);

        assertTrue(controller.shouldPauseForConfirmation(
                new ToolCall("call_1", tool.getName(), "{}")));

        settings.setFullAccessEnabled(true);
        assertFalse(controller.shouldPauseForConfirmation(
                new ToolCall("call_2", tool.getName(), "{}")));

        settings.setFullAccessEnabled(false);
        assertTrue(controller.shouldPauseForConfirmation(
                new ToolCall("call_3", tool.getName(), "{}")));
    }

    @Test
    public void fullAccessSubAgentSkipsReviewForDangerousTool() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("dangerous_write", ToolCategory.WRITE, false, true);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, true, registry);
        ToolExecutor executor = new ToolExecutor(registry, settings, null, null, null, null, null);
        AgentExecutionController controller = new AgentExecutionController(
                null, null, settings, executor, registry, null, null);
        controller.setToolReviewAwaiter((displayToolCallId, call, cancellationToken) -> {
            throw new AssertionError("Full access must not request review for a dangerous tool");
        });
        AgentProgressSession progress = new AgentProgressSession(
                1, "agent_call", "agent", AgentTool.TYPE_SUB_CODING, "dangerous write");

        ToolResult result = controller.executeAgentToolCall(
                new ToolCall("read_1", tool.getName(), "{}"),
                Collections.singleton(tool.getName()),
                AgentTool.TYPE_SUB_CODING,
                Collections.emptyList(),
                "",
                progress,
                new FakeHost(),
                null);

        assertFalse(result.isError());
        assertEquals(1, tool.runCount);
    }

    @Test
    public void executorCannotBypassGlobalConfirmationPolicy() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("safe_read", ToolCategory.READ, false);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, false, registry);
        ToolExecutor executor = new ToolExecutor(registry, settings, null, null, null, null, null);
        ToolCall call = new ToolCall("call_1", tool.getName(), "{}");

        ToolResult rejected = executor.execute(call, ToolContext.builder().homePath("").build());
        assertTrue(rejected.isError());
        assertEquals(0, tool.runCount);

        ToolResult accepted = executor.executeConfirmed(call, ToolContext.builder().homePath("").build());
        assertFalse(accepted.isError());
        assertEquals(1, tool.runCount);
    }

    @Test
    public void subAgentUsesSameConfirmPolicyForSafeTool() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("safe_read", ToolCategory.READ, false);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_CONFIRM, false, registry);
        ToolExecutor executor = new ToolExecutor(registry, settings, null, null, null, null, null);
        AgentExecutionController controller = new AgentExecutionController(
                null, null, settings, executor, registry, null, null);
        AtomicInteger reviewCount = new AtomicInteger();
        controller.setToolReviewAwaiter((displayToolCallId, call, cancellationToken) -> {
            reviewCount.incrementAndGet();
            return "accepted";
        });
        AgentProgressSession progress = new AgentProgressSession(
                1, "agent_call", "agent", AgentTool.TYPE_SUB_CODING, "safe read");

        ToolResult result = controller.executeAgentToolCall(
                new ToolCall("read_1", tool.getName(), "{}"),
                Collections.singleton(tool.getName()),
                AgentTool.TYPE_SUB_CODING,
                Collections.emptyList(),
                "",
                progress,
                new FakeHost(),
                null);

        assertFalse(result.isError());
        assertEquals("accepted", result.getReviewState());
        assertEquals(1, reviewCount.get());
        assertEquals(1, tool.runCount);
    }

    @Test
    public void subAgentAutoModeSkipsReviewForSafeTool() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("safe_read", ToolCategory.READ, false);
        registry.register(tool);
        TestSettings settings = new TestSettings(ToolSettingsStore.PERMISSION_AUTO, false, registry);
        ToolExecutor executor = new ToolExecutor(registry, settings, null, null, null, null, null);
        AgentExecutionController controller = new AgentExecutionController(
                null, null, settings, executor, registry, null, null);
        controller.setToolReviewAwaiter((displayToolCallId, call, cancellationToken) -> {
            throw new AssertionError("AUTO mode must not request review for a safe tool");
        });
        AgentProgressSession progress = new AgentProgressSession(
                1, "agent_call", "agent", AgentTool.TYPE_SUB_CODING, "safe read");

        ToolResult result = controller.executeAgentToolCall(
                new ToolCall("read_1", tool.getName(), "{}"),
                Collections.singleton(tool.getName()),
                AgentTool.TYPE_SUB_CODING,
                Collections.emptyList(),
                "",
                progress,
                new FakeHost(),
                null);

        assertFalse(result.isError());
        assertEquals(1, tool.runCount);
    }

    private static final class RecordingTool extends BaseTool {
        private final String name;
        private final ToolCategory category;
        private final boolean concurrencySafe;
        private final boolean requiresConfirmation;
        private int runCount;

        RecordingTool(String name, ToolCategory category, boolean concurrencySafe) {
            this(name, category, concurrencySafe, false);
        }

        RecordingTool(String name, ToolCategory category, boolean concurrencySafe, boolean requiresConfirmation) {
            this.name = name;
            this.category = category;
            this.concurrencySafe = concurrencySafe;
            this.requiresConfirmation = requiresConfirmation;
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
        public boolean isConcurrencySafe() {
            return concurrencySafe;
        }

        @Override
        public boolean needsConfirmation() {
            return requiresConfirmation;
        }

        @Override
        public JSONObject getParameters() {
            return new JSONObject();
        }

        @Override
        public ToolResult execute(JSONObject input, ToolContext context) {
            runCount++;
            return ToolResult.withReview("", name, "ok", false, "", "", "");
        }
    }

    private static final class TestSettings implements ToolSettingsStore {
        private final String permissionMode;
        private boolean fullAccessEnabled;
        private final ToolPermissionService permissionService;

        TestSettings(String permissionMode) {
            this(permissionMode, false, null);
        }

        TestSettings(String permissionMode, boolean fullAccessEnabled, ToolRegistry registry) {
            this.permissionMode = permissionMode;
            this.fullAccessEnabled = fullAccessEnabled;
            // Mirror production wiring: ToolSettingsRepository delegates
            // needsConfirmation to ToolPermissionService.
            this.permissionService = new ToolPermissionService(this, registry);
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
            return Collections.emptySet();
        }

        @Override
        public Set<String> getEnabledToolNames(Collection<ToolInfo> implementedTools) {
            return Collections.emptySet();
        }

        @Override
        public PermissionResult canExecuteTool(String toolName, ToolCategory category) {
            return PermissionResult.allowed();
        }

        @Override
        public boolean needsConfirmation(String toolName) {
            return permissionService.needsConfirmation(toolName);
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

    private static final class FakeHost implements AgentExecutionController.Host {
        @Override
        public String projectPath() {
            return "";
        }

        @Override
        public String projectSource() {
            return "";
        }

        @Override
        public void syncModePermission() {
        }

        @Override
        public void addOrReplaceToolResult(ToolResult result) {
        }

        @Override
        public void render() {
        }

        @Override
        public void scheduleAgentProgressRender(AgentProgressSession session) {
        }

        @Override
        public void postToolProgress(
                int generationId,
                cn.lineai.ai.ModelCancellationToken cancellationToken,
                String toolCallId,
                String toolName,
                String content,
                boolean error
        ) {
        }

        @Override
        public void requestAgentToolReview(String displayToolCallId, ToolCall call, ToolResult pendingToolResult) {
        }

        @Override
        public void clearAgentToolReview(String displayToolCallId) {
        }
    }
}
