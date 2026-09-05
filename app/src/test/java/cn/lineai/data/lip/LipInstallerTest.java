package cn.lineai.data.lip;

import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.model.LipManifest;
import cn.lineai.model.LipPackageRecord;
import cn.lineai.model.SkillRecord;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public final class LipInstallerTest {
    @Rule public final TemporaryFolder folder = new TemporaryFolder();
    private final List<String> locations = new ArrayList<>();
    private final List<String> deleted = new ArrayList<>();
    private final List<Boolean> enabled = new ArrayList<>();

    private ExtensionStore store() {
        return (ExtensionStore) Proxy.newProxyInstance(ExtensionStore.class.getClassLoader(),
                new Class<?>[] {ExtensionStore.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "installSkill":
                    locations.add((String) args[1]);
                    return new SkillRecord("skill-" + locations.size(), "test", "", (String) args[2],
                            "", (String) args[1], true, 1L, 1L);
                case "deleteSkill":
                    deleted.add((String) args[0]);
                    return null;
                case "setSkillEnabled":
                    enabled.add((Boolean) args[1]);
                    return null;
                case "saveAgentExtension":
                case "saveMcpExtension":
                    return args[0];
                default:
                    throw new AssertionError("Unexpected store call: " + method.getName());
            }
        });
    }

    private LipInstaller installer(File cache) throws Exception {
        return new LipInstaller(store(), new LipPackageIndex(folder.newFolder()), cache);
    }

    private File skillFolder() throws Exception {
        File source = folder.newFolder();
        Files.write(new File(source, "SKILL.md").toPath(), "# Demo".getBytes(StandardCharsets.UTF_8));
        return source;
    }

    private File archive(String entry) throws Exception {
        File source = folder.newFile("demo.lip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(source.toPath()))) {
            out.putNextEntry(new ZipEntry(entry));
            out.write("# Demo".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return source;
    }

    @Test public void directoryInstallNeverDeletesSourceAndUsesSelectedLocation() throws Exception {
        File source = skillFolder();
        LipInstaller installer = installer(folder.newFolder());
        LipPackageRecord record = installer.installFile("/project", "project", source);
        assertTrue(new File(source, "SKILL.md").isFile());
        assertEquals("project", locations.get(0));
        assertEquals(1, record.getSkillIds().size());
    }

    @Test public void failedDirectoryInstallAlsoPreservesSource() throws Exception {
        File source = folder.newFolder();
        File sentinel = new File(source, "notes.txt");
        Files.write(sentinel.toPath(), new byte[] {1});
        try {
            installer(folder.newFolder()).installFile("/project", "app", source);
            fail("Expected empty package error");
        } catch (IllegalArgumentException expected) {
            assertTrue(sentinel.isFile());
        }
    }

    @Test public void wrappedZipCleansCacheAndReinstallKeepsIdentity() throws Exception {
        File source = archive("wrapper/skills/demo/SKILL.md");
        File cache = folder.newFolder();
        LipInstaller installer = installer(cache);
        LipPackageRecord first = installer.installFile("/project", "app", source);
        LipPackageRecord second = installer.installFile("/project", "app", source);
        assertEquals("demo", first.getId());
        assertEquals(first.getId(), second.getId());
        assertEquals(1, installer.list().size());
        assertEquals(0, cache.list().length);
        assertTrue(source.isFile());
    }

    @Test public void uriDisplayNameDefinesIdentityNotTemporaryFilename() throws Exception {
        File source = archive("SKILL.md");
        LipPackageRecord result = installer(folder.newFolder())
                .installFile("/project", "project", source, "My Package.lip");
        assertEquals("my-package", result.getId());
        assertEquals("project", locations.get(0));
    }

    @Test public void explicitScopeOverridesSelectionButMissingScopeInheritsIt() throws Exception {
        LipManifest manifest = LipManifestParser.parseJson(
                "{\"skills\":[\"one\",{\"path\":\"two\"},{\"path\":\"three\",\"location\":\"app\"}]}", "demo");
        assertEquals("", manifest.getSkills().get(0).getLocation());
        assertEquals("", manifest.getSkills().get(1).getLocation());
        assertEquals("app", manifest.getSkills().get(2).getLocation());
    }

    @Test public void invalidReplacementDoesNotDeleteInstalledComponents() throws Exception {
        File source = skillFolder();
        LipInstaller installer = installer(folder.newFolder());
        LipPackageRecord first = installer.installFile("/project", "app", source);
        Files.write(new File(source, "manifest.json").toPath(),
                ("{\"id\":\"" + first.getId() + "\",\"skills\":[\"missing\"]}")
                        .getBytes(StandardCharsets.UTF_8));
        try {
            installer.installFile("/project", "app", source);
            fail("Expected missing component error");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, deleted.size());
            assertEquals(1, installer.list().size());
        }
    }

    @Test public void toggleAndDeleteUseRecordedComponents() throws Exception {
        LipInstaller installer = installer(folder.newFolder());
        LipPackageRecord record = installer.installFile("/project", "app", skillFolder());
        installer.setEnabled(record.getId(), false);
        assertFalse(installer.list().get(0).isEnabled());
        assertEquals(Boolean.FALSE, enabled.get(0));
        installer.setEnabled(record.getId(), true);
        assertTrue(installer.list().get(0).isEnabled());
        installer.delete(record.getId());
        assertEquals(record.getSkillIds(), deleted);
        assertTrue(installer.list().isEmpty());
    }

    @Test public void failedExtractionCleansTemporaryDirectory() throws Exception {
        File cache = folder.newFolder();
        try {
            installer(cache).installFile("/project", "app", archive("../escape"));
            fail("Expected invalid archive error");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, cache.list().length);
        }
    }
}
