package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lineai.model.ExtensionMcpConfig
import cn.lineai.model.McpRequestHeader
import cn.lineai.model.McpToolSummary
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface McpExtensionEditorRepository {
    fun loadEditingMcp(): ExtensionMcpConfig?

    @Throws(Exception::class)
    fun queryTools(url: String, headers: List<McpRequestHeader>): List<McpToolSummary>

    fun saveMcpExtension(config: ExtensionMcpConfig)
}

data class McpHeaderUiState(
    val key: Long,
    val name: String,
    val value: String
)

enum class McpQueryStatus {
    IDLE,
    LOADING,
    SUCCESS,
    EMPTY,
    ERROR
}

data class McpExtensionEditorUiState(
    val name: String = "",
    val url: String = "",
    val headers: List<McpHeaderUiState> = emptyList(),
    val tools: List<McpToolSummary> = emptyList(),
    val queryStatus: McpQueryStatus = McpQueryStatus.IDLE,
    val queryError: String = "",
    val toolsMatchCurrentRequest: Boolean = false,
    val isQuerying: Boolean = false,
    val isSaving: Boolean = false
) {
    val enabledToolCount: Int
        get() = tools.count { it.isEnabled }
}

sealed interface McpExtensionEditorUiAction {
    data class SetName(val value: String) : McpExtensionEditorUiAction
    data class SetUrl(val value: String) : McpExtensionEditorUiAction
    data object AddHeader : McpExtensionEditorUiAction
    data class SetHeaderName(val key: Long, val value: String) : McpExtensionEditorUiAction
    data class SetHeaderValue(val key: Long, val value: String) : McpExtensionEditorUiAction
    data class RemoveHeader(val key: Long) : McpExtensionEditorUiAction
    data object QueryTools : McpExtensionEditorUiAction
    data class SetToolEnabled(val index: Int, val enabled: Boolean) : McpExtensionEditorUiAction
    data object Save : McpExtensionEditorUiAction
    data object Back : McpExtensionEditorUiAction
}

sealed interface McpExtensionEditorUiEffect {
    data object Back : McpExtensionEditorUiEffect
    data object UrlInvalid : McpExtensionEditorUiEffect
    data object SaveRequiresNameAndUrl : McpExtensionEditorUiEffect
    data object SaveRequiresCurrentTools : McpExtensionEditorUiEffect
    data class QueryCompleted(val count: Int) : McpExtensionEditorUiEffect
    data class QueryFailed(val message: String) : McpExtensionEditorUiEffect
    data class SaveFailed(val message: String) : McpExtensionEditorUiEffect
}

class McpExtensionEditorViewModel(
    private val repository: McpExtensionEditorRepository,
    private val queryDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private data class HeaderSnapshot(val name: String, val value: String)
    private data class RequestSnapshot(val url: String, val headers: List<HeaderSnapshot>)
    private data class RequestData(
        val url: String,
        val headers: List<McpRequestHeader>,
        val snapshot: RequestSnapshot
    )

    private val original = repository.loadEditingMcp()
    private var nextHeaderKey = 1L
    private var confirmedSnapshot: RequestSnapshot? = null
    private var queryGeneration = 0L

    private val initialHeaders = original?.requestHeaders.orEmpty().map { header ->
        McpHeaderUiState(nextHeaderKey++, header.name, header.value)
    }
    private val initialTools = original?.tools.orEmpty().toList()

    private val _state = MutableStateFlow(
        McpExtensionEditorUiState(
            name = original?.name.orEmpty(),
            url = original?.url.orEmpty(),
            headers = initialHeaders,
            tools = initialTools,
            queryStatus = if (initialTools.isEmpty()) McpQueryStatus.IDLE else McpQueryStatus.SUCCESS,
            toolsMatchCurrentRequest = initialTools.isNotEmpty()
        )
    )
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<McpExtensionEditorUiEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    init {
        if (initialTools.isNotEmpty()) {
            confirmedSnapshot = requestData(_state.value).snapshot
        }
    }

    fun onAction(action: McpExtensionEditorUiAction): McpExtensionEditorUiEffect? {
        return when (action) {
            is McpExtensionEditorUiAction.SetName -> {
                _state.update { it.copy(name = action.value) }
                null
            }
            is McpExtensionEditorUiAction.SetUrl -> {
                mutateRequestState { it.copy(url = action.value) }
                null
            }
            McpExtensionEditorUiAction.AddHeader -> {
                val key = nextHeaderKey++
                mutateRequestState { it.copy(headers = it.headers + McpHeaderUiState(key, "", "")) }
                null
            }
            is McpExtensionEditorUiAction.SetHeaderName -> {
                mutateRequestState { state ->
                    state.copy(headers = state.headers.map { header ->
                        if (header.key == action.key) header.copy(name = action.value) else header
                    })
                }
                null
            }
            is McpExtensionEditorUiAction.SetHeaderValue -> {
                mutateRequestState { state ->
                    state.copy(headers = state.headers.map { header ->
                        if (header.key == action.key) header.copy(value = action.value) else header
                    })
                }
                null
            }
            is McpExtensionEditorUiAction.RemoveHeader -> {
                mutateRequestState { state ->
                    state.copy(headers = state.headers.filterNot { it.key == action.key })
                }
                null
            }
            McpExtensionEditorUiAction.QueryTools -> queryTools()
            is McpExtensionEditorUiAction.SetToolEnabled -> {
                setToolEnabled(action.index, action.enabled)
                null
            }
            McpExtensionEditorUiAction.Save -> save()
            McpExtensionEditorUiAction.Back -> McpExtensionEditorUiEffect.Back
        }
    }

    private fun mutateRequestState(transform: (McpExtensionEditorUiState) -> McpExtensionEditorUiState) {
        _state.update { current ->
            val next = transform(current)
            val currentMatch = confirmedSnapshot != null && confirmedSnapshot == requestData(next).snapshot
            next.copy(
                toolsMatchCurrentRequest = currentMatch,
                queryStatus = when {
                    next.isQuerying -> McpQueryStatus.LOADING
                    currentMatch && next.tools.isNotEmpty() -> McpQueryStatus.SUCCESS
                    else -> McpQueryStatus.IDLE
                },
                queryError = ""
            )
        }
    }

    private fun queryTools(): McpExtensionEditorUiEffect? {
        val before = _state.value
        if (before.isQuerying) {
            return null
        }
        val request = requestData(before)
        if (!validUrl(request.url)) {
            return McpExtensionEditorUiEffect.UrlInvalid
        }

        val generation = ++queryGeneration
        _state.update {
            it.copy(
                isQuerying = true,
                queryStatus = McpQueryStatus.LOADING,
                queryError = ""
            )
        }

        viewModelScope.launch(queryDispatcher) {
            try {
                val result = repository.queryTools(request.url, request.headers).toList()
                currentCoroutineContext().ensureActive()
                if (generation != queryGeneration) {
                    return@launch
                }

                val currentRequest = requestData(_state.value)
                if (currentRequest.snapshot != request.snapshot) {
                    val stillMatchesPrevious = confirmedSnapshot != null && confirmedSnapshot == currentRequest.snapshot
                    _state.update {
                        it.copy(
                            isQuerying = false,
                            queryStatus = if (stillMatchesPrevious && it.tools.isNotEmpty()) {
                                McpQueryStatus.SUCCESS
                            } else {
                                McpQueryStatus.IDLE
                            },
                            toolsMatchCurrentRequest = stillMatchesPrevious,
                            queryError = ""
                        )
                    }
                    return@launch
                }

                confirmedSnapshot = request.snapshot
                _state.update {
                    it.copy(
                        tools = result,
                        isQuerying = false,
                        queryStatus = if (result.isEmpty()) McpQueryStatus.EMPTY else McpQueryStatus.SUCCESS,
                        queryError = "",
                        toolsMatchCurrentRequest = true
                    )
                }
                _effects.tryEmit(McpExtensionEditorUiEffect.QueryCompleted(result.size))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation != queryGeneration) {
                    return@launch
                }
                val currentRequest = requestData(_state.value)
                val stillMatchesPrevious = confirmedSnapshot != null && confirmedSnapshot == currentRequest.snapshot
                _state.update {
                    it.copy(
                        isQuerying = false,
                        queryStatus = McpQueryStatus.ERROR,
                        queryError = error.message.orEmpty(),
                        toolsMatchCurrentRequest = stillMatchesPrevious
                    )
                }
                _effects.tryEmit(McpExtensionEditorUiEffect.QueryFailed(error.message.orEmpty()))
            }
        }
        return null
    }

    private fun setToolEnabled(index: Int, enabled: Boolean) {
        _state.update { state ->
            if (index !in state.tools.indices) {
                state
            } else {
                val nextTools = state.tools.mapIndexed { currentIndex, tool ->
                    if (currentIndex == index) {
                        McpToolSummary(tool.name, enabled, tool.description, tool.inputSchemaJson)
                    } else {
                        tool
                    }
                }
                state.copy(tools = nextTools)
            }
        }
    }

    private fun save(): McpExtensionEditorUiEffect? {
        val current = _state.value
        if (current.isSaving || current.isQuerying) {
            return null
        }
        val name = current.name.trim()
        val request = requestData(current)
        if (name.isEmpty() || !validUrl(request.url)) {
            return McpExtensionEditorUiEffect.SaveRequiresNameAndUrl
        }
        if (current.tools.isEmpty() || confirmedSnapshot == null || confirmedSnapshot != request.snapshot) {
            return McpExtensionEditorUiEffect.SaveRequiresCurrentTools
        }

        val config = ExtensionMcpConfig(
            original?.id.orEmpty(),
            original?.isEnabled ?: true,
            name,
            request.url,
            request.headers,
            current.tools,
            original?.createdAt ?: 0L,
            original?.updatedAt ?: 0L
        )
        _state.update { it.copy(isSaving = true) }
        return try {
            repository.saveMcpExtension(config)
            null
        } catch (error: RuntimeException) {
            _state.update { it.copy(isSaving = false) }
            McpExtensionEditorUiEffect.SaveFailed(error.message.orEmpty())
        }
    }

    private fun requestData(state: McpExtensionEditorUiState): RequestData {
        val normalizedUrl = normalizeUrl(state.url)
        val modelHeaders = state.headers.map { McpRequestHeader(it.name, it.value) }
            .filter { it.name.isNotEmpty() }
        return RequestData(
            url = normalizedUrl,
            headers = modelHeaders,
            snapshot = RequestSnapshot(
                url = normalizedUrl,
                headers = modelHeaders.map { HeaderSnapshot(it.name, it.value) }
            )
        )
    }

    private fun normalizeUrl(raw: String): String {
        var value = raw.trim()
        while (value.endsWith("/") && value.length > "https://".length) {
            value = value.dropLast(1)
        }
        return value
    }

    private fun validUrl(url: String): Boolean {
        val normalized = url.trim().lowercase(Locale.ROOT)
        return normalized.startsWith("http://") || normalized.startsWith("https://")
    }

    companion object {
        fun factory(
            repository: McpExtensionEditorRepository,
            queryDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return McpExtensionEditorViewModel(repository, queryDispatcher) as T
            }
        }
    }
}
