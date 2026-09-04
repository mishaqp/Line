package cn.lineai.tool.builtin;

import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.model.SkillRecord;
import cn.lineai.model.tool.ToolResult;
import cn.lineai.tool.R;
import cn.lineai.tool.ToolCategory;
import cn.lineai.tool.ToolContext;
import cn.lineai.tool.ToolDisplayCategory;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 安装 Skill：本地目录/ZIP 路径、content URI 或 GitHub 仓库/raw SKILL.md。
 * 三种来源都复用扩展系统里已有的安装实现。
 */
public final class SkillInstallTool extends BaseSkillTool {
    public static final String NAME = "skill_install";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Install a skill package from a local path (directory, SKILL.md or .zip), a content URI, "
                + "or a GitHub repository / raw SKILL.md URL. Requires user confirmation.";
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.WRITE;
    }

    @Override
    public ToolDisplayCategory getDisplayCategory() {
        return ToolDisplayCategory.WRITE;
    }

    @Override
    public boolean needsConfirmation() {
        return true;
    }

    @Override
    public JSONObject getParameters() throws org.json.JSONException {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("source_path", new JSONObject()
                                .put("type", "string")
                                .put("description", "Local path to a skill directory, SKILL.md or .zip"))
                        .put("uri", new JSONObject()
                                .put("type", "string")
                                .put("description", "content:// URI of a skill package"))
                        .put("github_url", new JSONObject()
                                .put("type", "string")
                                .put("description", "GitHub repository or raw SKILL.md URL"))
                        .put("name", new JSONObject()
                                .put("type", "string")
                                .put("description", "Optional display name override"))
                        .put("location", new JSONObject()
                                .put("type", "string")
                                .put("description", "app (global, default) or project (current workspace .linecode/skills)")))
                .put("required", new JSONArray());
    }

    @Override
    public ToolResult execute(JSONObject input, ToolContext context) {
        ExtensionStore store = store(context);
        if (store == null) {
            return error(unavailable(context));
        }
        String sourcePath = input.optString("source_path").trim();
        String uri = input.optString("uri").trim();
        String githubUrl = input.optString("github_url").trim();
        String name = input.optString("name").trim();
        String location = location(input.optString("location"));
        String home = home(context);
        if (sourcePath.length() == 0 && uri.length() == 0 && githubUrl.length() == 0) {
            return error(context.getString(R.string.tool_skill_source_empty));
        }
        try {
            SkillRecord record;
            if (githubUrl.length() > 0) {
                record = store.installSkillFromGitHub(home, location, githubUrl);
            } else if (uri.length() > 0) {
                record = store.installSkillFromUri(home, location, uri, name.length() > 0 ? name : null);
            } else {
                record = store.installSkill(home, location, sourcePath, name.length() > 0 ? name : null);
            }
            return ok(context.getString(R.string.tool_skill_installed, record.getName(), record.getLocation(), record.getSkillMdPath()));
        } catch (Exception e) {
            return error(context.getString(R.string.tool_skill_failed, describe(e)));
        }
    }
}
