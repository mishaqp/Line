package cn.lineai.ui.component

import android.view.View
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.ToolCallPreviewRowUi
import cn.lineai.ui.model.ToolCallPreviewUiAction
import cn.lineai.ui.model.ToolCallPreviewUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ToolCallPreviewScreenContent(
    state: ToolCallPreviewUiState,
    renderer: ToolCallPreviewCardRenderer,
    onAction: (ToolCallPreviewUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_toolcall_preview_title,
            onBack = { onAction(ToolCallPreviewUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            Text(
                text = stringResource(R.string.toolcall_preview_section_note),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                modifier = Modifier.padding(
                    start = LineTheme.LG.dp,
                    top = LineTheme.XS.dp,
                    end = LineTheme.LG.dp,
                    bottom = 0.dp
                )
            )
            state.rows.forEach { row ->
                key(row.renderId) {
                    ToolCallPreviewRow(row = row, renderer = renderer)
                }
            }
        }
    }
}

@Composable
private fun ToolCallPreviewRow(
    row: ToolCallPreviewRowUi,
    renderer: ToolCallPreviewCardRenderer
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = LineTheme.LG.dp,
                top = LineTheme.MD.dp,
                end = LineTheme.LG.dp,
                bottom = 0.dp
            )
    ) {
        Text(
            text = row.categoryLabel,
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = LineTheme.XS.dp)
        )
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                val card = renderer.createCard(context, row.renderId)
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    if (card != null) {
                        addView(
                            card,
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        )
                    } else {
                        visibility = View.GONE
                    }
                }
            }
        )
    }
}
