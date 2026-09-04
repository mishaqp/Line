package cn.lineai.tool.builtin;

import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.model.SkillRecord;
import cn.lineai.model.tool.ToolResult;
import cn.lineai.tool.R;
import cn.lineai.tool.ToolCategory;
import cn.lineai.tool.ToolContext;
import cn.lineai.tool.ToolDisplayCategory;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** 删除已安装的 Skill（同时删除其 SKILL.md 目录）。 */
public final class SkillDeleteTool extends BaseSkillTool {
    public static final String NAME = "skill_delete";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Delete an installed skill by id or name, removing its SKILL.md directory. "
                + "Requires user confirmation.";
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.WRITE;
    }

    @Override
    public ToolDisplayCategory getDisplayCategory() {
        return ToolDisplayCategory.DELETE;
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
                        .put("id", new JSONObject()
                                .put("type", "string")
                                .put("description", "Skill id or name as returned by skill_list")))
                .put("required", new JSONArray().put("id"));
    }

    @Override
    public ToolResult execute(JSONObject input, ToolContext context) {
        ExtensionStore store = store(context);
        if (store == null) {
            return error(unavailable(context));
        }
        String idOrName = input.optString("id").trim();
        if (idOrName.length() == 0) {
            return error(context.getString(R.string.tool_skill_not_found, ""));
        }
        try {
            List<SkillRecord> skills = store.getSkills(home(context));
            SkillRecord target = find(skills, idOrName);
            if (target == null) {
                return error(context.getString(R.string.tool_skill_not_found, idOrName));
            }
            store.deleteSkill(target.getId());
            return ok(context.getString(R.string.tool_skill_deleted, target.getName()));
        } catch (Exception e) {
            return error(context.getString(R.string.tool_skill_failed, describe(e)));
        }
    }
}
