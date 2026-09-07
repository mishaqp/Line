package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.model.ChatMode
import cn.lineai.ui.model.HeaderUiAction
import cn.lineai.ui.model.HeaderUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun HeaderScreenContent(
    state: HeaderUiState,
    onAction: (HeaderUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(LineTheme.BG))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                iconRes = cn.lineai.ui.theme.R.drawable.ic_lucide_menu,
                descriptionRes = R.string.header_menu_desc,
                tint = LineTheme.TEXT,
                onClick = { onAction(HeaderUiAction.Menu) }
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 6.dp)
                    .clickable { onAction(HeaderUiAction.Project) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(LineTheme.SHAPE_XS.dp))
                        .background(Color(LineTheme.ACCENT))
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                ) {
                    Text(
                        text = state.projectLabel,
                        color = Color(LineTheme.TEXT),
                        fontSize = LineTheme.FONT_MD.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.executionTargetLabel.isNotEmpty()) {
                        Text(
                            text = state.executionTargetLabel,
                            color = Color(LineTheme.TEXT_SECONDARY),
                            fontSize = LineTheme.FONT_XS.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    painter = painterResource(cn.lineai.ui.theme.R.drawable.ic_lucide_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(width = 20.dp, height = 14.dp),
                    tint = Color(LineTheme.TEXT_SECONDARY)
                )
            }
            Box {
                Row(
                    modifier = Modifier
                        .heightIn(min = 28.dp)
                        .clip(RoundedCornerShape(LineTheme.SHAPE_FULL.dp))
                        .background(Color(LineTheme.SURFACE_ELEVATED))
                        .border(
                            1.dp,
                            Color(LineTheme.BORDER),
                            RoundedCornerShape(LineTheme.SHAPE_FULL.dp)
                        )
                        .clickable { onAction(HeaderUiAction.ToggleModeMenu) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modeLabel(state.chatMode),
                        color = Color(LineTheme.TEXT),
                        fontSize = LineTheme.FONT_XS.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        painter = painterResource(cn.lineai.ui.theme.R.drawable.ic_lucide_chevron_down),
                        contentDescription = null,
                        modifier = Modifier.size(width = 16.dp, height = 12.dp),
                        tint = Color(LineTheme.TEXT_SECONDARY)
                    )
                }
                DropdownMenu(
                    expanded = state.modeMenuVisible,
                    onDismissRequest = { onAction(HeaderUiAction.DismissModeMenu) },
                    modifier = Modifier
                        .width(160.dp)
                        .background(Color(LineTheme.SURFACE_ELEVATED))
                ) {
                    modeOptions().forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = modeLabel(mode),
                                    color = if (mode == state.chatMode) {
                                        Color(LineTheme.TEXT_ON_COLOR)
                                    } else {
                                        Color(LineTheme.TEXT)
                                    },
                                    fontSize = LineTheme.FONT_SM.sp
                                )
                            },
                            modifier = Modifier.background(
                                if (mode == state.chatMode) {
                                    Color(LineTheme.ACCENT)
                                } else {
                                    Color.Transparent
                                }
                            ),
                            onClick = { onAction(HeaderUiAction.SelectMode(mode)) }
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            HeaderIconButton(
                cn.lineai.ui.theme.R.drawable.ic_lucide_shield,
                R.string.header_permission_desc,
                LineTheme.TEXT_SECONDARY
            ) { onAction(HeaderUiAction.Permission) }
            HeaderIconButton(
                cn.lineai.ui.theme.R.drawable.ic_lucide_plus,
                R.string.header_new_conversation_desc,
                LineTheme.TEXT_SECONDARY
            ) { onAction(HeaderUiAction.NewConversation) }
            HeaderIconButton(
                cn.lineai.ui.theme.R.drawable.ic_lucide_ellipsis_vertical,
                R.string.header_more_desc,
                LineTheme.TEXT_SECONDARY
            ) { onAction(HeaderUiAction.More) }
        }
        HorizontalDivider(thickness = 1.dp, color = Color(LineTheme.BORDER))
    }
}

@Composable
private fun HeaderIconButton(
    iconRes: Int,
    descriptionRes: Int,
    tint: Int,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descriptionRes),
            modifier = Modifier.size(20.dp),
            tint = Color(tint)
        )
    }
}

@Composable
private fun modeLabel(mode: String): String = stringResource(
    when (mode) {
        ChatMode.CHAT -> R.string.header_mode_chat
        ChatMode.PLAN -> R.string.header_mode_plan
        ChatMode.CONTROL -> R.string.header_mode_control
        else -> R.string.header_mode_agent
    }
)

private fun modeOptions(): List<String> =
    listOf(ChatMode.CHAT, ChatMode.PLAN, ChatMode.AGENT, ChatMode.CONTROL)
