package cn.lineai.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cn.lineai.R
import cn.lineai.ui.model.MessageActionBarUiAction
import cn.lineai.ui.model.MessageActionBarUiState
import cn.lineai.ui.theme.LineTheme

private data class MessageActionSpec(
    val action: MessageActionBarUiAction,
    @DrawableRes val icon: Int,
    @StringRes val description: Int
)

@Composable
internal fun MessageActionBarContent(
    state: MessageActionBarUiState,
    onAction: (MessageActionBarUiAction) -> Unit
) {
    val touchSize = (32f * LineTheme.CHAT_DENSITY_SCALE).dp
    val gap = (8f * LineTheme.CHAT_DENSITY_SCALE).dp
    val items = buildList {
        if (state.copyVisible) {
            add(MessageActionSpec(
                MessageActionBarUiAction.Copy,
                cn.lineai.ui.theme.R.drawable.ic_lucide_copy,
                R.string.message_action_copy_desc
            ))
        }
        if (state.secondaryVisible) {
            add(MessageActionSpec(
                MessageActionBarUiAction.Quote,
                cn.lineai.ui.theme.R.drawable.ic_lucide_quote,
                R.string.message_action_quote_desc
            ))
            add(MessageActionSpec(
                MessageActionBarUiAction.Share,
                cn.lineai.ui.theme.R.drawable.ic_lucide_share_2,
                R.string.message_action_share_desc
            ))
            add(MessageActionSpec(
                MessageActionBarUiAction.Select,
                cn.lineai.ui.theme.R.drawable.ic_lucide_text_cursor,
                R.string.message_action_select_desc
            ))
            add(MessageActionSpec(
                MessageActionBarUiAction.MultiSelect,
                cn.lineai.ui.theme.R.drawable.ic_lucide_check_square,
                R.string.message_action_multi_select_desc
            ))
            if (state.recallEnabled) {
                add(MessageActionSpec(
                    MessageActionBarUiAction.Recall,
                    cn.lineai.ui.theme.R.drawable.ic_lucide_rotate_ccw,
                    R.string.message_action_recall_desc
                ))
            }
        }
        if (state.moreVisible) {
            add(MessageActionSpec(
                MessageActionBarUiAction.More,
                cn.lineai.ui.theme.R.drawable.ic_lucide_ellipsis_vertical,
                R.string.message_action_more_desc
            ))
        }
    }

    Row(
        modifier = Modifier.heightIn(min = touchSize),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) Spacer(Modifier.width(gap))
            MessageActionButton(
                spec = item,
                size = touchSize,
                onClick = { onAction(item.action) }
            )
        }
    }
}

@Composable
private fun MessageActionButton(
    spec: MessageActionSpec,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color(LineTheme.ACCENT)),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(spec.icon),
            contentDescription = stringResource(spec.description),
            modifier = Modifier.size(16.dp),
            tint = Color(LineTheme.TEXT_SECONDARY)
        )
    }
}
