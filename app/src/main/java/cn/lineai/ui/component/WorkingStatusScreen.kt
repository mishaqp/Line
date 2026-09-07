package cn.lineai.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.ui.model.WorkingStatusUiState
import cn.lineai.ui.theme.LineTheme

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun WorkingStatusContent(state: WorkingStatusUiState) {
    val transition = rememberInfiniteTransition(label = "working-status")
    val animatedProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "working-status-progress"
    )
    val progress = if (state.working) animatedProgress else 0f
    val label = state.displayLabel
    val textMeasurer = rememberTextMeasurer()
    val style = TextStyle(
        color = Color(LineTheme.TEXT_SECONDARY),
        fontSize = LineTheme.FONT_SM.sp,
        fontFamily = FontFamily.Monospace
    )
    val measured = textMeasurer.measure(
        text = label,
        style = style,
        constraints = Constraints(maxWidth = Int.MAX_VALUE)
    )
    val matrixSize = 16.dp
    val gap = 8.dp
    val horizontalPadding = 1.dp
    val desiredWidth = horizontalPadding * 2 + matrixSize + gap +
        with(androidx.compose.ui.platform.LocalDensity.current) { measured.size.width.toDp() }

    Row(
        modifier = Modifier
            .widthIn(max = desiredWidth)
            .heightIn(min = 24.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .size(matrixSize)
        ) {
            val alphas = WorkingStatusMath.dotAlphas(progress)
            val step = size.width / 3f
            val radius = 1.35.dp.toPx()
            for (row in 0 until 3) {
                for (column in 0 until 3) {
                    val offset = row * 3 + column
                    drawCircle(
                        color = Color(LineTheme.ACCENT).copy(alpha = alphas[offset]),
                        radius = radius,
                        center = Offset(step * (column + 0.5f), step * (row + 0.5f))
                    )
                }
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(gap))
        Canvas(
            modifier = Modifier.size(
                width = with(androidx.compose.ui.platform.LocalDensity.current) {
                    measured.size.width.toDp()
                },
                height = 24.dp
            )
        ) {
            if (label.isEmpty()) return@Canvas
            val shimmerWidth = 72.dp.toPx()
            val center = -shimmerWidth + progress * (size.width + shimmerWidth * 2f)
            val brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(LineTheme.TEXT_SECONDARY),
                    Color(WorkingStatusMath.highlightColor(LineTheme.TEXT_SECONDARY)),
                    Color(LineTheme.TEXT_SECONDARY)
                ),
                startX = center - shimmerWidth,
                endX = center + shimmerWidth
            )
            val result = textMeasurer.measure(label, style.copy(brush = brush))
            drawText(
                textLayoutResult = result,
                topLeft = Offset(0f, (size.height - result.size.height) / 2f)
            )
        }
    }
}
