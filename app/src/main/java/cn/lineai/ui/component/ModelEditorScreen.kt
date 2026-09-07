package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.model.ModelProtocolType
import cn.lineai.ui.model.ModelCatalogKind
import cn.lineai.ui.model.ModelEditorUiAction
import cn.lineai.ui.model.ModelEditorUiState
import cn.lineai.ui.model.ModelEditorViewModel
import cn.lineai.ui.theme.LineTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelEditorScreen(
    state: ModelEditorUiState,
    saveEnabled: Boolean,
    canQueryMain: Boolean,
    canQueryCompression: Boolean,
    onAction: (ModelEditorUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        ModelEditorHeader(
            editing = state.editing,
            testVisible = state.testVisible,
            saveEnabled = saveEnabled,
            onBack = { onAction(ModelEditorUiAction.Back) },
            onTest = { onAction(ModelEditorUiAction.Test) },
            onSave = { onAction(ModelEditorUiAction.Save) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(LineTheme.LG.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProviderRow(state = state, onAction = onAction)
            if (state.local) {
                LocalForm(state = state, onAction = onAction)
            } else {
                RemoteForm(
                    state = state,
                    canQueryMain = canQueryMain,
                    canQueryCompression = canQueryCompression,
                    onAction = onAction
                )
            }
        }
    }

    if (state.picker.visible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onAction(ModelEditorUiAction.DismissPicker) },
            sheetState = sheetState,
            containerColor = Color(LineTheme.SURFACE)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(horizontal = LineTheme.LG.dp, vertical = LineTheme.SM.dp)
            ) {
                Text(
                    text = stringResource(R.string.screen_model_add_picker_title),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_LG.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    state.picker.ids.forEach { id ->
                        PickerRow(
                            label = id,
                            selected = id == state.picker.selectedId,
                            accent = false,
                            onClick = {
                                if (state.picker.kind == ModelCatalogKind.MAIN) {
                                    onAction(ModelEditorUiAction.SelectMainModel(id))
                                } else {
                                    onAction(ModelEditorUiAction.SelectCompressionModel(id))
                                }
                            }
                        )
                    }
                    PickerRow(
                        label = stringResource(R.string.screen_model_add_custom_id_picker),
                        selected = false,
                        accent = true,
                        onClick = {
                            if (state.picker.kind == ModelCatalogKind.MAIN) {
                                onAction(ModelEditorUiAction.SelectMainCustomRow)
                            } else {
                                onAction(ModelEditorUiAction.SelectCompressionCustomRow)
                            }
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ModelEditorHeader(
    editing: Boolean,
    testVisible: Boolean,
    saveEnabled: Boolean,
    onBack: () -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(LineTheme.BG))
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(52.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2039", color = Color(LineTheme.TEXT), fontSize = 28.sp)
        }
        Text(
            text = stringResource(
                if (editing) R.string.screen_model_add_title_edit
                else R.string.screen_model_add_title_add
            ),
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_LG.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (testVisible) {
            Text(
                text = stringResource(R.string.screen_model_add_test_button),
                modifier = Modifier
                    .clickable(onClick = onTest)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                color = Color(LineTheme.ACCENT),
                fontSize = LineTheme.FONT_SM.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = stringResource(R.string.common_save),
            modifier = Modifier
                .alpha(if (saveEnabled) 1f else 0.45f)
                .clickable(enabled = saveEnabled, onClick = onSave)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            color = Color(if (saveEnabled) LineTheme.ACCENT else LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_SM.sp,
            fontWeight = FontWeight.Bold
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(LineTheme.BORDER))
    )
}

@Composable
private fun ProviderRow(
    state: ModelEditorUiState,
    onAction: (ModelEditorUiAction) -> Unit
) {
    val selected = when {
        state.local -> 3
        state.protocol == ModelProtocolType.CODEX_RESPONSES -> 1
        state.protocol == ModelProtocolType.ANTHROPIC_MESSAGES -> 2
        else -> 0
    }
    val labels = listOf(
        "OpenAI",
        "Codex",
        "Anthropic",
        stringResource(R.string.model_provider_local)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (!state.providerLabel.isNullOrEmpty()) {
                stringResource(R.string.screen_model_add_provider_label) + state.providerLabel
            } else {
                stringResource(R.string.screen_model_add_provider_title)
            },
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val active = index == selected
                val enabledLook = !state.lockedPreset || index == selected || index == 3
                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (enabledLook) 1f else 0.4f)
                        .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                        .background(
                            Color(if (active) LineTheme.ACCENT_MUTED else LineTheme.SURFACE_ELEVATED)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(if (active) LineTheme.ACCENT else LineTheme.BORDER_LIGHT),
                            shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
                        )
                        .clickable { onAction(ModelEditorUiAction.SelectProvider(index)) }
                        .padding(vertical = 8.dp),
                    color = Color(if (active) LineTheme.ACCENT else LineTheme.TEXT),
                    fontSize = LineTheme.FONT_XS.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RemoteForm(
    state: ModelEditorUiState,
    canQueryMain: Boolean,
    canQueryCompression: Boolean,
    onAction: (ModelEditorUiAction) -> Unit
) {
    EditorField(
        label = stringResource(R.string.screen_model_add_field_name),
        value = state.name,
        onValueChange = { onAction(ModelEditorUiAction.SetName(it)) },
        hint = stringResource(
            if (state.hasPreset) R.string.screen_model_add_hint_name_optional
            else R.string.screen_model_add_hint_remote_name
        )
    )
    EditorField(
        label = stringResource(R.string.screen_model_add_field_base_url),
        value = state.baseUrl,
        onValueChange = { onAction(ModelEditorUiAction.SetBaseUrl(it)) },
        hint = state.presetPlaceholder.ifBlank {
            ModelEditorViewModel.placeholderFor(state.protocol)
        }
    )
    Text(
        text = state.presetHint.ifBlank { baseUrlHint(state.protocol) },
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp
    )
    EditorField(
        label = stringResource(R.string.screen_model_add_field_api_key),
        value = state.apiKey,
        onValueChange = { onAction(ModelEditorUiAction.SetApiKey(it)) },
        hint = stringResource(R.string.screen_model_add_hint_api_key),
        hidden = true
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.screen_model_add_field_model_id),
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp
        )
        Text(
            text = stringResource(R.string.screen_model_add_custom_id_label),
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Switch(
            checked = state.mainCatalog.customIdEnabled,
            onCheckedChange = { onAction(ModelEditorUiAction.SetCustomModelId(it)) },
            colors = lineSwitchColors()
        )
    }
    if (state.mainCatalog.customIdEnabled) {
        EditorField(
            label = "",
            value = state.mainCatalog.customIdText,
            onValueChange = { onAction(ModelEditorUiAction.SetModelId(it)) },
            hint = stringResource(R.string.screen_model_add_hint_model_id)
        )
    } else {
        CatalogSelector(
            selectedId = state.mainCatalog.selectedId,
            fetching = state.mainCatalog.fetching,
            canQuery = canQueryMain,
            onQuery = { onAction(ModelEditorUiAction.QueryMainCatalog) }
        )
    }
    EditorField(
        label = stringResource(R.string.screen_model_add_field_tool_call_limit),
        value = state.toolCallLimitText,
        onValueChange = { onAction(ModelEditorUiAction.SetToolLimit(it)) },
        hint = stringResource(R.string.screen_model_add_hint_tool_call_limit),
        keyboardType = KeyboardType.Number
    )
    Text(
        text = stringResource(R.string.screen_model_add_max_tool_calls_hint),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp
    )
    EditorField(
        label = stringResource(R.string.model_field_context_size),
        value = state.contextSizeText,
        onValueChange = { onAction(ModelEditorUiAction.SetContextSize(it)) },
        hint = stringResource(R.string.model_field_context_size_hint)
    )
    Text(
        text = stringResource(R.string.model_field_context_size_hint_desc),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp
    )
    if (state.compression.supported) {
        CompressionSection(
            state = state,
            canQuery = canQueryCompression,
            onAction = onAction
        )
    }
}

@Composable
private fun LocalForm(
    state: ModelEditorUiState,
    onAction: (ModelEditorUiAction) -> Unit
) {
    EditorField(
        label = stringResource(R.string.screen_model_add_field_name),
        value = state.name,
        onValueChange = { onAction(ModelEditorUiAction.SetName(it)) },
        hint = stringResource(R.string.screen_model_add_hint_local_name)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .clickable { onAction(ModelEditorUiAction.LocalFilePlaceholder) }
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_model_add_local_field_file),
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.screen_model_add_local_pick_file_label),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_MD.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.screen_model_add_local_pick_file_desc),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp
        )
    }
    EditorField(
        label = stringResource(R.string.screen_model_add_context_length_label),
        value = state.localContextText,
        onValueChange = {},
        hint = "4096"
    )
    Text(
        text = stringResource(R.string.screen_model_add_context_length_hint),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp
    )
    Text(
        text = stringResource(R.string.screen_model_add_acceleration_label),
        color = Color(LineTheme.TEXT_SECONDARY),
        fontSize = LineTheme.FONT_XS.sp
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            stringResource(R.string.screen_model_add_acceleration_auto) to true,
            stringResource(R.string.screen_model_add_acceleration_cpu) to false,
            stringResource(R.string.screen_model_add_acceleration_npu) to false
        ).forEach { (label, selected) ->
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                    .background(
                        Color(if (selected) LineTheme.ACCENT_MUTED else LineTheme.SURFACE_ELEVATED)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(if (selected) LineTheme.ACCENT else LineTheme.BORDER_LIGHT),
                        shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
                    )
                    .padding(vertical = 8.dp),
                color = Color(if (selected) LineTheme.ACCENT else LineTheme.TEXT),
                fontSize = LineTheme.FONT_XS.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Text(
        text = stringResource(R.string.screen_model_add_acceleration_auto_desc),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp
    )
}

@Composable
private fun CompressionSection(
    state: ModelEditorUiState,
    canQuery: Boolean,
    onAction: (ModelEditorUiAction) -> Unit
) {
    SwitchLine(
        title = stringResource(R.string.screen_model_add_compaction_label),
        checked = state.compression.enabled,
        onCheckedChange = { onAction(ModelEditorUiAction.SetCompressionEnabled(it)) }
    )
    Text(
        text = stringResource(R.string.screen_model_add_compaction_hint),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp
    )
    if (state.compression.detailsVisible) {
        SwitchLine(
            title = stringResource(R.string.screen_model_add_compaction_auto_label),
            checked = state.compression.auto,
            onCheckedChange = { onAction(ModelEditorUiAction.SetCompressionAuto(it)) }
        )
        if (state.compression.manualVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.screen_model_add_compaction_id_label),
                    modifier = Modifier.weight(1f),
                    color = Color(LineTheme.TEXT_SECONDARY),
                    fontSize = LineTheme.FONT_XS.sp
                )
                Text(
                    text = stringResource(R.string.screen_model_add_custom_id_label),
                    color = Color(LineTheme.TEXT_SECONDARY),
                    fontSize = LineTheme.FONT_XS.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = state.compression.catalog.customIdEnabled,
                    onCheckedChange = { onAction(ModelEditorUiAction.SetCompressionCustom(it)) },
                    colors = lineSwitchColors()
                )
            }
            if (state.compression.catalog.customIdEnabled) {
                EditorField(
                    label = "",
                    value = state.compression.catalog.customIdText,
                    onValueChange = { onAction(ModelEditorUiAction.SetCompressionModelId(it)) },
                    hint = stringResource(R.string.screen_model_add_compaction_id_hint)
                )
            } else {
                CatalogSelector(
                    selectedId = state.compression.catalog.selectedId,
                    fetching = state.compression.catalog.fetching,
                    canQuery = canQuery,
                    onQuery = { onAction(ModelEditorUiAction.QueryCompressionCatalog) }
                )
            }
        }
    }
}

@Composable
private fun CatalogSelector(
    selectedId: String,
    fetching: Boolean,
    canQuery: Boolean,
    onQuery: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (selectedId.isNotEmpty()) {
                selectedId
            } else {
                stringResource(R.string.screen_model_add_pick_first)
            },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                .background(Color(LineTheme.SURFACE_ELEVATED))
                .border(
                    width = 1.dp,
                    color = Color(LineTheme.BORDER_LIGHT),
                    shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            color = Color(if (selectedId.isNotEmpty()) LineTheme.TEXT else LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_SM.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(
                if (fetching) R.string.screen_model_add_query_button_loading
                else R.string.screen_model_add_query_button
            ),
            modifier = Modifier
                .alpha(if (canQuery || fetching) 1f else 0.45f)
                .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                .background(Color(LineTheme.SURFACE_LIGHT))
                .clickable(enabled = canQuery, onClick = onQuery)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            color = Color(LineTheme.ACCENT),
            fontSize = LineTheme.FONT_SM.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    hidden: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    if (label.isNotEmpty()) {
        Text(
            text = label,
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp
        )
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(hint) },
        singleLine = true,
        visualTransformation = if (hidden) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (hidden) KeyboardType.Password else keyboardType
        ),
        textStyle = TextStyle(
            fontSize = LineTheme.FONT_SM.sp,
            color = Color(LineTheme.TEXT)
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(LineTheme.TEXT),
            unfocusedTextColor = Color(LineTheme.TEXT),
            focusedBorderColor = Color(LineTheme.ACCENT),
            unfocusedBorderColor = Color(LineTheme.BORDER_LIGHT),
            focusedLabelColor = Color(LineTheme.ACCENT),
            unfocusedLabelColor = Color(LineTheme.TEXT_TERTIARY),
            cursorColor = Color(LineTheme.ACCENT),
            focusedPlaceholderColor = Color(LineTheme.TEXT_TERTIARY),
            unfocusedPlaceholderColor = Color(LineTheme.TEXT_TERTIARY)
        )
    )
}

@Composable
private fun SwitchLine(
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
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_SM.sp
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = lineSwitchColors())
    }
}

@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    accent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = Color(
                when {
                    accent -> LineTheme.ACCENT
                    selected -> LineTheme.ACCENT
                    else -> LineTheme.TEXT
                }
            ),
            fontSize = LineTheme.FONT_MD.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (selected && !accent) {
            Text("\u2713", color = Color(LineTheme.ACCENT), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun lineSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color(LineTheme.TEXT_ON_COLOR),
    checkedTrackColor = Color(LineTheme.ACCENT),
    uncheckedThumbColor = Color(LineTheme.TEXT_SECONDARY),
    uncheckedTrackColor = Color(LineTheme.SURFACE_LIGHT)
)

@Composable
private fun baseUrlHint(protocol: ModelProtocolType): String {
    val res = when (protocol) {
        ModelProtocolType.CODEX_RESPONSES -> R.string.screen_model_add_url_codex
        ModelProtocolType.ANTHROPIC_MESSAGES -> R.string.screen_model_add_url_anthropic
        else -> R.string.screen_model_add_url_openai
    }
    return stringResource(res)
}
