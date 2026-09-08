package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.ui.model.ComposerPendingQueueUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ComposerPendingQueueContent(
    state: ComposerPendingQueueUiState,
    onRemove: (Int) -> Unit
) {
    if (!state.visible) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 4.dp, end = 8.dp)
    ) {
        state.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                    .background(
                        Color(LineTheme.WARNING).copy(
                            alpha = LineTheme.STATE_LAYER_ALPHA_HOVER
                        )
                    )
                    .padding(start = 8.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 20.dp)
                        .clip(RoundedCornerShape(LineTheme.SHAPE_XS.dp))
                        .background(Color(LineTheme.WARNING))
                )
                Text(
                    text = item.preview,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    color = Color(LineTheme.WARNING),
                    fontSize = LineTheme.FONT_SM.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = { onRemove(item.index) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        painter = painterResource(cn.lineai.ui.theme.R.drawable.ic_lucide_x),
                        contentDescription = stringResource(
                            R.string.composer_queue_remove_desc
                        ),
                        modifier = Modifier.size(12.dp),
                        tint = Color(LineTheme.TEXT_TERTIARY)
                    )
                }
            }
        }
        if (state.overflowCount > 0) {
            Text(
                text = stringResource(
                    R.string.composer_queue_overflow,
                    state.overflowCount
                ),
                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp),
                color = Color(LineTheme.WARNING).copy(alpha = 0.8f),
                fontSize = LineTheme.FONT_SM.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}
