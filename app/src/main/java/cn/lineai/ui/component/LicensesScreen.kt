package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.ui.model.LicenseUiItem
import cn.lineai.ui.model.LicensesUiAction
import cn.lineai.ui.model.LicensesUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun LicensesScreenContent(
    state: LicensesUiState,
    onAction: (LicensesUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_licenses_title,
            onBack = { onAction(LicensesUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 84.dp)
        ) {
            state.licenses.forEach { license ->
                LicenseCard(license)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LicenseCard(license: LicenseUiItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(license.titleResId),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_MD.sp
        )
        Text(
            text = license.meta,
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_SM.sp
        )
        Text(
            text = stringResource(license.descriptionResId),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            lineHeight = (LineTheme.FONT_XS + 3).sp
        )
    }
}
