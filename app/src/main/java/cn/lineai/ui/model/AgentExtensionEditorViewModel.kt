package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface AgentExtensionEditorRepository {
    fun loadSnapshot(): AgentExtensionEditorSnapshot

    @Throws(Exception::class)
    fun generateDraft(description: String): AgentExtensionDraft?

    fun saveAgentExtension(request: AgentExtensionSaveRequest)
}

data class AgentExtensionIdentity(
    val id: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class AgentExtensionDraft(
    val name: String,
    val slug: String,
    val prompt: String,
    val trigger: String,
    val toolNames: List<String>,
    val mcpIds: List<String>
)

data class AgentToolOption(
    val name: String,
    val description: String,
    val selectedByDefault: Boolean
)

data class AgentMcpOption(
    val id: String,
    val label: String,
    val description: String
)

data class AgentExtensionEditorSnapshot(
    val identity: AgentExtensionIdentity?,
    val initialDraft: AgentExtensionDraft?,
    val tools: List<AgentToolOption>,
    val mcps: List<AgentMcpOption>
)

data class AgentExtensionSaveRequest(
    val identity: AgentExtensionIdentity?,
    val name: String,
    val slug: String,
    val prompt: String,
    val trigger: String,
    val toolNames: List<String>,
    val mcpIds: List<String>
)

data class AgentExtensionEditorUiState(
    val name: String = "",
    val slug: String = "",
    val prompt: String = "",
    val trigger: String = "",
    val tools: List<AgentToolOption> = emptyList(),
    val mcps: List<AgentMcpOption> = emptyList(),
    val selectedToolNames: Set<String> = emptySet(),
    val selectedMcpIds: Set<String> = emptySet(),
    val showAiDialog: Boolean = false,
    val aiDescription: String = "",
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false
) {
    val selectedToolCount: Int
        get() = selectedToolNames.size

    val selectedMcpCount: Int
        get() = selectedMcpIds.size

    val isBusy: Boolean
        get() = isGenerating || isSaving
}

sealed interface AgentExtensionEditorUiAction {
    data object Back : AgentExtensionEditorUiAction
    data object Save : AgentExtensionEditorUiAction
    data class SetName(val value: String) : AgentExtensionEditorUiAction
    data class SetSlug(val value: String) : AgentExtensionEditorUiAction
    data class SetPrompt(val value: String) : AgentExtensionEditorUiAction
    data class SetTrigger(val value: String) : AgentExtensionEditorUiAction
    data class ToggleTool(val name: String) : AgentExtensionEditorUiAction
    data class ToggleMcp(val id: String) : AgentExtensionEditorUiAction
    data object OpenAiDialog : AgentExtensionEditorUiAction
    data object DismissAiDialog : AgentExtensionEditorUiAction
    data class SetAiDescription(val value: String) : AgentExtensionEditorUiAction
    data object GenerateDraft : AgentExtensionEditorUiAction
}

sealed interface AgentExtensionEditorUiEffect {
    data object Back : AgentExtensionEditorUiEffect
    data object GenerateRequiresDescription : AgentExtensionEditorUiEffect
    data object DraftGenerated : AgentExtensionEditorUiEffect
    data object DraftMissing : AgentExtensionEditorUiEffect
    data class GenerateFailed(val message: String) : AgentExtensionEditorUiEffect
    data object SaveRequiresFields : AgentExtensionEditorUiEffect
    data class SaveFailed(val message: String) : AgentExtensionEditorUiEffect
}

class AgentExtensionEditorViewModel(
    private val repository: AgentExtensionEditorRepository,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val snapshot = repository.loadSnapshot()
    private val identity = snapshot.identity
    private val initialDraft = snapshot.initialDraft

    private val _state = MutableStateFlow(
        AgentExtensionEditorUiState(
            name = initialDraft?.name.orEmpty(),
            slug = initialDraft?.slug.orEmpty(),
            prompt = initialDraft?.prompt.orEmpty(),
            trigger = initialDraft?.trigger.orEmpty(),
            tools = snapshot.tools.toList(),
            mcps = snapshot.mcps.toList(),
            selectedToolNames = initialDraft?.toolNames?.toSet()
                ?: snapshot.tools.filter { it.selectedByDefault }.map { it.name }.toSet(),
            selectedMcpIds = initialDraft?.mcpIds?.toSet().orEmpty()
        )
    )
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AgentExtensionEditorUiEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    fun onAction(action: AgentExtensionEditorUiAction): AgentExtensionEditorUiEffect? =
        when (action) {
            AgentExtensionEditorUiAction.Back ->
                if (_state.value.isBusy) null else AgentExtensionEditorUiEffect.Back
            AgentExtensionEditorUiAction.Save -> save()
            is AgentExtensionEditorUiAction.SetName -> updateForm { it.copy(name = action.value) }
            is AgentExtensionEditorUiAction.SetSlug -> updateForm { it.copy(slug = action.value) }
            is AgentExtensionEditorUiAction.SetPrompt -> updateForm { it.copy(prompt = action.value) }
            is AgentExtensionEditorUiAction.SetTrigger -> updateForm { it.copy(trigger = action.value) }
            is AgentExtensionEditorUiAction.ToggleTool -> {
                toggleTool(action.name)
                null
            }
            is AgentExtensionEditorUiAction.ToggleMcp -> {
                toggleMcp(action.id)
                null
            }
            AgentExtensionEditorUiAction.OpenAiDialog -> {
                if (!_state.value.isBusy) {
                    _state.update { it.copy(showAiDialog = true) }
                }
                null
            }
            AgentExtensionEditorUiAction.DismissAiDialog -> {
                if (!_state.value.isGenerating) {
                    _state.update { it.copy(showAiDialog = false) }
                }
                null
            }
            is AgentExtensionEditorUiAction.SetAiDescription -> {
                if (!_state.value.isGenerating) {
                    _state.update { it.copy(aiDescription = action.value) }
                }
                null
            }
            AgentExtensionEditorUiAction.GenerateDraft -> generateDraft()
        }

    private fun updateForm(
        transform: (AgentExtensionEditorUiState) -> AgentExtensionEditorUiState
    ): AgentExtensionEditorUiEffect? {
        if (!_state.value.isBusy) {
            _state.update(transform)
        }
        return null
    }

    private fun toggleTool(name: String) {
        val current = _state.value
        if (current.isBusy || current.tools.none { it.name == name }) return
        _state.update {
            it.copy(selectedToolNames = it.selectedToolNames.toggled(name))
        }
    }

    private fun toggleMcp(id: String) {
        val current = _state.value
        if (current.isBusy || current.mcps.none { it.id == id }) return
        _state.update {
            it.copy(selectedMcpIds = it.selectedMcpIds.toggled(id))
        }
    }

    private fun generateDraft(): AgentExtensionEditorUiEffect? {
        val current = _state.value
        if (current.isGenerating || current.isSaving) return null
        val description = current.aiDescription.trim()
        if (description.isEmpty()) {
            return AgentExtensionEditorUiEffect.GenerateRequiresDescription
        }

        _state.update { it.copy(isGenerating = true) }
        viewModelScope.launch(workDispatcher) {
            try {
                val draft = repository.generateDraft(description)
                if (draft == null) {
                    _state.update { it.copy(isGenerating = false) }
                    _effects.tryEmit(AgentExtensionEditorUiEffect.DraftMissing)
                    return@launch
                }
                _state.update {
                    it.copy(
                        name = draft.name,
                        slug = draft.slug,
                        prompt = draft.prompt,
                        trigger = draft.trigger,
                        selectedToolNames = draft.toolNames.toSet(),
                        selectedMcpIds = draft.mcpIds.toSet(),
                        showAiDialog = false,
                        aiDescription = "",
                        isGenerating = false
                    )
                }
                _effects.tryEmit(AgentExtensionEditorUiEffect.DraftGenerated)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(isGenerating = false) }
                _effects.tryEmit(
                    AgentExtensionEditorUiEffect.GenerateFailed(error.message.orEmpty())
                )
            }
        }
        return null
    }

    private fun save(): AgentExtensionEditorUiEffect? {
        val current = _state.value
        if (current.isBusy) return null

        val name = current.name.trim()
        val slug = normalizeSlug(current.slug.ifBlank { name })
        val prompt = current.prompt.trim()
        val trigger = current.trigger.trim()
        if (name.isEmpty() || slug.isEmpty() || prompt.isEmpty()) {
            return AgentExtensionEditorUiEffect.SaveRequiresFields
        }

        val request = AgentExtensionSaveRequest(
            identity = identity,
            name = name,
            slug = slug,
            prompt = prompt,
            trigger = trigger,
            toolNames = current.selectedToolNames.toList(),
            mcpIds = current.selectedMcpIds.toList()
        )
        _state.update { it.copy(isSaving = true) }
        return try {
            repository.saveAgentExtension(request)
            null
        } catch (error: Exception) {
            _state.update { it.copy(isSaving = false) }
            AgentExtensionEditorUiEffect.SaveFailed(error.message.orEmpty())
        }
    }

    private fun normalizeSlug(value: String): String {
        val raw = value.trim().lowercase(Locale.ROOT)
        val builder = StringBuilder()
        var lastDash = false
        for (character in raw) {
            if (builder.length >= 48) break
            if (
                character in 'a'..'z' ||
                character in '0'..'9' ||
                character == '_'
            ) {
                builder.append(character)
                lastDash = false
            } else if (!lastDash && builder.isNotEmpty()) {
                builder.append('-')
                lastDash = true
            }
        }

        val clean = builder.toString().trimEnd('-', '_')
        if (clean.isEmpty()) return ""
        return if (clean.first() in 'a'..'z') clean else "agent-" + clean
    }

    private fun Set<String>.toggled(value: String): Set<String> =
        if (contains(value)) this - value else this + value

    companion object {
        fun factory(
            repository: AgentExtensionEditorRepository,
            workDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AgentExtensionEditorViewModel::class.java)) {
                    return AgentExtensionEditorViewModel(repository, workDispatcher) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
            }
        }
    }
}
