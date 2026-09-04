package cn.lineai.tool;

import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.model.tool.ToolResult;
import cn.lineai.tool.builtin.SkillCreateTool;
import cn.lineai.tool.builtin.SkillDeleteTool;
import cn.lineai.tool.builtin.SkillInstallTool;
import cn.lineai.tool.builtin.SkillListTool;
import cn.lineai.tool.builtin.SkillSetEnabledTool;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * skill_* 工具的行为：委托到扩展系统、参数校验、确认标记。
 */
public final class SkillToolsTest {

    @Test
    public void listShowsIdLocationAndEnabledState() throws Exception {
        FakeExtensionStore store = new FakeExtensionStore()
                .add("app:deploy", "deploy", "app", true)
                .add("project:lint", "lint", "project", false);

        ToolResult result = new SkillListTool().execute(new JSONObject(), context(store));

        Assert.assertFalse(result.getContent(), result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("Found 2 skill(s)"));
        Assert.assertTrue(result.getContent(), result.getContent().contains("id=app:deploy"));
        Assert.assertTrue(result.getContent(), result.getContent().contains("[disabled]"));
    }

    @Test
    public void listCanFilterToEnabledOnly() throws Exception {
        FakeExtensionStore store = new FakeExtensionStore()
                .add("app:deploy", "deploy", "app", true)
                .add("project:lint", "lint", "project", false);

        ToolResult result = new SkillListTool().execute(
                new JSONObject().put("enabled_only", true), context(store));

        Assert.assertTrue(result.getContent(), result.getContent().contains("Found 1 skill(s)"));
        Assert.assertFalse(result.getContent(), result.getContent().contains("id=project:lint"));
    }

    @Test
    public void listWithoutSkillsExplainsHowToInstall() throws Exception {
        ToolResult result = new SkillListTool().execute(new JSONObject(), context(new FakeExtensionStore()));

        Assert.assertFalse(result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("skill_install"));
    }

    @Test
    public void createWritesSkillIntoRequestedLocation() throws Exception {
        FakeExtensionStore store = new FakeExtensionStore();

        ToolResult result = new SkillCreateTool().execute(new JSONObject()
                .put("name", "release-checklist")
                .put("description", "Steps before publishing a release")
                .put("content", "# Release checklist")
                .put("location", "project"), context(store));

        Assert.assertFalse(result.getContent(), result.isError());
        Assert.assertEquals("release-checklist", store.lastCreatedName);
        Assert.assertEquals("project", store.lastCreatedLocation);
        Assert.assertTrue(result.getContent(), result.getContent().contains("Created skill"));
    }

    @Test
    public void createRequiresName() throws Exception {
        ToolResult result = new SkillCreateTool().execute(new JSONObject(), context(new FakeExtensionStore()));

        Assert.assertTrue(result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("cannot be empty"));
    }

    @Test
    public void createNormalizesUnknownLocationToApp() throws Exception {
        FakeExtensionStore store = new FakeExtensionStore();

        new SkillCreateTool().execute(new JSONObject()
                .put("name", "x")
                .put("location", "ssh"), context(store));

        Assert.assertEquals("app", store.lastCreatedLocation);
    }

    @Test
    public void installSupportsLocalPathUriAndGitHub() throws Exception {
        FakeExtensionStore store = new FakeExtensionStore();
        SkillInstallTool tool = new SkillInstallTool();

        tool.execute(new JSONObject().put("source_path", "/sdcard/pack.zip"), context(store));
        tool.execute(new JSONObject().put("uri", "content://downloads/1"), context(store));
        tool.execute(new JSONObject().put("github_url", "https://github.com/acme/skills"), context(store));

        Assert.assertEquals("/sdcard/pack.zip", store.lastInstalledSource);
        Assert.assertEquals("content://downloads/1", store.lastInstalledUri);
        Assert.assertEquals("https://github.com/acme/skills", store.lastInstalledGithub);
    }

    @Test
    public void installWithoutSourceIsRejected() throws Exception {
        ToolResult result = new SkillInstallTool().execute(new JSONObject(), context(new FakeExtensionStore()));

        Assert.assertTrue(result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("github_url"));
    }

    @Test
    public void setEnabledResolvesByNameAndDelegatesId() throws Exception {
        FakeExtensionStore store = new FakeExtensionStore().add("app:deploy", "deploy", "app", false);

        ToolResult result = new SkillSetEnabledTool().execute(
                new JSONObject().put("id", "deploy").put("enabled", true), context(store));

        Assert.assertFalse(result.getContent(), result.isError());
        Assert.assertEquals("app:deploy", store.lastEnabledId);
        Assert.assertEquals(Boolean.TRUE, store.lastEnabledValue);
        Assert.assertTrue(result.getContent(), result.getContent().contains("Enabled skill"));
    }

    @Test
    public void setEnabledReportsUnknownSkill() throws Exception {
        FakeExtensionStore store = new FakeExtensionStore().add("app:deploy", "deploy", "app", true);

        ToolResult result = new SkillSetEnabledTool().execute(
                new JSONObject().put("id", "nope").put("enabled", false), context(store));

        Assert.assertTrue(result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("skill_list"));
        Assert.assertNull(store.lastEnabledId);
    }

    @Test
    public void deleteRemovesResolvedSkill() throws Exception {
        FakeExtensionStore store = new FakeExtensionStore().add("project:lint", "lint", "project", true);

        ToolResult result = new SkillDeleteTool().execute(
                new JSONObject().put("id", "project:lint"), context(store));

        Assert.assertFalse(result.getContent(), result.isError());
        Assert.assertEquals("project:lint", store.lastDeletedId);
        Assert.assertTrue(result.getContent(), result.getContent().contains("Deleted skill"));
    }

    @Test
    public void repositoryFailureIsSurroundedByFriendlyMessage() throws Exception {
        FakeExtensionStore store = new FakeExtensionStore();
        store.failure = new Exception("no space left on device");

        ToolResult result = new SkillCreateTool().execute(
                new JSONObject().put("name", "x"), context(store));

        Assert.assertTrue(result.isError());
        Assert.assertTrue(result.getContent(), result.getContent().contains("no space left on device"));
    }

    @Test
    public void toolsRequireExtensionStore() throws Exception {
        ToolContext context = ToolContext.builder()
                .homePath("/data/project")
                .stringResolver(new FakeResourceContext())
                .build();

        Assert.assertTrue(new SkillListTool().execute(new JSONObject(), context).isError());
        Assert.assertTrue(new SkillDeleteTool().execute(new JSONObject().put("id", "x"), context).isError());
    }

    @Test
    public void mutatingToolsAskForConfirmationAndListDoesNot() {
        Assert.assertTrue(new SkillCreateTool().needsConfirmation());
        Assert.assertTrue(new SkillInstallTool().needsConfirmation());
        Assert.assertTrue(new SkillSetEnabledTool().needsConfirmation());
        Assert.assertTrue(new SkillDeleteTool().needsConfirmation());
        Assert.assertFalse(new SkillListTool().needsConfirmation());
        Assert.assertEquals(ToolCategory.READ, new SkillListTool().getCategory());
        Assert.assertEquals(ToolCategory.WRITE, new SkillDeleteTool().getCategory());
    }

    @Test
    public void allFiveSkillToolsAreRegisteredAsBuiltins() {
        ToolRegistry registry = new ToolRegistry();

        Assert.assertNotNull(registry.get(ToolNames.SKILL_LIST));
        Assert.assertNotNull(registry.get(ToolNames.SKILL_CREATE));
        Assert.assertNotNull(registry.get(ToolNames.SKILL_INSTALL));
        Assert.assertNotNull(registry.get(ToolNames.SKILL_SET_ENABLED));
        Assert.assertNotNull(registry.get(ToolNames.SKILL_DELETE));
    }

    private ToolContext context(FakeExtensionStore store) {
        return ToolContext.builder()
                .homePath("/data/project")
                .stringResolver(new FakeResourceContext())
                .extensionStore(store)
                .toolSettingsStore(new FakeToolSettingsStore().executionMode(ToolSettingsStore.EXECUTION_LOCAL))
                .build();
    }
}
