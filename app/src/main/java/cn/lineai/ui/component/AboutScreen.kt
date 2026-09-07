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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.AboutUiAction
import cn.lineai.ui.model.AboutUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun AboutScreenContent(
    state: AboutUiState,
    onAction: (AboutUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_about_title,
            onBack = { onAction(AboutUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 84.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(LineTheme.ACCENT_MUTED)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            IconButtonView(context, IconButtonView.CODE).apply {
                                setIconColor(LineTheme.ACCENT)
                                setIconSizeDp(88, 48)
                                isClickable = false
                            }
                        },
                        modifier = Modifier.size(88.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.appLabel,
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_XL.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.screen_about_apk_label,
                        state.versionName,
                        state.versionCode
                    ),
                    color = Color(LineTheme.TEXT_SECONDARY),
                    fontSize = LineTheme.FONT_MD.sp,
                    textAlign = TextAlign.Center
                )
            }

            AboutSectionTitle(R.string.screen_about_section_version)
            AboutRow(
                icon = IconButtonView.PACKAGE,
                label = stringResource(R.string.screen_about_apk_version),
                value = stringResource(
                    R.string.screen_about_version_value,
                    state.versionName,
                    state.versionCode
                )
            )

            AboutSectionTitle(R.string.screen_about_section_developer)
            AboutRow(
                icon = IconButtonView.USER,
                label = stringResource(R.string.screen_about_developer_label),
                value = stringResource(R.string.screen_about_developer_value)
            )
            Spacer(Modifier.height(8.dp))
            AboutRow(
                icon = IconButtonView.MESSAGE_CIRCLE,
                label = stringResource(R.string.screen_about_qq_label),
                value = stringResource(R.string.screen_about_qq_value)
            )
            Spacer(Modifier.height(8.dp))
            AboutRow(
                icon = IconButtonView.GIT_BRANCH,
                label = stringResource(R.string.screen_about_github_label),
                value = stringResource(R.string.screen_about_github_value),
                onClick = { onAction(AboutUiAction.OpenGithub) }
            )

            AboutSectionTitle(R.string.screen_about_section_legal)
            AboutRow(
                icon = IconButtonView.FILE_TEXT,
                label = stringResource(R.string.screen_about_open_source_licenses),
                value = stringResource(R.string.screen_about_legal_value),
                onClick = { onAction(AboutUiAction.OpenLicenses) }
            )

            Text(
                text = stringResource(R.string.screen_about_copyright),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AboutSectionTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_SM.sp
    )
}

@Composable
private fun AboutRow(
    icon: Int,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
        .background(Color(LineTheme.SURFACE_ELEVATED))
        .let { modifier ->
            if (onClick != null) modifier.clickable(onClick = onClick) else modifier
        }
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            factory = { context ->
                IconButtonView(context, icon).apply {
                    setIconColor(LineTheme.TEXT_SECONDARY)
                    setIconSizeDp(28, 18)
                    isClickable = false
                }
            },
            modifier = Modifier.size(28.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = label,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        if (onClick != null) {
            Text(
                text = "\u203a",
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = 18.sp
            )
            Spacer(Modifier.width(2.dp))
        }
    }
}
