package cn.lineai.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.ui.model.SettingsIcon
import cn.lineai.ui.model.SettingsItemUi
import cn.lineai.ui.model.SettingsSectionUi
import cn.lineai.ui.model.SettingsUiAction
import cn.lineai.ui.model.SettingsUiState
import cn.lineai.ui.theme.LineTheme
import java.util.Locale

@Composable
internal fun SettingsScreenContent(
    state: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsHeader(onBack = { onAction(SettingsUiAction.Back) })
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            state.sections.forEach { section ->
                SettingsSection(section, onAction)
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(LineTheme.BG))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2039", color = Color(LineTheme.TEXT), fontSize = 22.sp)
        }
        Text(
            text = stringResource(R.string.screen_settings_title),
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
}

@Composable
private fun SettingsSection(
    section: SettingsSectionUi,
    onAction: (SettingsUiAction) -> Unit
) {
    Text(
        text = stringResource(section.titleRes).uppercase(Locale.ROOT),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
    ) {
        section.items.forEachIndexed { index, item ->
            SettingsRow(item, onAction)
            if (index < section.items.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 68.dp)
                        .height(1.dp)
                        .background(Color(LineTheme.BORDER_LIGHT))
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    item: SettingsItemUi,
    onAction: (SettingsUiAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction(SettingsUiAction.Open(item.destination)) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_LG.dp))
                .background(Color(LineTheme.ACCENT_MUTED)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyph(item.icon),
                color = Color(LineTheme.ACCENT),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = stringResource(item.titleRes),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(item.descRes),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        Text("\u203a", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 18.sp)
        Spacer(Modifier.width(2.dp))
    }
}

private fun glyph(icon: SettingsIcon): String = when (icon) {
    SettingsIcon.BOX -> "\u25a3"
    SettingsIcon.USER -> "\u263a"
    SettingsIcon.BRAIN -> "\u25c9"
    SettingsIcon.MCP -> "\u2b21"
    SettingsIcon.SLIDERS -> "\u2630"
    SettingsIcon.PACKAGE -> "\u25a2"
    SettingsIcon.ZAP -> "\u26a1"
    SettingsIcon.MESSAGE -> "\u270e"
    SettingsIcon.PALETTE -> "\u25d0"
    SettingsIcon.MONITOR -> "\u25af"
    SettingsIcon.SHIELD -> "\u26e8"
    SettingsIcon.DATABASE -> "\u26c1"
    SettingsIcon.BOOK -> "\u2630"
    SettingsIcon.ARCHIVE -> "\u25ad"
    SettingsIcon.BUG -> "\u2731"
    SettingsIcon.BATTERY -> "\u26a1"
    SettingsIcon.CPU -> "\u2302"
}
