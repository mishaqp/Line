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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.ui.model.ImageGenerationModelItemUi
import cn.lineai.ui.model.ImageGenerationModelPickerUiAction
import cn.lineai.ui.model.ImageGenerationModelPickerUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ImageGenerationModelPickerScreenContent(
    state: ImageGenerationModelPickerUiState,
    onAction: (ImageGenerationModelPickerUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_models_pick_image_generation,
            onBack = { onAction(ImageGenerationModelPickerUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 34.dp)
        ) {
            if (state.models.isEmpty()) {
                Text(
                    text = stringResource(R.string.screen_models_empty_readonly),
                    color = Color(LineTheme.TEXT_TERTIARY),
                    fontSize = LineTheme.FONT_SM.sp,
                    lineHeight = (LineTheme.FONT_SM + 3).sp
                )
            } else {
                state.models.forEach { model ->
                    ImageGenerationModelCard(
                        model = model,
                        onClick = {
                            onAction(ImageGenerationModelPickerUiAction.SelectModel(model.internalId))
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ImageGenerationModelCard(
    model: ImageGenerationModelItemUi,
    onClick: () -> Unit
) {
    val border = if (model.selected) Color(LineTheme.ACCENT) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.BG))
            .border(1.dp, border, RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                .background(Color(model.badgeColor))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = model.badgeLabel,
                color = Color(LineTheme.TEXT_ON_COLOR),
                fontSize = LineTheme.FONT_XS.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = model.name,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = model.displayedModelId,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (model.selected) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(LineTheme.SHAPE_XS.dp))
                    .background(Color(LineTheme.ACCENT))
            )
        }
    }
}
