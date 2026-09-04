package cn.lineai.tool.builtin;

import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.model.SkillRecord;
import cn.lineai.tool.BaseTool;
import cn.lineai.tool.R;
import cn.lineai.tool.ToolContext;
import java.util.List;
import java.util.Locale;

/**
 * skill_* 工具的共同基类：统一解析 {@link ExtensionStore} 与 Skill 查找逻辑。
 *
 * <p>所有操作都复用扩展系统现有的 {@code SkillRepository} 实现（扫描 SKILL.md、写文件、
 * 更新启用状态），不存在第二套 Skill 存储。</p>
 */
abstract class BaseSkillTool extends BaseTool {

    protected static final String LOCATION_APP = "app";

    /** 取扩展仓库；不可用时返回 null，由调用方给出明确错误。 */
    protected ExtensionStore store(ToolContext context) {
        return context == null ? null : context.getExtensionStore();
    }

    protected String unavailable(ToolContext context) {
        return context.getString(R.string.tool_skill_store_unavailable);
    }

    protected String home(ToolContext context) {
        return context == null ? "" : context.getHomePath();
    }

    /** 归一化 location 参数：只接受 app / project，其它一律按 app 处理。 */
    protected String location(String value) {
        String normalized = SkillRecord.normalizeLocation(value == null ? "" : value.trim().toLowerCase(Locale.ROOT));
        return SkillRecord.LOCATION_SSH.equals(normalized) ? LOCATION_APP : normalized;
    }

    /** 按 id 精确匹配，其次按名称不区分大小写匹配。 */
    protected SkillRecord find(List<SkillRecord> skills, String idOrName) {
        String needle = idOrName == null ? "" : idOrName.trim();
        if (needle.length() == 0 || skills == null) {
            return null;
        }
        for (SkillRecord skill : skills) {
            if (needle.equals(skill.getId())) {
                return skill;
            }
        }
        for (SkillRecord skill : skills) {
            if (needle.equalsIgnoreCase(skill.getName())) {
                return skill;
            }
        }
        return null;
    }

    /** 统一的失败信息。 */
    protected String describe(Exception error) {
        String message = error == null ? null : error.getMessage();
        if (message != null && message.trim().length() > 0) {
            return message.trim();
        }
        return error == null ? "unknown error" : error.getClass().getSimpleName();
    }

    /** 单行摘要，用于列表输出。 */
    protected String summary(SkillRecord skill) {
        StringBuilder builder = new StringBuilder();
        builder.append("- ").append(skill.getName())
                .append(" [").append(skill.isEnabled() ? "enabled" : "disabled").append("]")
                .append(" (").append(skill.getLocation()).append(")")
                .append(" id=").append(skill.getId());
        if (skill.getDescription().length() > 0) {
            builder.append(" — ").append(skill.getDescription());
        }
        return builder.toString();
    }

    @Override
    public String promptSupplement(String executionMode, boolean isSsh) {
        return "Enabled skills are injected into the system prompt; changes made by skill_* tools "
                + "apply to the next model request.";
    }
}
