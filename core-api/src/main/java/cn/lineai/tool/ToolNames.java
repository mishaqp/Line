package cn.lineai.tool;

/**
 * 内置工具名称常量集中定义，供 AI 解析层和工具层共用，
 * 避免 AI 模块直接引用工具具体实现类。
 */
public final class ToolNames {
    private ToolNames() {}

    public static final String FILE_READ = "file_read";
    public static final String FILE_WRITE = "file_write";
    public static final String FILE_EDIT = "file_edit";
    public static final String FILE_DELETE = "file_delete";
    public static final String LIST_DIR = "list_dir";
    public static final String GLOB = "glob";
    public static final String SHELL_EXECUTE = "shell_execute";
    public static final String AGENT = "agent";
    public static final String AGENT_PIPELINE = "agent_pipeline";
    public static final String AGENT_OUTPUT = "agent_output";
    public static final String TODO_UPDATE = "todo_update";
    public static final String MEMORY_UPDATE = "memory_update";
    public static final String WEB_SEARCH = "web_search";
    public static final String WEB_FETCH = "web_fetch";
    public static final String IMAGE_UNDERSTANDING = "image_understanding";
    public static final String IMAGE_GENERATION = "image_generation";
    public static final String SKILL_CREATE = "skill_create";
    public static final String SKILL_INSTALL = "skill_install";
    public static final String SKILL_LIST = "skill_list";
    public static final String SKILL_SET_ENABLED = "skill_set_enabled";
    public static final String SKILL_DELETE = "skill_delete";
    public static final String PHONE_SCREENSHOT = "phone_screenshot";
    public static final String PHONE_CLICK = "phone_click";
    public static final String PHONE_CLICK_VIEW = "phone_click_view";
    public static final String PHONE_SWIPE = "phone_swipe";
    public static final String PHONE_LONG_PRESS = "phone_long_press";
    public static final String PHONE_VIEW_HIERARCHY = "phone_view_hierarchy";
    public static final String PHONE_GLOBAL_ACTION = "phone_global_action";

    private static final String CUSTOM_AGENT_PREFIX = "agentx_";
    private static final String CUSTOM_MCP_PREFIX = "mcpx_";

    /** 判断是否为扩展工具名称（自定义 Agent 或 MCP） */
    public static boolean isExtensionToolName(String name) {
        return name != null && (name.startsWith(CUSTOM_AGENT_PREFIX) || name.startsWith(CUSTOM_MCP_PREFIX));
    }

    /** 判断是否为自定义 Agent 扩展工具名称 */
    public static boolean isCustomAgentToolName(String name) {
        return name != null && name.startsWith(CUSTOM_AGENT_PREFIX);
    }

    /** 判断是否为自定义 MCP 扩展工具名称 */
    public static boolean isCustomMcpToolName(String name) {
        return name != null && name.startsWith(CUSTOM_MCP_PREFIX);
    }
}
