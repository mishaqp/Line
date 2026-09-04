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

/** 启用/停用已安装的 Skill。 */
public final class SkillSetEnabledTool extends BaseSkillTool {
    public static final String NAME = "skill_set_enabled";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Enable or disable an installed skill by id or name. Disabled skills are not injected "
                + "into the system prompt. Requires user confirmation.";
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
                        .put("id", new JSONObject()
                                .put("type", "string")
                                .put("description", "Skill id or name as returned by skill_list"))
                        .put("enabled", new JSONObject()
                                .put("type", "boolean")
                                .put("description", "true to enable, false to disable")))
                .put("required", new JSONArray().put("id").put("enabled"));
    }

    @Override
    public ToolResult execute(JSONObject input, ToolContext context) {
        ExtensionStore store = store(context);
        if (store == null) {
            return error(unavailable(context));
        }
        String idOrName = input.optString("id").trim();
        boolean enabled = input.optBoolean("enabled", true);
        if (idOrName.length() == 0) {
            return error(context.getString(R.string.tool_skill_not_found, ""));
        }
        try {
            List<SkillRecord> skills = store.getSkills(home(context));
            SkillRecord target = find(skills, idOrName);
            if (target == null) {
                return error(context.getString(R.string.tool_skill_not_found, idOrName));
            }
            store.setSkillEnabled(target.getId(), enabled);
            return ok(context.getString(enabled ? R.string.tool_skill_enabled : R.string.tool_skill_disabled, target.getName()));
        } catch (Exception e) {
            return error(context.getString(R.string.tool_skill_failed, describe(e)));
        }
    }
}
