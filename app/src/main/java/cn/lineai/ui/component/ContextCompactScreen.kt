package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.ui.model.ContextCompactStatus
import cn.lineai.ui.model.ContextCompactUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ContextCompactContent(state: ContextCompactUiState) {
    val error = state.status == ContextCompactStatus.ERROR
    val contentColor = Color(if (error) LineTheme.DANGER else LineTheme.TEXT_TERTIARY)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
            .background(Color(LineTheme.CODE_BG))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(cn.lineai.ui.theme.R.drawable.ic_lucide_archive),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )
        Text(
            text = state.label,
            modifier = Modifier.padding(start = 6.dp),
            color = contentColor,
            fontSize = LineTheme.FONT_SM.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.weight(1f))
        when (state.status) {
            ContextCompactStatus.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color(LineTheme.ACCENT),
                strokeWidth = 2.dp
            )
            ContextCompactStatus.DONE -> Icon(
                painter = painterResource(cn.lineai.ui.theme.R.drawable.ic_lucide_check),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            ContextCompactStatus.ERROR -> Icon(
                painter = painterResource(cn.lineai.ui.theme.R.drawable.ic_lucide_x),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
        }
    }
}
