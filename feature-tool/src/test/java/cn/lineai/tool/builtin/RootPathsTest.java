package cn.lineai.tool.builtin;

import org.junit.Assert;
import org.junit.Test;

public final class RootPathsTest {

    @Test
    public void shellCwdDefaultsEmptyAndEmulatedToData() {
        Assert.assertEquals("/data", RootPaths.shellCwd(null));
        Assert.assertEquals("/data", RootPaths.shellCwd(""));
        Assert.assertEquals("/data", RootPaths.shellCwd("/storage/emulated/0"));
        Assert.assertEquals("/data", RootPaths.shellCwd("/storage/emulated/0/"));
        Assert.assertEquals("/data", RootPaths.shellCwd("/sdcard"));
        Assert.assertEquals("/data", RootPaths.shellCwd("/sdcard/Download"));
        Assert.assertEquals("/data", RootPaths.shellCwd("/storage/self/primary"));
    }

    @Test
    public void shellCwdKeepsRealTrees() {
        Assert.assertEquals("/data/media/0", RootPaths.shellCwd("/data/media/0"));
        Assert.assertEquals("/data/adb", RootPaths.shellCwd("/data/adb"));
        Assert.assertEquals("/data/project", RootPaths.shellCwd("/data/project"));
    }

    @Test
    public void toRootVisibleRewritesEmulatedPrefixes() {
        Assert.assertEquals("/data/media/0", RootPaths.toRootVisible("/sdcard"));
        Assert.assertEquals("/data/media/0/Download", RootPaths.toRootVisible("/sdcard/Download"));
        Assert.assertEquals("/data/media/0/DCIM/x.jpg",
                RootPaths.toRootVisible("/storage/emulated/0/DCIM/x.jpg"));
        Assert.assertEquals("/system/build.prop", RootPaths.toRootVisible("/system/build.prop"));
        Assert.assertEquals("/data/user/0", RootPaths.toRootVisible("/data/user/0"));
    }
}
