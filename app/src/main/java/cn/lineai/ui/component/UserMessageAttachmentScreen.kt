package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.ui.model.UserMessageAttachmentUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun UserMessageAttachmentContent(
    state: UserMessageAttachmentUiState
) {
    if (!state.visible) return

    val shape = RoundedCornerShape(LineTheme.SHAPE_FULL.dp)
    Column(horizontalAlignment = Alignment.End) {
        state.items.forEach { item ->
            Text(
                text = item.name,
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .clip(shape)
                    .background(Color(LineTheme.SURFACE_LIGHT))
                    .border(1.dp, Color(LineTheme.BORDER_LIGHT), shape)
                    .padding(horizontal = LineTheme.SM.dp, vertical = 4.dp),
                color = Color(LineTheme.TEXT_SECONDARY),
                fontSize = LineTheme.chatSp(
                    LineTheme.TYPE_BODY_SMALL.toFloat()
                ).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
            Spacer(Modifier.height(LineTheme.XS.dp))
        }
    }
}
