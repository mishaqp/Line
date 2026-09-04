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

/** 创建一个新 Skill（写入 SKILL.md 并登记到扩展系统）。 */
public final class SkillCreateTool extends BaseSkillTool {
    public static final String NAME = "skill_create";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Create a new skill: writes a SKILL.md with the given name, description and body into the "
                + "app-global or current-workspace skills directory. Requires user confirmation.";
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
                        .put("name", new JSONObject()
                                .put("type", "string")
                                .put("description", "Skill name, also used as the directory name"))
                        .put("description", new JSONObject()
                                .put("type", "string")
                                .put("description", "One-line description of when this skill applies"))
                        .put("content", new JSONObject()
                                .put("type", "string")
                                .put("description", "Markdown body: trigger conditions, steps, pitfalls, verification"))
                        .put("location", new JSONObject()
                                .put("type", "string")
                                .put("description", "app (global, default) or project (current workspace .linecode/skills)")))
                .put("required", new JSONArray().put("name"));
    }

    @Override
    public ToolResult execute(JSONObject input, ToolContext context) {
        ExtensionStore store = store(context);
        if (store == null) {
            return error(unavailable(context));
        }
        String name = input.optString("name").trim();
        if (name.length() == 0) {
            return error(context.getString(R.string.tool_skill_name_empty));
        }
        String description = input.optString("description").trim();
        String content = input.optString("content");
        String location = location(input.optString("location"));
        try {
            SkillRecord record = store.createSkill(home(context), location, name, description, content);
            return ok(context.getString(R.string.tool_skill_created, record.getName(), record.getLocation(), record.getSkillMdPath()));
        } catch (Exception e) {
            return error(context.getString(R.string.tool_skill_failed, describe(e)));
        }
    }
}
