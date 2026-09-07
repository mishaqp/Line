package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.ui.model.MessageHeaderUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun MessageHeaderContent(state: MessageHeaderUiState) {
    val avatarSize = (28f * LineTheme.CHAT_DENSITY_SCALE).dp
    val gap = (8f * LineTheme.CHAT_DENSITY_SCALE).dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (state.outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.outgoing) {
            HeaderLabel(state.name)
            Spacer(Modifier.width(gap))
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(Color(LineTheme.ACCENT_MUTED), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        cn.lineai.ui.theme.R.drawable.ic_lucide_user
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = Color(LineTheme.ACCENT)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(Color(LineTheme.ACCENT_MUTED), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.monogram,
                    color = Color(LineTheme.ACCENT),
                    fontSize = LineTheme.chatSp(LineTheme.TYPE_BODY_SMALL.toFloat()).sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(gap))
            HeaderLabel(state.name)
        }
    }
}

@Composable
private fun HeaderLabel(name: String) {
    Text(
        text = name,
        color = Color(LineTheme.TEXT_SECONDARY),
        fontSize = LineTheme.chatSp(LineTheme.TYPE_LABEL.toFloat()).sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
