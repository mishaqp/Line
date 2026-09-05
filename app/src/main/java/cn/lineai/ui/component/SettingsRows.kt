package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import cn.lineai.ui.theme.LineTheme
import java.util.Locale

@Composable
internal fun SettingsScreenHeader(
    titleRes: Int,
    onBack: () -> Unit
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
            Text("\u2039", color = Color(LineTheme.TEXT), fontSize = 22.sp)
        }
        Text(
            text = stringResource(titleRes),
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_LG.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.size(36.dp))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(LineTheme.BORDER))
    )
}

@Composable
internal fun SettingsGroup(
    titleRes: Int,
    content: @Composable () -> Unit
) {
    Text(
        text = stringResource(titleRes).uppercase(Locale.ROOT),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
    ) {
        content()
    }
}

@Composable
internal fun SettingsGroupDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 48.dp)
            .height(1.dp)
            .background(Color(LineTheme.BORDER_LIGHT))
    )
}

@Composable
internal fun SettingsChoiceRow(
    glyph: String,
    titleRes: Int,
    descRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(if (selected) LineTheme.ACCENT_MUTED else 0x00000000))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = glyph,
            color = Color(if (selected) LineTheme.ACCENT else LineTheme.TEXT_SECONDARY),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        SettingsRowCopy(
            titleRes = titleRes,
            descRes = descRes,
            titleColor = if (selected) LineTheme.ACCENT else LineTheme.TEXT,
            titleWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
internal fun SettingsToggleRow(
    glyph: String,
    titleRes: Int,
    descRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = glyph,
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        SettingsRowCopy(
            titleRes = titleRes,
            descRes = descRes,
            titleColor = LineTheme.TEXT,
            titleWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(LineTheme.ACCENT),
                checkedTrackColor = Color(LineTheme.ACCENT_DIM),
                uncheckedThumbColor = Color(LineTheme.TEXT_TERTIARY),
                uncheckedTrackColor = Color(LineTheme.SURFACE_LIGHT)
            )
        )
    }
}

@Composable
internal fun SettingsNavRow(
    glyph: String,
    titleRes: Int,
    descRes: Int,
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
            text = glyph,
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        SettingsRowCopy(
            titleRes = titleRes,
            descRes = descRes,
            titleColor = LineTheme.TEXT,
            titleWeight = FontWeight.Medium
        )
        Text("\u203a", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 18.sp)
        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun RowScope.SettingsRowCopy(
    titleRes: Int,
    descRes: Int,
    titleColor: Int,
    titleWeight: FontWeight
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(start = 12.dp, end = 8.dp)
    ) {
        Text(
            text = stringResource(titleRes),
            color = Color(titleColor),
            fontSize = LineTheme.FONT_MD.sp,
            fontWeight = titleWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(descRes),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            lineHeight = (LineTheme.FONT_XS + 3).sp
        )
    }
}
