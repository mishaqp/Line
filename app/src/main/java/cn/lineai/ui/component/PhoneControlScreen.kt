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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.PhoneControlAccessibilityStatus
import cn.lineai.ui.model.PhoneControlPermission
import cn.lineai.ui.model.PhoneControlPermissionUiItem
import cn.lineai.ui.model.PhoneControlUiAction
import cn.lineai.ui.model.PhoneControlUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme
import java.util.Locale

@Composable
internal fun PhoneControlScreenContent(
    state: PhoneControlUiState,
    onAction: (PhoneControlUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_phone_control_title,
            onBack = { onAction(PhoneControlUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 100.dp)
        ) {
            AccessibilityRow(
                status = state.accessibilityStatus,
                onClick = { onAction(PhoneControlUiAction.AccessibilityClicked) }
            )

            if (state.showPermissions) {
                PermissionSectionHeader()
                state.permissions.forEach { item ->
                    PermissionRow(
                        item = item,
                        onCheckedChange = { enabled ->
                            onAction(PhoneControlUiAction.SetPermission(item.permission, enabled))
                        }
                    )
                    if (dividerAfter(item.permission)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(LineTheme.BORDER_LIGHT))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessibilityRow(
    status: PhoneControlAccessibilityStatus,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PhoneControlIcon(
            iconType = IconButtonView.SHIELD_CHECK,
            size = 20,
            glyphSize = 20
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_phone_control_accessibility_label),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(accessibilityDescription(status)),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        PhoneControlIcon(
            iconType = IconButtonView.CHEVRON_RIGHT,
            size = 20,
            glyphSize = 17
        )
    }
}

@Composable
private fun PermissionSectionHeader() {
    Text(
        text = stringResource(R.string.screen_phone_control_permission_management)
            .uppercase(Locale.ROOT),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun PermissionRow(
    item: PhoneControlPermissionUiItem,
    onCheckedChange: (Boolean) -> Unit
) {
    val spec = permissionSpec(item.permission)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PhoneControlIcon(
            iconType = spec.iconType,
            size = 20,
            glyphSize = 20
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = stringResource(spec.titleRes),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(spec.descriptionRes),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        Spacer(Modifier.width(4.dp))
        Switch(
            checked = item.enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PhoneControlIcon(
    iconType: Int,
    size: Int,
    glyphSize: Int
) {
    AndroidView(
        modifier = Modifier.size(size.dp),
        factory = { context ->
            IconButtonView(context, iconType).apply {
                setIconColor(LineTheme.ACCENT)
                setIconSizeDp(size, glyphSize)
                isClickable = false
            }
        }
    )
}

private data class PermissionSpec(
    val titleRes: Int,
    val descriptionRes: Int,
    val iconType: Int
)

private fun permissionSpec(permission: PhoneControlPermission): PermissionSpec = when (permission) {
    PhoneControlPermission.SCREENSHOT -> PermissionSpec(
        R.string.screen_phone_control_permission_screenshot,
        R.string.screen_phone_control_permission_screenshot_desc,
        IconButtonView.ZAP
    )
    PhoneControlPermission.CLICK -> PermissionSpec(
        R.string.screen_phone_control_permission_click,
        R.string.screen_phone_control_permission_click_desc,
        IconButtonView.PLAY
    )
    PhoneControlPermission.SWIPE -> PermissionSpec(
        R.string.screen_phone_control_permission_swipe,
        R.string.screen_phone_control_permission_swipe_desc,
        IconButtonView.SLIDERS_HORIZONTAL
    )
    PhoneControlPermission.LONG_PRESS -> PermissionSpec(
        R.string.screen_phone_control_permission_long_press,
        R.string.screen_phone_control_permission_long_press_desc,
        IconButtonView.CLOCK_3
    )
    PhoneControlPermission.VIEW_HIERARCHY -> PermissionSpec(
        R.string.screen_phone_control_permission_view_hierarchy,
        R.string.screen_phone_control_permission_view_hierarchy_desc,
        IconButtonView.SQUARE_FUNCTION
    )
    PhoneControlPermission.VIEW_ACTION -> PermissionSpec(
        R.string.screen_phone_control_permission_view_action,
        R.string.screen_phone_control_permission_view_action_desc,
        IconButtonView.WRENCH
    )
    PhoneControlPermission.GLOBAL_ACTION -> PermissionSpec(
        R.string.screen_phone_control_permission_global_action,
        R.string.screen_phone_control_permission_global_action_desc,
        IconButtonView.SMARTPHONE
    )
}

private fun dividerAfter(permission: PhoneControlPermission): Boolean = when (permission) {
    PhoneControlPermission.SCREENSHOT,
    PhoneControlPermission.CLICK,
    PhoneControlPermission.SWIPE,
    PhoneControlPermission.LONG_PRESS,
    PhoneControlPermission.VIEW_HIERARCHY -> true
    PhoneControlPermission.VIEW_ACTION,
    PhoneControlPermission.GLOBAL_ACTION -> false
}

private fun accessibilityDescription(status: PhoneControlAccessibilityStatus): Int = when (status) {
    PhoneControlAccessibilityStatus.DISCLAIMER_REQUIRED -> R.string.screen_phone_control_disclaimer_title
    PhoneControlAccessibilityStatus.DISABLED -> R.string.screen_phone_control_accessibility_status_disabled
    PhoneControlAccessibilityStatus.ENABLED -> R.string.screen_phone_control_accessibility_status_enabled
}
