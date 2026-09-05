package cn.lineai.data.lip;

import cn.lineai.model.LipManifest;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class LipManifestParserTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void parseManifestJson() throws Exception {
        LipManifest manifest = LipManifestParser.parseJson(
                "{"
                        + "\"id\":\"github-agent\","
                        + "\"name\":\"GitHub Agent\","
                        + "\"version\":\"1.1\","
                        + "\"description\":\"Talk to GitHub\","
                        + "\"skills\":[{\"path\":\"skills/github-agent\",\"location\":\"app\"}],"
                        + "\"agents\":[\"agents/reviewer.json\"],"
                        + "\"mcps\":[{\"path\":\"mcps/gh.json\"}]"
                        + "}",
                "fallback"
        );
        Assert.assertEquals("github-agent", manifest.getId());
        Assert.assertEquals("GitHub Agent", manifest.getName());
        Assert.assertEquals("1.1", manifest.getVersion());
        Assert.assertEquals(1, manifest.getSkills().size());
        Assert.assertEquals("skills/github-agent", manifest.getSkills().get(0).getPath());
        Assert.assertEquals("agents/reviewer.json", manifest.getAgentPaths().get(0));
        Assert.assertEquals("mcps/gh.json", manifest.getMcpPaths().get(0));
        Assert.assertFalse(manifest.isEmpty());
    }

    @Test
    public void inferFromSkillLayout() throws Exception {
        File root = folder.newFolder("demo-pack");
        File skill = new File(root, "skills/hello");
        Assert.assertTrue(skill.mkdirs());
        write(new File(skill, "SKILL.md"), "# Hello\n");
        File agents = new File(root, "agents");
        Assert.assertTrue(agents.mkdirs());
        write(new File(agents, "bot.json"), "{\"name\":\"Bot\"}");
        LipManifest manifest = LipManifestParser.parseExtracted(root);
        Assert.assertEquals("demo-pack", manifest.getId());
        Assert.assertEquals(1, manifest.getSkills().size());
        Assert.assertEquals("skills/hello", manifest.getSkills().get(0).getPath());
        Assert.assertEquals("agents/bot.json", manifest.getAgentPaths().get(0));
    }

    @Test
    public void rejectEscapingPath() {
        try {
            LipManifestParser.parseJson("{\"skills\":[{\"path\":\"../secret\"}]}", "x");
            Assert.fail("expected escape rejection");
        } catch (Exception expected) {
            Assert.assertTrue(expected.getMessage().contains("escapes"));
        }
    }

    @Test
    public void sanitizeDropsExtension() {
        Assert.assertEquals("github-agent", LipManifestParser.sanitizeId("GitHub Agent.lip"));
        Assert.assertEquals("my-pack", LipManifestParser.sanitizeId("my pack.zip"));
    }

    @Test
    public void installerReadsBareSkillZip() throws Exception {
        File zip = folder.newFile("notes.lip");
        ZipOutputStream output = new ZipOutputStream(new FileOutputStream(zip));
        try {
            output.putNextEntry(new ZipEntry("SKILL.md"));
            output.write("# Notes\n".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        } finally {
            output.close();
        }
        File extracted = folder.newFolder("out");
        LipInstaller.unzip(zip, extracted);
        LipManifest manifest = LipManifestParser.parseExtracted(extracted);
        Assert.assertEquals(1, manifest.getSkills().size());
        Assert.assertEquals(".", manifest.getSkills().get(0).getPath());
    }

    private static void write(File file, String text) throws Exception {
        FileOutputStream output = new FileOutputStream(file);
        try {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        } finally {
            output.close();
        }
    }
}
