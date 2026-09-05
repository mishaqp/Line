package cn.lineai.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Typed Navigation 3 keys used by Line while legacy Java Views are migrated.
 *
 * [screenId] is the temporary compatibility boundary with ScreenRegistry. New
 * Compose screens should consume the typed key and never parse this string.
 */
sealed interface LineDestination : NavKey {
    val screenId: String

    data object Chat : LineDestination {
        override val screenId: String = ""
    }

    data object Settings : LineDestination {
        override val screenId: String = "settings"
    }

    data object Models : LineDestination {
        override val screenId: String = "models"
    }

    data object CodexAccount : LineDestination {
        override val screenId: String = "codexAccount"
    }

    data object GrokAccount : LineDestination {
        override val screenId: String = "grokAccount"
    }

    data object ModelAddOptions : LineDestination {
        override val screenId: String = "modelAddOptions"
    }

    data object ModelAdd : LineDestination {
        override val screenId: String = "modelAdd"
    }

    data object ModelAddLocal : LineDestination {
        override val screenId: String = "modelAdd:local"
    }

    data class ModelAddPreset(val providerId: String) : LineDestination {
        override val screenId: String = "modelAdd:preset:$providerId"
    }

    data class ModelEdit(val modelId: String) : LineDestination {
        override val screenId: String = "modelEdit:$modelId"
    }

    data class Browser(val url: String) : LineDestination {
        override val screenId: String = "browser:$url"
    }

    data class Extension(val kind: String) : LineDestination {
        override val screenId: String = "extension:$kind"
    }

    data class AgentEdit(val agentId: String?) : LineDestination {
        override val screenId: String = agentId?.takeIf(String::isNotBlank)
            ?.let { "agentEdit:$it" }
            ?: "agentEdit"
    }

    data class McpEdit(val mcpId: String?) : LineDestination {
        override val screenId: String = mcpId?.takeIf(String::isNotBlank)
            ?.let { "mcpEdit:$it" }
            ?: "mcpEdit"
    }

    data class Legacy(override val screenId: String) : LineDestination
}

/**
 * Single codec at the legacy boundary. It prevents string parsing from leaking
 * into the new navigation stack and can be deleted with ScreenRegistry.
 */
object LineDestinations {
    private const val MODEL_ADD_PRESET = "modelAdd:preset:"
    private const val MODEL_EDIT = "modelEdit:"
    private const val BROWSER = "browser:"
    private const val EXTENSION = "extension:"
    private const val AGENT_EDIT = "agentEdit"
    private const val MCP_EDIT = "mcpEdit"

    @JvmStatic
    fun fromScreenId(rawId: String?): LineDestination {
        val id = rawId.orEmpty().trim()
        return when {
            id.isEmpty() -> LineDestination.Chat
            id == "settings" -> LineDestination.Settings
            id == "models" -> LineDestination.Models
            id == "codexAccount" -> LineDestination.CodexAccount
            id == "grokAccount" -> LineDestination.GrokAccount
            id == "modelAddOptions" -> LineDestination.ModelAddOptions
            id == "modelAdd" -> LineDestination.ModelAdd
            id == "modelAdd:local" -> LineDestination.ModelAddLocal
            id.startsWith(MODEL_ADD_PRESET) ->
                LineDestination.ModelAddPreset(id.removePrefix(MODEL_ADD_PRESET))
            id.startsWith(MODEL_EDIT) ->
                LineDestination.ModelEdit(id.removePrefix(MODEL_EDIT))
            id.startsWith(BROWSER) ->
                LineDestination.Browser(id.removePrefix(BROWSER))
            id.startsWith(EXTENSION) ->
                LineDestination.Extension(id.removePrefix(EXTENSION))
            id == AGENT_EDIT -> LineDestination.AgentEdit(null)
            id.startsWith("$AGENT_EDIT:") ->
                LineDestination.AgentEdit(id.removePrefix("$AGENT_EDIT:"))
            id == MCP_EDIT -> LineDestination.McpEdit(null)
            id.startsWith("$MCP_EDIT:") ->
                LineDestination.McpEdit(id.removePrefix("$MCP_EDIT:"))
            else -> LineDestination.Legacy(id)
        }
    }

    @JvmStatic
    fun parentOf(destination: LineDestination): LineDestination {
        return when (destination) {
            LineDestination.Chat,
            LineDestination.Settings -> LineDestination.Chat

            LineDestination.Models,
            LineDestination.CodexAccount,
            LineDestination.GrokAccount -> LineDestination.Settings

            LineDestination.ModelAddOptions,
            is LineDestination.ModelEdit -> LineDestination.Models

            LineDestination.ModelAdd,
            LineDestination.ModelAddLocal,
            is LineDestination.ModelAddPreset -> LineDestination.ModelAddOptions

            is LineDestination.Extension,
            is LineDestination.AgentEdit,
            is LineDestination.McpEdit -> LineDestination.Legacy("extensions")

            is LineDestination.Browser -> LineDestination.Chat
            is LineDestination.Legacy -> legacyParent(destination.screenId)
        }
    }

    private fun legacyParent(id: String): LineDestination {
        val parentId = when (id) {
            "llm", "input", "extensions", "mcp", "toolSettings", "output",
            "theme", "data", "storage", "memory", "keepAlive", "about",
            "tutorialFromSettings" -> "settings"
            "sshSettings", "termuxIntegration" -> "mcp"
            "imageUnderstandingModel", "imageGenerationModel" -> "toolSettings"
            "promptTemplates" -> "llm"
            "licenses" -> "about"
            "terminalProvider" -> "extensions"
            else -> ""
        }
        return fromScreenId(parentId)
    }
}
