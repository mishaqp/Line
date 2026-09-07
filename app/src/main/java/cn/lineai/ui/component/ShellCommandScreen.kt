package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.ShellCommandUiAction
import cn.lineai.ui.model.ShellCommandUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ShellCommandScreenContent(
    state: ShellCommandUiState,
    onAction: (ShellCommandUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        ShellCommandHeader(onBack = { onAction(ShellCommandUiAction.Back) })
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(LineTheme.LG.dp)
        ) {
            val body = if (state.hasCommand) {
                state.command
            } else {
                stringResource(R.string.shell_command_empty)
            }
            SelectionContainer {
                Text(
                    text = body,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
                        .background(Color(LineTheme.CODE_BG))
                        .border(
                            width = 1.dp,
                            color = Color(LineTheme.CODE_BORDER),
                            shape = RoundedCornerShape(LineTheme.SHAPE_MD.dp)
                        )
                        .padding(LineTheme.MD.dp),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_SM.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = (LineTheme.FONT_SM + 4).sp
                )
            }
        }
    }
}

@Composable
private fun ShellCommandHeader(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(LineTheme.SURFACE_ELEVATED))
                .padding(LineTheme.MD.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .width(68.dp)
                    .clickable(onClick = onBack),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AndroidView(
                    modifier = Modifier.size(22.dp),
                    factory = { context ->
                        IconButtonView(context, IconButtonView.CHEVRON_LEFT).apply {
                            setIconColor(LineTheme.TEXT)
                            setIconSizeDp(22, 22)
                            isClickable = false
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.in_app_browser_exit),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_MD.sp
                )
            }
            Text(
                text = stringResource(R.string.shell_command_title),
                modifier = Modifier.weight(1f),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(68.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(LineTheme.BORDER_LIGHT))
        )
    }
}
