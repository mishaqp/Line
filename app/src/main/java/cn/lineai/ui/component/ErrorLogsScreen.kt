package cn.lineai.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.ui.model.ErrorLogItem
import cn.lineai.ui.model.ErrorLogsUiAction
import cn.lineai.ui.model.ErrorLogsUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ErrorLogsScreenContent(
    state: ErrorLogsUiState,
    onAction: (ErrorLogsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        ErrorLogsHeader(
            onBack = { onAction(ErrorLogsUiAction.Back) },
            onClear = { onAction(ErrorLogsUiAction.Clear) }
        )
        if (state.logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = stringResource(R.string.screen_error_logs_empty),
                    color = Color(LineTheme.TEXT_TERTIARY),
                    fontSize = LineTheme.FONT_MD.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                SettingsNamedGroup(stringResource(R.string.screen_error_logs_section_title)) {
                    state.logs.forEachIndexed { index, item ->
                        ErrorLogRow(
                            item = item,
                            onClick = {
                                onAction(ErrorLogsUiAction.Open(item))
                            }
                        )
                        if (index < state.logs.lastIndex) SettingsGroupDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorLogsHeader(
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(LineTheme.BG))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", color = Color(LineTheme.TEXT), fontSize = 22.sp)
        }
        Text(
            text = stringResource(R.string.screen_error_logs_title),
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_LG.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_LG.dp))
                .clickable(onClick = onClear),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×",
                color = Color(LineTheme.DANGER),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(LineTheme.BORDER))
    )
}

@Composable
private fun ErrorLogRow(
    item: ErrorLogItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "≡",
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = item.title,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("›", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 18.sp)
    }
}
