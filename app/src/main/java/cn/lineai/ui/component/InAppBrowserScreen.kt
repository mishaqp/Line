package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.InAppBrowserUiAction
import cn.lineai.ui.model.InAppBrowserUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun InAppBrowserScreenContent(
    state: InAppBrowserUiState,
    onAction: (InAppBrowserUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        InAppBrowserHeader(
            state = state,
            onBack = { onAction(InAppBrowserUiAction.Back) }
        )
        val initial = remember(state.normalizedUrl, state.supported, state.javaScriptEnabled) { state }
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .fillMaxHeight(),
            factory = { context -> InAppBrowserWebViews.create(context, initial) }
        )
    }
}

@Composable
private fun InAppBrowserHeader(
    state: InAppBrowserUiState,
    onBack: () -> Unit
) {
    val title = if (state.useDefaultTitle) {
        stringResource(R.string.in_app_browser_default_title)
    } else {
        state.headerUrl
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(LineTheme.SURFACE_ELEVATED))
                .padding(LineTheme.MD.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .width(56.dp)
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
                text = title,
                modifier = Modifier.weight(1f),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(56.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(LineTheme.BORDER))
        )
    }
}
