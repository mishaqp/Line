package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.R
import cn.lineai.model.ModelConfig
import cn.lineai.ui.model.AccountModelEditorState
import cn.lineai.ui.model.AccountModelEditorViewModel
import cn.lineai.ui.model.AccountModelProvider
import cn.lineai.ui.model.ModelEditorIssue
import cn.lineai.ui.theme.LineTheme

/**
 * First Compose screen in Line's incremental UI migration.
 *
 * It intentionally handles only account-backed providers (Codex/Grok). The
 * existing Java editor remains the fallback for API-key, Anthropic and local
 * providers while the architecture is migrated screen-by-screen.
 */
class AccountModelEditorScreenView(
    context: Context,
    provider: AccountModelProvider,
    editingModel: ModelConfig?,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onSave(model: ModelConfig)
        fun onTest(model: ModelConfig)
        fun onOpenAccount(screenId: String)
    }

    init {
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val editor: AccountModelEditorViewModel = viewModel(
                    key = "account-model-editor:${provider.kind}:${editingModel?.id ?: "new"}",
                    factory = AccountModelEditorViewModel.factory(context, provider, editingModel)
                )
                AccountModelEditorTheme {
                    AccountModelEditorScreen(
                        state = editor.state.collectAsState().value,
                        editing = editingModel != null,
                        provider = provider,
                        onBack = listener::onBack,
                        onOpenAccount = { listener.onOpenAccount(provider.accountScreenId) },
                        onRefresh = editor::refreshModels,
                        onSelectModel = editor::selectModel,
                        onNameChanged = editor::setName,
                        onAdvancedChanged = editor::setAdvancedExpanded,
                        onUseCustomIdChanged = editor::setUseCustomModelId,
                        onCustomIdChanged = editor::setCustomModelId,
                        onToolLimitChanged = editor::setToolCallLimit,
                        onContextSizeChanged = editor::setContextSize,
                        onCompressionEnabledChanged = editor::setCompressionEnabled,
                        onCompressionAutoChanged = editor::setCompressionAuto,
                        onCompressionModelIdChanged = editor::setCompressionModelId,
                        onSave = { editor.buildModel()?.let(listener::onSave) },
                        onTest = { editor.buildModel()?.let(listener::onTest) }
                    )
                }
            }
        }
        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }
}

@Composable
private fun AccountModelEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(LineTheme.ACCENT),
            onPrimary = Color(LineTheme.TEXT_ON_COLOR),
            background = Color(LineTheme.BG),
            onBackground = Color(LineTheme.TEXT),
            surface = Color(LineTheme.SURFACE),
            onSurface = Color(LineTheme.TEXT),
            surfaceVariant = Color(LineTheme.SURFACE_ELEVATED),
            onSurfaceVariant = Color(LineTheme.TEXT_SECONDARY),
            error = Color(LineTheme.DANGER)
        ),
        content = content
    )
}

@Composable
private fun AccountModelEditorScreen(
    state: AccountModelEditorState,
    editing: Boolean,
    provider: AccountModelProvider,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onRefresh: () -> Unit,
    onSelectModel: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onAdvancedChanged: (Boolean) -> Unit,
    onUseCustomIdChanged: (Boolean) -> Unit,
    onCustomIdChanged: (String) -> Unit,
    onToolLimitChanged: (String) -> Unit,
    onContextSizeChanged: (String) -> Unit,
    onCompressionEnabledChanged: (Boolean) -> Unit,
    onCompressionAutoChanged: (Boolean) -> Unit,
    onCompressionModelIdChanged: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit
) {
    val canAct = state.authenticated && state.effectiveModelId.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        EditorHeader(
            editing = editing,
            canAct = canAct,
            onBack = onBack,
            onSave = onSave,
            onTest = onTest
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AccountCard(state, provider, onOpenAccount)

            if (state.authenticated) {
                ModelSection(state, onRefresh, onSelectModel)
                LabeledTextField(
                    label = stringResource(R.string.account_model_editor_name),
                    value = state.name,
                    onValueChange = onNameChanged,
                    supporting = stringResource(R.string.account_model_editor_name_hint)
                )
            }

            IssueText(state.issue)

            TextButton(
                onClick = { onAdvancedChanged(!state.advancedExpanded) },
                enabled = state.authenticated,
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text(
                    if (state.advancedExpanded) {
                        stringResource(R.string.account_model_editor_advanced_hide)
                    } else {
                        stringResource(R.string.account_model_editor_advanced)
                    }
                )
            }

            if (state.authenticated && state.advancedExpanded) {
                AdvancedSection(
                    state = state,
                    onUseCustomIdChanged = onUseCustomIdChanged,
                    onCustomIdChanged = onCustomIdChanged,
                    onToolLimitChanged = onToolLimitChanged,
                    onContextSizeChanged = onContextSizeChanged,
                    onCompressionEnabledChanged = onCompressionEnabledChanged,
                    onCompressionAutoChanged = onCompressionAutoChanged,
                    onCompressionModelIdChanged = onCompressionModelIdChanged
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EditorHeader(
    editing: Boolean,
    canAct: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("‹", fontSize = 30.sp, color = Color(LineTheme.TEXT))
        }
        Text(
            text = stringResource(
                if (editing) R.string.account_model_editor_title_edit
                else R.string.account_model_editor_title_add
            ),
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onTest, enabled = canAct) {
            Text(stringResource(R.string.account_model_editor_test))
        }
        TextButton(onClick = onSave, enabled = canAct) {
            Text(stringResource(R.string.account_model_editor_save))
        }
    }
}

@Composable
private fun AccountCard(
    state: AccountModelEditorState,
    provider: AccountModelProvider,
    onOpenAccount: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.account_model_editor_provider),
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = 13.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(LineTheme.SURFACE_ELEVATED))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        provider.label,
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (state.authenticated) stringResource(R.string.account_model_editor_connected)
                        else stringResource(R.string.account_model_editor_signed_out),
                        color = if (state.authenticated) Color(LineTheme.SUCCESS) else Color(LineTheme.TEXT_TERTIARY),
                        fontSize = 13.sp
                    )
                }
                if (state.authenticated) {
                    val identity = listOf(state.email, state.plan)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (identity.isNotBlank()) {
                        Text(identity, color = Color(LineTheme.TEXT_SECONDARY), fontSize = 13.sp)
                    }
                } else {
                    Button(onClick = onOpenAccount) {
                        Text(stringResource(R.string.account_model_editor_open_account))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelSection(
    state: AccountModelEditorState,
    onRefresh: () -> Unit,
    onSelectModel: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.account_model_editor_model),
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = 13.sp
        )
        if (state.loadingModels) {
            Text(
                stringResource(R.string.account_model_editor_loading_models),
                color = Color(LineTheme.TEXT_TERTIARY)
            )
        } else if (state.loadError) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(LineTheme.SURFACE_ELEVATED)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        stringResource(R.string.account_model_editor_models_failed),
                        color = Color(LineTheme.TEXT_SECONDARY),
                        fontSize = 13.sp
                    )
                    OutlinedButton(onClick = onRefresh) {
                        Text(stringResource(R.string.account_model_editor_retry))
                    }
                }
            }
        } else {
            ModelDropdown(
                models = state.models,
                selected = state.selectedModelId,
                enabled = !state.useCustomModelId,
                onSelect = onSelectModel
            )
            Text(
                stringResource(R.string.account_model_editor_model_hint),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ModelDropdown(
    models: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && models.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selected.ifBlank { "—" },
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("⌄")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            models.forEach { modelId ->
                DropdownMenuItem(
                    text = { Text(modelId, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        expanded = false
                        onSelect(modelId)
                    }
                )
            }
        }
    }
}

@Composable
private fun AdvancedSection(
    state: AccountModelEditorState,
    onUseCustomIdChanged: (Boolean) -> Unit,
    onCustomIdChanged: (String) -> Unit,
    onToolLimitChanged: (String) -> Unit,
    onContextSizeChanged: (String) -> Unit,
    onCompressionEnabledChanged: (Boolean) -> Unit,
    onCompressionAutoChanged: (Boolean) -> Unit,
    onCompressionModelIdChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(LineTheme.SURFACE_ELEVATED))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SwitchRow(
                title = stringResource(R.string.account_model_editor_custom_id),
                checked = state.useCustomModelId,
                onCheckedChange = onUseCustomIdChanged
            )
            if (state.useCustomModelId) {
                LabeledTextField(
                    label = stringResource(R.string.account_model_editor_custom_id_hint),
                    value = state.customModelId,
                    onValueChange = onCustomIdChanged
                )
            }

            LabeledTextField(
                label = stringResource(R.string.account_model_editor_tool_limit),
                value = state.toolCallLimit,
                onValueChange = onToolLimitChanged,
                supporting = stringResource(R.string.account_model_editor_tool_limit_hint)
            )
            LabeledTextField(
                label = stringResource(R.string.account_model_editor_context),
                value = state.contextSize,
                onValueChange = onContextSizeChanged,
                supporting = stringResource(R.string.account_model_editor_context_hint)
            )

            SwitchRow(
                title = stringResource(R.string.account_model_editor_compression),
                checked = state.compressionEnabled,
                onCheckedChange = onCompressionEnabledChanged
            )
            if (state.compressionEnabled) {
                SwitchRow(
                    title = stringResource(R.string.account_model_editor_compression_auto),
                    checked = state.compressionAuto,
                    onCheckedChange = onCompressionAutoChanged
                )
                if (!state.compressionAuto) {
                    LabeledTextField(
                        label = stringResource(R.string.account_model_editor_compression_id),
                        value = state.compressionModelId,
                        onValueChange = onCompressionModelIdChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), color = Color(LineTheme.TEXT))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supporting: String = ""
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color(LineTheme.TEXT_SECONDARY), fontSize = 13.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (supporting.isNotBlank()) {
            Text(supporting, color = Color(LineTheme.TEXT_TERTIARY), fontSize = 12.sp)
        }
    }
}

@Composable
private fun IssueText(issue: ModelEditorIssue?) {
    val text = when (issue) {
        ModelEditorIssue.MODEL_REQUIRED -> stringResource(R.string.account_model_editor_issue_model)
        ModelEditorIssue.TOOL_LIMIT_INVALID -> stringResource(R.string.account_model_editor_issue_tool_limit)
        ModelEditorIssue.COMPRESSION_MODEL_REQUIRED -> stringResource(R.string.account_model_editor_issue_compression)
        null -> return
    }
    Text(text, color = Color(LineTheme.DANGER), fontSize = 13.sp)
}
