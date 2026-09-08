package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.ui.model.UserMessageContentUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun UserMessageContent(
    state: UserMessageContentUiState
) {
    if (!state.visible) return

    val densityScale = LineTheme.CHAT_DENSITY_SCALE
    val shape = RoundedCornerShape(LineTheme.SHAPE_LG.dp)
    Text(
        text = state.content,
        modifier = Modifier
            .widthIn(max = state.maxWidthDp.dp)
            .clip(shape)
            .background(Color(LineTheme.USER_BUBBLE))
            .padding(
                horizontal = (LineTheme.MD * densityScale).dp,
                vertical = (LineTheme.SM * densityScale).dp
            ),
        color = Color(LineTheme.TEXT_ON_COLOR),
        fontSize = LineTheme.chatSp(
            LineTheme.TYPE_TITLE.toFloat()
        ).sp,
        fontWeight = FontWeight.Normal
    )
}
