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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.AdvancedFeatureKind
import cn.lineai.ui.model.AdvancedFeatureUiItem
import cn.lineai.ui.model.AdvancedFeaturesUiAction
import cn.lineai.ui.model.AdvancedFeaturesUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun AdvancedFeaturesScreenContent(
    state: AdvancedFeaturesUiState,
    onAction: (AdvancedFeaturesUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_advanced_title,
            onBack = { onAction(AdvancedFeaturesUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 84.dp)
        ) {
            state.features.forEach { feature ->
                AdvancedFeatureCard(
                    feature = feature,
                    onClick = { onAction(AdvancedFeaturesUiAction.Open(feature.destination)) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AdvancedFeatureCard(
    feature: AdvancedFeatureUiItem,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(LineTheme.SHAPE_MD.dp)
    val titleRes: Int
    val descriptionRes: Int
    val badgeRes: Int
    val iconType: Int
    when (feature.kind) {
        AdvancedFeatureKind.PHONE_CONTROL -> {
            titleRes = R.string.screen_advanced_phone_control_title
            descriptionRes = R.string.screen_advanced_phone_control_desc
            badgeRes = R.string.screen_advanced_phone_control_badge
            iconType = IconButtonView.SMARTPHONE
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .border(1.dp, Color(LineTheme.BORDER), shape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
                .background(Color(LineTheme.ACCENT_MUTED)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.size(44.dp),
                factory = { context ->
                    IconButtonView(context, iconType).apply {
                        setIconColor(LineTheme.ACCENT)
                        setIconSizeDp(44, 22)
                        isClickable = false
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(titleRes),
                    modifier = Modifier.weight(1f),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_LG.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(badgeRes),
                    modifier = Modifier
                        .clip(RoundedCornerShape(LineTheme.SHAPE_FULL.dp))
                        .background(Color(LineTheme.ACCENT_MUTED))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = Color(LineTheme.ACCENT),
                    fontSize = LineTheme.FONT_XS.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(descriptionRes),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_SM.sp,
                lineHeight = (LineTheme.FONT_SM + 3).sp
            )
        }

        AndroidView(
            modifier = Modifier.size(20.dp),
            factory = { context ->
                IconButtonView(context, IconButtonView.CHEVRON_RIGHT).apply {
                    setIconColor(LineTheme.TEXT_TERTIARY)
                    setIconSizeDp(20, 17)
                    isClickable = false
                }
            }
        )
    }
}
