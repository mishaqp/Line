package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.PromptTemplateItem
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface PromptTemplatesRepository {
    fun templates(): List<PromptTemplateUi>
    fun saveTemplate(id: String, value: String)
    fun resetTemplate(id: String)
}

data class PromptTemplateUi(
    val id: String,
    val title: String,
    val description: String,
    val sourceLabel: String,
    val variables: List<String>,
    val defaultText: String,
    val currentText: String,
    val customized: Boolean
) {
    companion object {
        fun from(item: PromptTemplateItem): PromptTemplateUi {
            return PromptTemplateUi(
                id = item.id,
                title = item.title,
                description = item.description,
                sourceLabel = item.sourceLabel,
                variables = item.variables.toList(),
                defaultText = item.defaultText,
                currentText = item.currentText,
                customized = item.isCustomized
            )
        }
    }
}

data class PromptTemplatesUiState(
    val templates: List<PromptTemplateUi> = emptyList()
)

sealed interface PromptTemplatesUiAction {
    data object Back : PromptTemplatesUiAction
    data class UpdateDraft(val id: String, val text: String) : PromptTemplatesUiAction
    data class Save(val id: String) : PromptTemplatesUiAction
    data class Reset(val id: String) : PromptTemplatesUiAction
}

/**
 * UDF owner for the Compose Prompt Templates screen. Persistence stays behind
 * [PromptTemplatesRepository]; Compose never talks to MainUiController.
 */
class PromptTemplatesViewModel(
    private val repository: PromptTemplatesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PromptTemplatesUiState(repository.templates()))
    val state: StateFlow<PromptTemplatesUiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = PromptTemplatesUiState(repository.templates())
    }

    fun onAction(action: PromptTemplatesUiAction): LineDestination? {
        return when (action) {
            PromptTemplatesUiAction.Back -> null
            is PromptTemplatesUiAction.UpdateDraft -> {
                updateTemplate(action.id) { current ->
                    current.copy(currentText = action.text)
                }
                null
            }
            is PromptTemplatesUiAction.Save -> {
                val current = template(action.id) ?: return null
                repository.saveTemplate(current.id, current.currentText)
                updateTemplate(action.id) { item ->
                    item.copy(customized = item.currentText != item.defaultText)
                }
                null
            }
            is PromptTemplatesUiAction.Reset -> {
                repository.resetTemplate(action.id)
                updateTemplate(action.id) { item ->
                    item.copy(currentText = item.defaultText, customized = false)
                }
                null
            }
        }
    }

    private fun template(id: String): PromptTemplateUi? {
        return _state.value.templates.firstOrNull { it.id == id }
    }

    private fun updateTemplate(id: String, transform: (PromptTemplateUi) -> PromptTemplateUi) {
        _state.update { state ->
            state.copy(
                templates = state.templates.map { item ->
                    if (item.id == id) transform(item) else item
                }
            )
        }
    }

    companion object {
        fun factory(repository: PromptTemplatesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PromptTemplatesViewModel(repository) as T
                }
            }
    }
}
