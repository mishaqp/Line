package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.model.WebSearchConfig
import cn.lineai.ui.model.ToolSettingsUiAction
import cn.lineai.ui.model.ToolSettingsUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ToolSettingsScreenContent(
    state: ToolSettingsUiState,
    onAction: (ToolSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_tools_title,
            onBack = { onAction(ToolSettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            ToolSectionLabel(stringResource(R.string.screen_tools_section_images))
            ToolModelCard(
                title = stringResource(R.string.screen_tools_image_understanding_label),
                description = stringResource(R.string.screen_tools_image_understanding_desc),
                selectedLabel = state.imageUnderstandingLabel,
                iconType = IconButtonView.PAINTBRUSH,
                onPick = { onAction(ToolSettingsUiAction.OpenImageUnderstandingModel) }
            )
            Spacer(Modifier.height(12.dp))
            ToolModelCard(
                title = stringResource(R.string.screen_tools_image_generation_label),
                description = stringResource(R.string.screen_tools_image_generation_desc),
                selectedLabel = state.imageGenerationLabel,
                iconType = IconButtonView.SPARKLES,
                onPick = { onAction(ToolSettingsUiAction.OpenImageGenerationModel) }
            )
            Spacer(Modifier.height(16.dp))
            ToolSectionLabel(stringResource(R.string.screen_tools_section_search))
            ToolWebSearchCard(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun ToolSectionLabel(label: String) {
    Text(
        text = label,
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ToolModelCard(
    title: String,
    description: String,
    selectedLabel: String,
    iconType: Int,
    onPick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_MD.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = description,
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            lineHeight = (LineTheme.FONT_XS + 3).sp
        )
        Spacer(Modifier.height(12.dp))
        val empty = selectedLabel.isBlank()
        Text(
            text = if (empty) stringResource(R.string.screen_tools_no_model_selected) else selectedLabel,
            color = Color(if (empty) LineTheme.TEXT_TERTIARY else LineTheme.TEXT),
            fontSize = LineTheme.FONT_SM.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        ToolPickButton(
            label = stringResource(R.string.screen_tools_pick_model),
            iconType = iconType,
            onClick = onPick
        )
    }
}

@Composable
private fun ToolPickButton(
    label: String,
    iconType: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_FULL.dp))
            .background(Color(LineTheme.ACCENT))
            .border(
                width = 1.dp,
                color = Color(LineTheme.ACCENT),
                shape = RoundedCornerShape(LineTheme.SHAPE_FULL.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            modifier = Modifier.size(15.dp),
            factory = { context ->
                IconButtonView(context, iconType).apply {
                    setIconColor(LineTheme.TEXT_ON_COLOR)
                    setIconSizeDp(15, 15)
                    isClickable = false
                }
            },
            update = { view ->
                view.setIconType(iconType)
                view.setIconColor(LineTheme.TEXT_ON_COLOR)
            }
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = Color(LineTheme.TEXT_ON_COLOR),
            fontSize = LineTheme.FONT_XS.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ToolWebSearchCard(
    state: ToolSettingsUiState,
    onAction: (ToolSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_tools_web_search_label),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_MD.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.screen_tools_web_search_desc),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            lineHeight = (LineTheme.FONT_XS + 3).sp
        )
        Spacer(Modifier.height(12.dp))
        PROVIDERS.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    ToolProviderChip(
                        label = stringResource(item.labelRes),
                        selected = state.provider == item.provider,
                        onClick = { onAction(ToolSettingsUiAction.SelectProvider(item.provider)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        if (state.showSearchFields) {
            ToolSearchField(
                value = state.baseUrl,
                onValueChange = { onAction(ToolSettingsUiAction.ChangeBaseUrl(it)) },
                label = stringResource(R.string.screen_tools_field_search_url),
                hint = "https://api.example.com/search"
            )
            ToolSearchField(
                value = state.apiKey,
                onValueChange = { onAction(ToolSettingsUiAction.ChangeApiKey(it)) },
                label = stringResource(R.string.screen_tools_field_api_key),
                hint = stringResource(R.string.screen_tools_hint_api_key),
                hidden = true
            )
            ToolSearchField(
                value = state.model,
                onValueChange = { onAction(ToolSettingsUiAction.ChangeModel(it)) },
                label = stringResource(R.string.screen_tools_field_search_model),
                hint = stringResource(R.string.screen_tools_hint_model)
            )
            ToolSearchField(
                value = state.queryParam,
                onValueChange = { onAction(ToolSettingsUiAction.ChangeQueryParam(it)) },
                label = stringResource(R.string.screen_tools_field_query_param),
                hint = "q"
            )
            ToolSearchField(
                value = state.apiKeyHeader,
                onValueChange = { onAction(ToolSettingsUiAction.ChangeApiKeyHeader(it)) },
                label = stringResource(R.string.screen_tools_field_key_header),
                hint = stringResource(R.string.screen_tools_hint_key_header)
            )
            ToolSearchField(
                value = state.apiKeyParam,
                onValueChange = { onAction(ToolSettingsUiAction.ChangeApiKeyParam(it)) },
                label = stringResource(R.string.screen_tools_field_key_query),
                hint = stringResource(R.string.screen_tools_hint_key_query)
            )
        }
    }
}

@Composable
private fun ToolProviderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
            .background(Color(if (selected) LineTheme.ACCENT else LineTheme.SURFACE_LIGHT))
            .border(
                width = 1.dp,
                color = Color(if (selected) LineTheme.ACCENT else LineTheme.BORDER_LIGHT),
                shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(if (selected) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ToolSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    hidden: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        label = { Text(label) },
        placeholder = { Text(hint) },
        singleLine = true,
        visualTransformation = if (hidden) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (hidden) KeyboardType.Password else KeyboardType.Uri
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

private data class ToolProviderItem(
    val provider: String,
    val labelRes: Int
)

private val PROVIDERS = listOf(
    ToolProviderItem(WebSearchConfig.PROVIDER_BING_RSS_FREE, R.string.screen_tools_provider_bing_rss_free),
    ToolProviderItem(WebSearchConfig.PROVIDER_TAVILY, R.string.screen_tools_provider_tavily),
    ToolProviderItem(WebSearchConfig.PROVIDER_BRAVE, R.string.screen_tools_provider_brave),
    ToolProviderItem(WebSearchConfig.PROVIDER_SERPAPI, R.string.screen_tools_provider_serpapi),
    ToolProviderItem(WebSearchConfig.PROVIDER_BING, R.string.screen_tools_provider_bing),
    ToolProviderItem(WebSearchConfig.PROVIDER_CUSTOM, R.string.screen_tools_provider_custom)
)
