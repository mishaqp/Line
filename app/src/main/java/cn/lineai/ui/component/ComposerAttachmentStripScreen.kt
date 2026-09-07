package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.ui.model.ComposerAttachmentUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ComposerAttachmentStripContent(
    state: ComposerAttachmentUiState,
    onRemove: (Int) -> Unit
) {
    if (!state.visible) return

    val chipShape = RoundedCornerShape(LineTheme.SHAPE_FULL.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        state.items.forEach { item ->
            Row(
                modifier = Modifier
                    .padding(end = LineTheme.SM.dp)
                    .height(34.dp)
                    .clip(chipShape)
                    .background(Color(LineTheme.INPUT_BG))
                    .border(1.dp, Color(LineTheme.BORDER), chipShape)
                    .padding(start = LineTheme.MD.dp, end = LineTheme.SM.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    modifier = Modifier.widthIn(max = 170.dp),
                    color = Color(LineTheme.TEXT_SECONDARY),
                    fontSize = LineTheme.TYPE_BODY.sp,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
                IconButton(
                    onClick = { onRemove(item.index) },
                    modifier = Modifier
                        .padding(start = LineTheme.SM.dp)
                        .size(18.dp)
                ) {
                    Icon(
                        painter = painterResource(cn.lineai.ui.theme.R.drawable.ic_lucide_x),
                        contentDescription = stringResource(
                            R.string.composer_attachment_remove_desc
                        ),
                        modifier = Modifier.size(12.dp),
                        tint = Color(LineTheme.TEXT_TERTIARY)
                    )
                }
            }
        }
    }
}
