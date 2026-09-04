package cn.lineai.tool.builtin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * root 文件执行器的单元测试：用伪造的 {@link RootCommandRunner} 验证
 * 命令拼装、输出解析与错误分支，不依赖真实 root 设备。
 */
public final class RootFileExecutorTest {

    @Test
    public void absolutePathResolvesRelativeAgainstWorkspace() throws Exception {
        Assert.assertEquals("/data/project/app/Main.java",
                RootFileExecutor.absolutePath("/data/project", "app/Main.java"));
        Assert.assertEquals("/system/build.prop",
                RootFileExecutor.absolutePath("/data/project", "/system/build.prop"));
        Assert.assertEquals("/data/project",
                RootFileExecutor.absolutePath("/data/project/", "  "));
    }

    @Test
    public void statParsesExistsDirectoryAndSize() throws Exception {
        FakeRootRunner runner = new FakeRootRunner()
                .on("if [ -e ", "exists=1\ndir=0\nsize=4096\n", 0);
        RootFileExecutor executor = new RootFileExecutor(runner);

        RootFileExecutor.Meta meta = executor.stat("/system/build.prop");

        Assert.assertTrue(meta.exists());
        Assert.assertTrue(meta.isFile());
        Assert.assertFalse(meta.isDirectory());
        Assert.assertEquals(4096L, meta.size());
        Assert.assertTrue(runner.lastScript().contains("'/system/build.prop'"));
        Assert.assertTrue(runner.lastScript().contains("wc -c"));
    }

    @Test
    public void readRangeUsesDdWithSkipAndCount() throws Exception {
        FakeRootRunner runner = new FakeRootRunner().on("dd if=", "line one\nline two\n", 0);
        RootFileExecutor executor = new RootFileExecutor(runner);

        String content = executor.readRange("/data/log.txt", 1024L, 2048L);

        Assert.assertEquals("line one\nline two\n", content);
        Assert.assertTrue(runner.lastScript().contains("skip=1024"));
        Assert.assertTrue(runner.lastScript().contains("count=2048"));
    }

    @Test
    public void writeSendsBase64ThroughStdinNotArgv() throws Exception {
        FakeRootRunner runner = new FakeRootRunner()
                .on("if [ -e ", "exists=0\ndir=0\nsize=\n", 0)
                .on("base64 -d", "", 0);
        RootFileExecutor executor = new RootFileExecutor(runner);

        RootFileExecutor.WriteResult result = executor.write("/data/local/tmp/a.txt",
                "привет root".getBytes(StandardCharsets.UTF_8));

        Assert.assertFalse(result.existed());
        FakeRootRunner.Call write = runner.calls().get(runner.calls().size() - 1);
        Assert.assertTrue(write.script.startsWith("mkdir -p '/data/local/tmp'"));
        Assert.assertTrue(write.script.contains("base64 -d > '/data/local/tmp/a.txt'"));
        // 内容必须走 stdin：放进 argv 会超出内核 ARG_MAX。
        Assert.assertNotNull(write.stdin);
        Assert.assertEquals("привет root",
                new String(Base64.getDecoder().decode(write.stdin.trim()), StandardCharsets.UTF_8));
        Assert.assertFalse(write.script.contains(Base64.getEncoder().encodeToString("привет root".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void writeReportsFailureExitCode() {
        FakeRootRunner runner = new FakeRootRunner()
                .on("if [ -e ", "exists=0\ndir=0\nsize=\n", 0)
                .on("base64 -d", "base64: invalid input", 4);
        RootFileExecutor executor = new RootFileExecutor(runner);

        try {
            executor.write("/system/x.txt", new byte[] {1, 2, 3});
            Assert.fail("expected RootFsException");
        } catch (RootFileExecutor.RootFsException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("exit 4"));
        }
    }

    @Test
    public void writeRejectsDirectoryTarget() {
        FakeRootRunner runner = new FakeRootRunner().on("if [ -e ", "exists=1\ndir=1\nsize=0\n", 0);
        RootFileExecutor executor = new RootFileExecutor(runner);

        try {
            executor.write("/data/data", new byte[] {1});
            Assert.fail("expected RootFsException");
        } catch (RootFileExecutor.RootFsException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("directory"));
        }
    }

    @Test
    public void deleteUsesRmRfAndReportsFailure() {
        FakeRootRunner runner = new FakeRootRunner().on("rm -rf", "", 1);
        RootFileExecutor executor = new RootFileExecutor(runner);

        try {
            executor.delete("/system/app");
            Assert.fail("expected RootFsException");
        } catch (RootFileExecutor.RootFsException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("exit 1"));
        }
        Assert.assertTrue(runner.lastScript().contains("rm -rf '/system/app'"));
    }

    @Test
    public void listChildrenSortsDirectoriesFirst() throws Exception {
        FakeRootRunner runner = new FakeRootRunner()
                .on("find ", "/ws/src\n/ws/README.md\n/ws/app\n", 0)
                .on("if [ -d ", "/ws/src\n/ws/app\n", 0);
        RootFileExecutor executor = new RootFileExecutor(runner);

        List<RootFileExecutor.Entry> entries = executor.listChildren("/ws");

        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("app", entries.get(0).getName());
        Assert.assertTrue(entries.get(0).isDirectory());
        Assert.assertEquals("src", entries.get(1).getName());
        Assert.assertEquals("README.md", entries.get(2).getName());
        Assert.assertFalse(entries.get(2).isDirectory());
    }

    @Test
    public void collectTreeRendersRelativeEntries() throws Exception {
        FakeRootRunner runner = new FakeRootRunner()
                .on("find ", "/ws/src\n/ws/src/Main.java\n", 0)
                .on("if [ -d ", "/ws/src\n", 0);
        RootFileExecutor executor = new RootFileExecutor(runner);

        List<String> tree = executor.collectTree("/ws", 100);

        Assert.assertEquals(2, tree.size());
        Assert.assertTrue(tree.get(0), tree.get(0).startsWith("[DIR]  src/"));
        Assert.assertTrue(tree.get(1), tree.get(1).startsWith("[FILE] src/Main.java"));
    }

    @Test
    public void timeoutIsReportedAsRootFsException() {
        FakeRootRunner runner = new FakeRootRunner()
                .failWith(new RootShellExecutor.RootTimeoutException("partial"));
        RootFileExecutor executor = new RootFileExecutor(runner);

        try {
            executor.readAll("/data/log.txt");
            Assert.fail("expected RootFsException");
        } catch (RootFileExecutor.RootFsException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("timed out"));
        }
    }

    @Test
    public void displayPathStripsWorkspacePrefix() {
        Assert.assertEquals("app/Main.java", RootFileExecutor.displayPath("/data/project/", "/data/project/app/Main.java"));
        Assert.assertEquals(".", RootFileExecutor.displayPath("/data/project", "/data/project"));
        Assert.assertEquals("/system/build.prop", RootFileExecutor.displayPath("/data/project", "/system/build.prop"));
    }
}
