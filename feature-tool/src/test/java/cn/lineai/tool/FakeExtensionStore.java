package cn.lineai.tool;

import cn.lineai.data.model.ExtensionOverviewState;
import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.model.ExtensionAgentConfig;
import cn.lineai.model.ExtensionMcpConfig;
import cn.lineai.model.McpRequestHeader;
import cn.lineai.model.McpToolSummary;
import cn.lineai.model.SkillRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link ExtensionStore} 的测试替身：只实现 Skills 相关契约，
 * 并记录调用参数，用于验证 skill_* 工具的委托关系。
 */
final class FakeExtensionStore implements ExtensionStore {

    final List<SkillRecord> skills = new ArrayList<>();
    final List<String> calls = new ArrayList<>();
    String lastCreatedName;
    String lastCreatedLocation;
    String lastInstalledSource;
    String lastInstalledGithub;
    String lastInstalledUri;
    String lastEnabledId;
    Boolean lastEnabledValue;
    String lastDeletedId;
    Exception failure;

    FakeExtensionStore add(String id, String name, String location, boolean enabled) {
        skills.add(new SkillRecord(id, name, "desc of " + name, "/skills/" + name,
                "/skills/" + name + "/SKILL.md", location, enabled, 0L, 0L));
        return this;
    }

    @Override
    public ExtensionOverviewState getOverview(String homePath) {
        return null;
    }

    @Override
    public List<ExtensionAgentConfig> getAgentExtensions() {
        return Collections.emptyList();
    }

    @Override
    public ExtensionAgentConfig saveAgentExtension(ExtensionAgentConfig input) {
        return input;
    }

    @Override
    public void setAgentEnabled(String id, boolean enabled) {
    }

    @Override
    public void deleteAgent(String id) {
    }

    @Override
    public List<ExtensionMcpConfig> getMcpExtensions() {
        return Collections.emptyList();
    }

    @Override
    public ExtensionMcpConfig saveMcpExtension(ExtensionMcpConfig input) {
        return input;
    }

    @Override
    public void setMcpEnabled(String id, boolean enabled) {
    }

    @Override
    public void deleteMcp(String id) {
    }

    @Override
    public List<McpToolSummary> queryMcpTools(String url, List<McpRequestHeader> headers) {
        return Collections.emptyList();
    }

    @Override
    public List<SkillRecord> getSkills(String homePath) {
        calls.add("getSkills:" + homePath);
        return new ArrayList<>(skills);
    }

    @Override
    public SkillRecord createSkill(String homePath, String location, String name, String description, String content) {
        calls.add("createSkill:" + location + ":" + name);
        if (failure != null) {
            throw new IllegalStateException(failure.getMessage());
        }
        lastCreatedName = name;
        lastCreatedLocation = location;
        SkillRecord record = new SkillRecord("app:" + name, name, description, "/skills/" + name,
                "/skills/" + name + "/SKILL.md", location, true, 0L, 0L);
        skills.add(record);
        return record;
    }

    @Override
    public SkillRecord installSkill(String homePath, String location, String sourcePath, String name) {
        calls.add("installSkill:" + location + ":" + sourcePath);
        if (failure != null) {
            throw new IllegalStateException(failure.getMessage());
        }
        lastInstalledSource = sourcePath;
        return installed(location, sourcePath);
    }

    @Override
    public SkillRecord installSkillFromUri(String homePath, String location, String uri, String displayName) {
        calls.add("installSkillFromUri:" + location + ":" + uri);
        if (failure != null) {
            throw new IllegalStateException(failure.getMessage());
        }
        lastInstalledUri = uri;
        return installed(location, uri);
    }

    @Override
    public SkillRecord installSkillFromGitHub(String homePath, String location, String githubUrl) {
        calls.add("installSkillFromGitHub:" + location + ":" + githubUrl);
        if (failure != null) {
            throw new IllegalStateException(failure.getMessage());
        }
        lastInstalledGithub = githubUrl;
        return installed(location, githubUrl);
    }

    private SkillRecord installed(String location, String source) {
        SkillRecord record = new SkillRecord(location + ":installed", "installed-skill", "installed",
                "/skills/installed-skill", "/skills/installed-skill/SKILL.md", location, true, 0L, 0L);
        skills.add(record);
        return record;
    }

    @Override
    public void setSkillEnabled(String id, boolean enabled) {
        calls.add("setSkillEnabled:" + id + ":" + enabled);
        lastEnabledId = id;
        lastEnabledValue = enabled;
    }

    @Override
    public void deleteSkill(String id) {
        calls.add("deleteSkill:" + id);
        lastDeletedId = id;
    }

    @Override
    public void deleteSkills(List<String> ids) {
        for (String id : ids) {
            deleteSkill(id);
        }
    }

    @Override
    public String buildExtensionPrompt(String homePath) {
        return "";
    }

    @Override
    public ArrayList<String> skillWriteRoots(String homePath) {
        return new ArrayList<>();
    }
}
