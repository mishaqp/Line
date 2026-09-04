package cn.lineai.tool.builtin;

import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.model.SkillRecord;
import cn.lineai.model.tool.ToolResult;
import cn.lineai.tool.R;
import cn.lineai.tool.ToolCategory;
import cn.lineai.tool.ToolContext;
import cn.lineai.tool.ToolDisplayCategory;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** 列出已安装的 Skills（含启用状态、location 和 id）。 */
public final class SkillListTool extends BaseSkillTool {
    public static final String NAME = "skill_list";
    private static final int MAX_LISTED = 200;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "List installed skills with id, name, location (app/project) and enabled state. "
                + "Use the returned id for skill_set_enabled and skill_delete.";
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.READ;
    }

    @Override
    public ToolDisplayCategory getDisplayCategory() {
        return ToolDisplayCategory.READ;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    @Override
    public boolean isAllowedInReadonlyMode() {
        return true;
    }

    @Override
    public JSONObject getParameters() throws org.json.JSONException {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("enabled_only", new JSONObject()
                                .put("type", "boolean")
                                .put("description", "Only list skills that are currently enabled")))
                .put("required", new JSONArray());
    }

    @Override
    public ToolResult execute(JSONObject input, ToolContext context) {
        ExtensionStore store = store(context);
        if (store == null) {
            return error(unavailable(context));
        }
        boolean enabledOnly = input != null && input.optBoolean("enabled_only", false);
        try {
            List<SkillRecord> skills = store.getSkills(home(context));
            List<SkillRecord> filtered = new ArrayList<>();
            if (skills != null) {
                for (SkillRecord skill : skills) {
                    if (!enabledOnly || skill.isEnabled()) {
                        filtered.add(skill);
                    }
                }
            }
            if (filtered.isEmpty()) {
                return ok(context.getString(R.string.tool_skill_none));
            }
            StringBuilder builder = new StringBuilder();
            builder.append(context.getString(R.string.tool_skill_list_header, filtered.size())).append('\n');
            int shown = 0;
            for (SkillRecord skill : filtered) {
                if (shown >= MAX_LISTED) {
                    builder.append("... (").append(filtered.size() - shown).append(" more)\n");
                    break;
                }
                builder.append(summary(skill)).append('\n');
                shown++;
            }
            return ok(builder.toString().trim());
        } catch (Exception e) {
            return error(context.getString(R.string.tool_skill_failed, describe(e)));
        }
    }
}
