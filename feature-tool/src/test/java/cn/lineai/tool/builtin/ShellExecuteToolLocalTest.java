package cn.lineai.tool.builtin;

import cn.lineai.model.tool.ToolResult;
import cn.lineai.tool.ToolContext;
import java.io.File;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class ShellExecuteToolLocalTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void localModeRunsPwdInWorkspace() throws Exception {
        File dir = folder.newFolder("workspace");
        ToolResult result = new ShellExecuteTool(null).execute(
                new JSONObject()
                        .put("command", "pwd")
                        .put("cwd", dir.getAbsolutePath()),
                ToolContext.builder().homePath(dir.getAbsolutePath()).build());

        Assert.assertFalse(result.isError());
        Assert.assertTrue(result.getContent().contains(dir.getCanonicalPath()));
    }

    @Test
    public void localModeCapturesStdout() throws Exception {
        ToolResult result = new ShellExecuteTool(null).execute(
                new JSONObject().put("command", "printf 'hello shell'"),
                ToolContext.builder().homePath(folder.getRoot().getAbsolutePath()).build());

        Assert.assertFalse(result.isError());
        Assert.assertTrue(result.getContent().contains("hello shell"));
    }

    @Test
    public void localModeReportsNonZeroExit() throws Exception {
        ToolResult result = new ShellExecuteTool(null).execute(
                new JSONObject().put("command", "exit 3"),
                ToolContext.builder().homePath(folder.getRoot().getAbsolutePath()).build());

        Assert.assertTrue(result.isError());
        Assert.assertTrue(result.getContent().contains("3"));
    }

    @Test
    public void localModeFallsBackToSuWhenShellCommandMissing() throws Exception {
        // "definitely_missing_command_xyz" exits 127; without a su binary the
        // tool must return the original failure instead of crashing.
        ToolResult result = new ShellExecuteTool(null).execute(
                new JSONObject().put("command", "definitely_missing_command_xyz"),
                ToolContext.builder().homePath(folder.getRoot().getAbsolutePath()).build());

        Assert.assertTrue(result.isError());
    }
}
