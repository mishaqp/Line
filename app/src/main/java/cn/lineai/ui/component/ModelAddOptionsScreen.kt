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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.model.ModelProtocolType
import cn.lineai.ui.model.ModelAddPresetUi
import cn.lineai.ui.model.ModelManagementUiAction
import cn.lineai.ui.model.ModelManagementUiState
import cn.lineai.ui.theme.LineTheme
import cn.lineai.ui.util.ModelProviderPresetStrings

@Composable
internal fun ModelAddOptionsScreenContent(
    state: ModelManagementUiState,
    onAction: (ModelManagementUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onAction(ModelManagementUiAction.Back) },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = Color(LineTheme.TEXT), fontSize = 22.sp)
            }
            Text(
                text = stringResource(R.string.screen_model_add_options_title),
                modifier = Modifier.weight(1f),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(36.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(LineTheme.BORDER))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            LargeAddCard(
                icon = "☰",
                title = stringResource(R.string.screen_model_add_options_custom),
                description = stringResource(R.string.screen_model_add_options_custom_desc),
                onClick = { onAction(ModelManagementUiAction.AddCustom) }
            )
            Spacer(Modifier.height(8.dp))
            LargeAddCard(
                icon = "↑",
                title = stringResource(R.string.screen_model_add_options_local),
                description = stringResource(R.string.screen_model_add_options_local_desc),
                onClick = { onAction(ModelManagementUiAction.AddLocal) }
            )
            Row(
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▣", color = Color(LineTheme.TEXT_SECONDARY), fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.screen_model_add_options_section_presets),
                    color = Color(LineTheme.TEXT_SECONDARY),
                    fontSize = LineTheme.FONT_SM.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            state.addPresets.forEach { preset ->
                PresetRow(preset = preset, onClick = {
                    onAction(ModelManagementUiAction.AddPreset(preset.id))
                })
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(84.dp))
        }
    }
}

@Composable
private fun LargeAddCard(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .border(1.dp, Color(LineTheme.BORDER_LIGHT), RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                .background(Color(LineTheme.ACCENT_MUTED)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = Color(LineTheme.ACCENT), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        Text("›", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 20.sp)
    }
}

@Composable
private fun PresetRow(
    preset: ModelAddPresetUi,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val name = ModelProviderPresetStrings.getLabel(context, preset.id)
    val desc = ModelProviderPresetStrings.getDesc(context, preset.id)
    val protocol = protocolLabel(preset.protocolType)
    val initial = name.take(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .border(1.dp, Color(LineTheme.BORDER_LIGHT), RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                .background(Color(LineTheme.ACCENT_MUTED)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color(LineTheme.ACCENT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = name,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$desc · $protocol",
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("›", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 18.sp)
    }
}

@Composable
private fun protocolLabel(type: ModelProtocolType): String = when (type) {
    ModelProtocolType.CODEX_RESPONSES -> "Codex"
    ModelProtocolType.ANTHROPIC_MESSAGES -> "Anthropic"
    ModelProtocolType.LOCAL_GGUF -> stringResource(R.string.model_provider_local_gguf)
    ModelProtocolType.OPENAI_COMPATIBLE,
    ModelProtocolType.GROK_RESPONSES -> stringResource(R.string.model_provider_openai_compatible)
}
