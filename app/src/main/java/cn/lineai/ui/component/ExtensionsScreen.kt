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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.ExtensionsItemKind
import cn.lineai.ui.model.ExtensionsUiAction
import cn.lineai.ui.model.ExtensionsUiItem
import cn.lineai.ui.model.ExtensionsUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

internal data class ExtensionCardVisualSpec(
    val titleRes: Int,
    val descriptionRes: Int,
    val badgeRes: Int,
    val iconType: Int
)

internal fun extensionCardVisualSpec(kind: ExtensionsItemKind): ExtensionCardVisualSpec = when (kind) {
    ExtensionsItemKind.AGENT -> ExtensionCardVisualSpec(
        titleRes = R.string.screen_extensions_section_agent,
        descriptionRes = R.string.screen_extensions_desc_agent,
        badgeRes = R.string.screen_extensions_badge_can_add,
        iconType = IconButtonView.BRAIN
    )

    ExtensionsItemKind.MCP -> ExtensionCardVisualSpec(
        titleRes = R.string.screen_extensions_section_mcp,
        descriptionRes = R.string.screen_extensions_desc_mcp,
        badgeRes = R.string.screen_extensions_badge_https,
        iconType = IconButtonView.MCP
    )

    ExtensionsItemKind.SKILLS -> ExtensionCardVisualSpec(
        titleRes = R.string.screen_extensions_section_skills,
        descriptionRes = R.string.screen_extensions_desc_skills,
        badgeRes = R.string.screen_extensions_badge_zip,
        iconType = IconButtonView.ARCHIVE
    )

    ExtensionsItemKind.LINECODE -> ExtensionCardVisualSpec(
        titleRes = R.string.screen_extensions_section_linecode,
        descriptionRes = R.string.screen_extensions_desc_linecode,
        badgeRes = R.string.screen_extensions_badge_lip,
        iconType = IconButtonView.PACKAGE
    )

    ExtensionsItemKind.TERMINAL_PROVIDER -> ExtensionCardVisualSpec(
        titleRes = R.string.screen_extensions_section_terminal_provider,
        descriptionRes = R.string.screen_extensions_desc_terminal_provider,
        badgeRes = R.string.screen_extensions_badge_terminal_provider,
        iconType = IconButtonView.TERMINAL
    )
}

@Composable
internal fun ExtensionsScreenContent(
    state: ExtensionsUiState,
    onAction: (ExtensionsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_extensions_title,
            onBack = { onAction(ExtensionsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 84.dp)
        ) {
            state.items.forEach { item ->
                ExtensionMenuCard(
                    item = item,
                    onClick = { onAction(ExtensionsUiAction.Open(item.destination)) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ExtensionMenuCard(
    item: ExtensionsUiItem,
    onClick: () -> Unit
) {
    val spec = extensionCardVisualSpec(item.kind)
    val shape = RoundedCornerShape(LineTheme.SHAPE_MD.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .border(1.dp, Color(LineTheme.BORDER), shape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
                .background(Color(LineTheme.ACCENT_MUTED)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.size(44.dp),
                factory = { context ->
                    IconButtonView(context, spec.iconType).apply {
                        setIconColor(LineTheme.ACCENT)
                        setIconSizeDp(44, 22)
                        isClickable = false
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(spec.titleRes),
                    modifier = Modifier.weight(1f),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_LG.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(spec.badgeRes),
                    modifier = Modifier
                        .clip(RoundedCornerShape(LineTheme.SHAPE_FULL.dp))
                        .background(Color(LineTheme.ACCENT_MUTED))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = Color(LineTheme.ACCENT),
                    fontSize = LineTheme.FONT_XS.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(spec.descriptionRes),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_SM.sp,
                lineHeight = (LineTheme.FONT_SM + 3).sp
            )
        }

        AndroidView(
            modifier = Modifier.size(20.dp),
            factory = { context ->
                IconButtonView(context, IconButtonView.CHEVRON_RIGHT).apply {
                    setIconColor(LineTheme.TEXT_TERTIARY)
                    setIconSizeDp(20, 17)
                    isClickable = false
                }
            }
        )
    }
}
