package cn.lineai.tool.builtin;

import cn.lineai.tool.ExecutionTargetLabel;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public final class RootProbeTest {
    @Test
    public void uidZeroIsReady() {
        Assert.assertEquals(RootProbe.Status.READY,
                RootProbe.fromResult(new RootCommandRunner.Result("0\n", 0)));
        Assert.assertEquals(RootProbe.Status.READY,
                RootProbe.fromResult(new RootCommandRunner.Result("uid=0(root)\n", 0)));
    }

    @Test
    public void otherUidIsDenied() {
        Assert.assertEquals(RootProbe.Status.DENIED,
                RootProbe.fromResult(new RootCommandRunner.Result("10203\n", 0)));
        Assert.assertEquals(RootProbe.Status.DENIED,
                RootProbe.fromResult(new RootCommandRunner.Result("permission denied\n", 1)));
    }

    @Test
    public void timeoutAndMissing() {
        Assert.assertEquals(RootProbe.Status.TIMEOUT,
                RootProbe.fromFailure(new RootShellExecutor.RootTimeoutException("")));
        Assert.assertEquals(RootProbe.Status.MISSING,
                RootProbe.fromFailure(new IOException("Cannot run program \"su\": error=2, No such file or directory")));
    }

    @Test
    public void headerLabelIncludesTargetAndCwd() {
        Assert.assertEquals("Root · /data/project",
                ExecutionTargetLabel.format("root", "/data/project", RootProbe.Status.READY));
        Assert.assertEquals("Root · su denied · /data/project",
                ExecutionTargetLabel.format("root", "/data/project", RootProbe.Status.DENIED));
        Assert.assertEquals("Local · /sdcard/Line",
                ExecutionTargetLabel.format("local", "/sdcard/Line", null));
        Assert.assertEquals("SSH",
                ExecutionTargetLabel.format("ssh", "", null));
        Assert.assertEquals("Root · /data",
                ExecutionTargetLabel.format("root", "/storage/emulated/0", RootProbe.Status.READY));
        Assert.assertEquals("Root · /data",
                ExecutionTargetLabel.format("root", "/sdcard", RootProbe.Status.READY));
    }

    @Test
    public void rememberIsWhatAssemblerReadsWithoutCoordinator() {
        ExecutionTargetLabel.remember("root");
        Assert.assertEquals("root", ExecutionTargetLabel.current());
        ExecutionTargetLabel.remember(null);
        Assert.assertEquals("", ExecutionTargetLabel.current());
    }
}
