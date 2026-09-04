package cn.lineai.tool;

import cn.lineai.data.repository.ToolSettingsStore;
import org.junit.Assert;
import org.junit.Test;

/**
 * 权限模式与执行目标组合下的放行/确认规则，
 * 重点是「完全访问」模式与 Root 执行目标。
 */
public final class ToolPermissionServiceModesTest {

    @Test
    public void autoModeStillConfirmsDangerousTools() {
        ToolRegistry registry = new ToolRegistry();
        ToolPermissionService service = new ToolPermissionService(
                new FakeToolSettingsStore().permissionMode(ToolSettingsStore.PERMISSION_AUTO), registry);

        Assert.assertTrue(service.needsConfirmation(ToolNames.FILE_DELETE));
        Assert.assertTrue(service.needsConfirmation(ToolNames.SHELL_EXECUTE));
        Assert.assertFalse(service.needsConfirmation(ToolNames.FILE_READ));
    }

    @Test
    public void confirmModeConfirmsEverything() {
        ToolPermissionService service = new ToolPermissionService(
                new FakeToolSettingsStore().permissionMode(ToolSettingsStore.PERMISSION_CONFIRM), new ToolRegistry());

        Assert.assertTrue(service.needsConfirmation(ToolNames.FILE_READ));
        Assert.assertTrue(service.needsConfirmation(ToolNames.SKILL_CREATE));
    }

    @Test
    public void fullAccessNeverAsksForConfirmation() {
        ToolRegistry registry = new ToolRegistry();
        ToolPermissionService service = new ToolPermissionService(
                new FakeToolSettingsStore().permissionMode(ToolSettingsStore.PERMISSION_FULL_ACCESS), registry);

        Assert.assertTrue(service.isFullAccess());
        Assert.assertFalse(service.needsConfirmation(ToolNames.FILE_DELETE));
        Assert.assertFalse(service.needsConfirmation(ToolNames.SHELL_EXECUTE));
        Assert.assertFalse(service.needsConfirmation(ToolNames.SKILL_DELETE));
        Assert.assertFalse(service.needsConfirmation(ToolNames.FILE_READ));
    }

    @Test
    public void rootTargetIsAcceptedForToolsAndExtensions() {
        FakeToolSettingsStore store = new FakeToolSettingsStore()
                .executionMode(ToolSettingsStore.EXECUTION_ROOT)
                .enable(ToolNames.SHELL_EXECUTE, ToolNames.FILE_READ, ToolNames.SKILL_CREATE);
        ToolPermissionService service = new ToolPermissionService(store, new ToolRegistry());

        Assert.assertTrue(service.canExecuteTool(ToolNames.SHELL_EXECUTE, ToolCategory.SYSTEM).isAllowed());
        Assert.assertTrue(service.canExecuteTool(ToolNames.FILE_READ, ToolCategory.READ).isAllowed());
        Assert.assertTrue(service.canExecuteTool(ToolNames.SKILL_CREATE, ToolCategory.WRITE).isAllowed());
        // 自定义 MCP 扩展在 root 目标下同样可用（与本地目标一致）。
        Assert.assertTrue(service.canExecuteTool("mcpx_weather", ToolCategory.SYSTEM).isAllowed());
    }

    @Test
    public void readonlyModeStillBlocksWriteAndSkillTools() {
        FakeToolSettingsStore store = new FakeToolSettingsStore()
                .executionMode(ToolSettingsStore.EXECUTION_ROOT)
                .permissionMode(ToolSettingsStore.PERMISSION_READONLY)
                .enable(ToolNames.FILE_READ, ToolNames.SKILL_CREATE, ToolNames.SKILL_DELETE);
        ToolPermissionService service = new ToolPermissionService(store, new ToolRegistry());

        Assert.assertTrue(service.canExecuteTool(ToolNames.FILE_READ, ToolCategory.READ).isAllowed());
        Assert.assertFalse(service.canExecuteTool(ToolNames.SKILL_CREATE, ToolCategory.WRITE).isAllowed());
        Assert.assertFalse(service.canExecuteTool(ToolNames.SKILL_DELETE, ToolCategory.WRITE).isAllowed());
    }

    @Test
    public void unknownExecutionTargetIsRejected() {
        FakeToolSettingsStore store = new FakeToolSettingsStore().executionMode("carrier-pigeon");
        ToolPermissionService service = new ToolPermissionService(store, new ToolRegistry());

        Assert.assertFalse(service.canExecuteTool("mcpx_weather", ToolCategory.SYSTEM).isAllowed());
    }
}
