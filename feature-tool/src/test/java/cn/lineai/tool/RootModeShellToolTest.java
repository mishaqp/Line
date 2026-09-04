package cn.lineai.tool;

import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.model.tool.ToolResult;
import cn.lineai.tool.builtin.RootCommandRunner;
import cn.lineai.tool.builtin.RootShellExecutor;
import cn.lineai.tool.builtin.RootSupport;
import cn.lineai.tool.builtin.ShellExecuteTool;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Root 执行目标下 {@code shell_execute} 的行为：走 {@code su -c}，
 * root 未授予时立即失败而不是挂起，非 0 退出码带输出返回。
 */
public final class RootModeShellToolTest {

    @After
    public void restoreRootSupport() {
        RootSupport.install(null);
    }

    @Test
    public void rootModeRunsCommandThroughSu() throws Exception {
        RecordingRunner runner = new RecordingRunner(new RootCommandRunner.Result("uid=0(root)\n", 0));
        install(runner, true);

        ToolResult result = new ShellExecuteTool(null).execute(
                new JSONObject().put("command", "id"), rootContext());

        Assert.assertFalse(result.getContent(), result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("uid=0(root)"));
        Assert.assertEquals(1, runner.scripts.size());
        // 未显式给 cwd 时，命令默认在工作区目录下执行。
        Assert.assertEquals("cd '/data/project' && id", runner.scripts.get(0));
    }

    @Test
    public void rootModeChangesDirectoryWhenCwdGiven() throws Exception {
        RecordingRunner runner = new RecordingRunner(new RootCommandRunner.Result("", 0));
        install(runner, true);

        new ShellExecuteTool(null).execute(
                new JSONObject().put("command", "ls -la").put("cwd", "/data/local/tmp"), rootContext());

        Assert.assertEquals("cd '/data/local/tmp' && ls -la", runner.scripts.get(0));
    }

    @Test
    public void rootUnavailableFailsFastWithoutRunningCommand() throws Exception {
        RecordingRunner runner = new RecordingRunner(new RootCommandRunner.Result("", 0));
        install(runner, false);

        ToolResult result = new ShellExecuteTool(null).execute(
                new JSONObject().put("command", "id"), rootContext());

        Assert.assertTrue(result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("Grant root permission"));
        Assert.assertTrue(runner.scripts.isEmpty());
    }

    @Test
    public void nonZeroExitCodeReturnsOutputAsError() throws Exception {
        RecordingRunner runner = new RecordingRunner(new RootCommandRunner.Result("pm: not found\n", 127));
        install(runner, true);

        ToolResult result = new ShellExecuteTool(null).execute(
                new JSONObject().put("command", "pm list packages"), rootContext());

        Assert.assertTrue(result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("pm: not found"));
        Assert.assertTrue(result.getContent(), result.getContent().contains("127"));
    }

    @Test
    public void timeoutIsReportedAsError() throws Exception {
        RecordingRunner runner = new RecordingRunner(null);
        runner.timeout = true;
        install(runner, true);

        ToolResult result = new ShellExecuteTool(null).execute(
                new JSONObject().put("command", "logcat"), rootContext());

        Assert.assertTrue(result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("timed out"));
    }

    @Test
    public void localModeStillUsesSshOrIpcPath() throws Exception {
        RecordingRunner runner = new RecordingRunner(new RootCommandRunner.Result("uid=0(root)\n", 0));
        install(runner, true);
        ToolContext context = ToolContext.builder()
                .homePath("/data/project")
                .stringResolver(new FakeResourceContext())
                .toolSettingsStore(new FakeToolSettingsStore().executionMode(ToolSettingsStore.EXECUTION_LOCAL))
                .build();

        ToolResult result = new ShellExecuteTool(null).execute(
                new JSONObject().put("command", "id"), context);

        // Local mode must use sh -c on the device/runner, even when a root
        // runner is installed. It must not sneak through RootSupport su.
        Assert.assertTrue(runner.scripts.isEmpty());
        Assert.assertFalse(result.getContent(), result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("uid="));
        Assert.assertFalse(result.getContent(), result.getContent().contains("uid=0(root)"));
    }

    private ToolContext rootContext() {
        return ToolContext.builder()
                .homePath("/data/project")
                .stringResolver(new FakeResourceContext())
                .toolSettingsStore(new FakeToolSettingsStore().executionMode(ToolSettingsStore.EXECUTION_ROOT))
                .build();
    }

    private void install(final RecordingRunner runner, final boolean rootAvailable) {
        RootSupport.install(new RootSupport.RunnerFactory() {
            @Override
            public RootCommandRunner runner() {
                return runner;
            }

            @Override
            public RootSupport.Availability availability() {
                return new RootSupport.Availability() {
                    @Override
                    public boolean isRootAvailable(long timeoutMs) {
                        return rootAvailable;
                    }

                    @Override
                    public void invalidateAvailability() {
                    }
                };
            }
        });
    }

    private static final class RecordingRunner implements RootCommandRunner {
        private final List<String> scripts = new ArrayList<>();
        private final Result result;
        private boolean timeout;

        RecordingRunner(Result result) {
            this.result = result;
        }

        @Override
        public Result run(String script, byte[] stdin, long timeoutMs) throws Exception {
            scripts.add(script);
            if (timeout) {
                throw new RootShellExecutor.RootTimeoutException("");
            }
            return result;
        }
    }
}
