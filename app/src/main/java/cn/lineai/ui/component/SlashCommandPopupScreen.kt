package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.ui.model.SlashCommandPopupUiState
import cn.lineai.ui.model.SlashCommandRowUi
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun SlashCommandPopupContent(
    state: SlashCommandPopupUiState,
    onSelect: (Int) -> Unit
) {
    if (!state.visible) return

    val shape = RoundedCornerShape(LineTheme.SHAPE_MD.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(LineTheme.INPUT_BG))
            .border(
                1.dp,
                Color(LineTheme.BORDER_LIGHT),
                shape
            )
            .padding(3.dp)
    ) {
        if (state.title.isNotEmpty()) {
            Text(
                text = state.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .padding(
                        start = LineTheme.MD.dp,
                        end = LineTheme.MD.dp,
                        top = 1.dp
                    ),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_SM.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        state.rows.forEach { row ->
            SlashCommandRow(
                row = row,
                onClick = { onSelect(row.index) }
            )
        }
    }
}

@Composable
private fun SlashCommandRow(
    row: SlashCommandRowUi,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    LineTheme.SHAPE_SM.dp
                )
            )
            .clickable(
                interactionSource =
                    remember { MutableInteractionSource() },
                indication = ripple(
                    color = Color(LineTheme.ACCENT)
                ),
                role = Role.Button,
                onClick = onClick
            )
            .padding(
                horizontal = LineTheme.MD.dp,
                vertical = LineTheme.SM.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedLabel(row.label),
                modifier = Modifier.weight(1f),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_SM.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (row.selected) {
                Spacer(Modifier.width(LineTheme.SM.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            Color(LineTheme.ACCENT),
                            RoundedCornerShape(
                                LineTheme.SHAPE_XS.dp
                            )
                        )
                )
            }
        }
        if (row.description.isNotEmpty()) {
            Text(
                text = row.description,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formattedLabel(value: String): AnnotatedString {
    if (!value.startsWith("/")) return AnnotatedString(value)
    var end = 1
    while (
        end < value.length &&
        value[end] != ' ' &&
        value[end] != '\t'
    ) {
        end++
    }
    return buildAnnotatedString {
        pushStyle(
            SpanStyle(
                color = Color(LineTheme.ACCENT),
                fontWeight = FontWeight.Bold
            )
        )
        append(value.substring(0, end))
        pop()
        append(value.substring(end))
    }
}
