package cn.lineai.ui.component

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.DrawerConversationUi
import cn.lineai.ui.model.DrawerFileColor
import cn.lineai.ui.model.DrawerFileIcon
import cn.lineai.ui.model.DrawerFileUi
import cn.lineai.ui.model.DrawerTab
import cn.lineai.ui.model.DrawerUiAction
import cn.lineai.ui.model.DrawerUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme
import kotlinx.coroutines.delay

private const val DRAWER_OPEN_MS = 180
private const val DRAWER_CLOSE_MS = 150

@Composable
internal fun DrawerScreenContent(
    state: DrawerUiState,
    onAction: (DrawerUiAction) -> Unit,
    onFullyClosed: () -> Unit
) {
    LaunchedEffect(state.isOpen) {
        if (!state.isOpen) {
            delay(DRAWER_CLOSE_MS.toLong())
            onFullyClosed()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        val drawerWidth = if (maxWidth > 48.dp) {
            minOf(maxWidth - 48.dp, 360.dp)
        } else {
            maxWidth
        }
        val animationDuration = if (state.isOpen) DRAWER_OPEN_MS else DRAWER_CLOSE_MS
        val easing = if (state.isOpen) LinearOutSlowInEasing else FastOutLinearInEasing
        val backdropAlpha by animateFloatAsState(
            targetValue = if (state.isOpen) 1f else 0f,
            animationSpec = tween(animationDuration, easing = easing)
        )
        val sidebarOffset by animateDpAsState(
            targetValue = if (state.isOpen) 0.dp else -drawerWidth,
            animationSpec = tween(animationDuration, easing = easing)
        )

        val overlay = Color(LineTheme.OVERLAY)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlay.copy(alpha = overlay.alpha * backdropAlpha))
                .clickable { onAction(DrawerUiAction.Close) }
        )

        Column(
            modifier = Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .offset(x = sidebarOffset)
                .background(Color(LineTheme.SURFACE_ELEVATED))
        ) {
            DrawerHeader(state = state, onAction = onAction)
            DrawerTabs(state.activeTab, onAction)
            when (state.activeTab) {
                DrawerTab.CONVERSATIONS ->
                    DrawerConversations(state = state, onAction = onAction)
                DrawerTab.FILES ->
                    DrawerFiles(state = state, onAction = onAction)
            }
        }
    }

    if (state.removeProjectDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(DrawerUiAction.DismissRemoveProject) },
            containerColor = Color(LineTheme.SURFACE_ELEVATED),
            titleContentColor = Color(LineTheme.TEXT),
            textContentColor = Color(LineTheme.TEXT_SECONDARY),
            title = {
                Text(
                    text = stringResource(R.string.drawer_project_remove_title),
                    fontSize = LineTheme.FONT_LG.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.drawer_project_remove_message,
                        state.projectLabel
                    ),
                    fontSize = LineTheme.FONT_SM.sp
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(DrawerUiAction.DismissRemoveProject) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(LineTheme.TEXT_SECONDARY)
                    )
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(DrawerUiAction.ConfirmRemoveProject) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(LineTheme.DANGER)
                    )
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            }
        )
    }
}

@Composable
private fun DrawerHeader(
    state: DrawerUiState,
    onAction: (DrawerUiAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = LineTheme.LG.dp,
                top = 50.dp,
                end = LineTheme.LG.dp,
                bottom = LineTheme.MD.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                if (state.activeTab == DrawerTab.CONVERSATIONS) {
                    R.string.drawer_title_conversations
                } else {
                    R.string.drawer_title_files
                }
            ),
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_LG.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (state.activeTab == DrawerTab.FILES) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onAction(DrawerUiAction.RefreshFiles) },
                contentAlignment = Alignment.Center
            ) {
                DrawerIcon(
                    type = IconButtonView.REFRESH_CW,
                    color = LineTheme.ACCENT,
                    containerSize = 32.dp,
                    iconSize = 16
                )
            }
        }
    }
}

@Composable
private fun DrawerTabs(
    activeTab: DrawerTab,
    onAction: (DrawerUiAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = LineTheme.LG.dp,
                end = LineTheme.LG.dp,
                bottom = LineTheme.MD.dp
            )
            .background(
                Color(LineTheme.SURFACE_LIGHT),
                RoundedCornerShape(LineTheme.SHAPE_SM.dp)
            )
            .padding(2.dp)
    ) {
        DrawerTabButton(
            modifier = Modifier.weight(1f),
            icon = IconButtonView.MESSAGE_SQUARE,
            text = stringResource(R.string.drawer_tab_conversations),
            active = activeTab == DrawerTab.CONVERSATIONS,
            onClick = {
                onAction(DrawerUiAction.SelectTab(DrawerTab.CONVERSATIONS))
            }
        )
        DrawerTabButton(
            modifier = Modifier.weight(1f),
            icon = IconButtonView.FOLDER_OPEN,
            text = stringResource(R.string.drawer_tab_files),
            active = activeTab == DrawerTab.FILES,
            onClick = {
                onAction(DrawerUiAction.SelectTab(DrawerTab.FILES))
            }
        )
    }
}

@Composable
private fun DrawerTabButton(
    modifier: Modifier,
    icon: Int,
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val color = if (active) LineTheme.ACCENT else LineTheme.TEXT_TERTIARY
    Row(
        modifier = modifier
            .background(
                if (active) Color(LineTheme.SURFACE_ELEVATED) else Color.Transparent,
                RoundedCornerShape(LineTheme.SHAPE_SM.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = LineTheme.SM.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawerIcon(
            type = icon,
            color = color,
            containerSize = 14.dp,
            iconSize = 14
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = Color(color),
            fontSize = LineTheme.FONT_SM.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DrawerConversations(
    state: DrawerUiState,
    onAction: (DrawerUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = LineTheme.LG.dp,
                    end = LineTheme.LG.dp,
                    bottom = LineTheme.MD.dp
                )
                .heightIn(min = 45.dp)
                .background(
                    Color(LineTheme.ACCENT),
                    RoundedCornerShape(LineTheme.SHAPE_MD.dp)
                )
                .clickable { onAction(DrawerUiAction.NewConversation) }
                .padding(horizontal = LineTheme.MD.dp, vertical = LineTheme.SM.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerIcon(
                type = IconButtonView.PLUS,
                color = android.graphics.Color.BLACK,
                containerSize = 18.dp,
                iconSize = 18
            )
            Spacer(Modifier.width(LineTheme.SM.dp))
            Text(
                text = stringResource(R.string.drawer_new_conversation),
                color = Color.Black,
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (state.conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = stringResource(R.string.drawer_empty_conversations),
                    color = Color(LineTheme.TEXT_TERTIARY),
                    fontSize = LineTheme.FONT_SM.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = LineTheme.SM.dp)
            ) {
                itemsIndexed(state.conversations) { _, conversation ->
                    DrawerConversationRow(
                        conversation = conversation,
                        active = conversation.id == state.currentConversationId,
                        onSelect = {
                            onAction(DrawerUiAction.SelectConversation(conversation.id))
                        },
                        onDelete = {
                            onAction(DrawerUiAction.DeleteConversation(conversation.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerConversationRow(
    conversation: DrawerConversationUi,
    active: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (active) Color(LineTheme.ACCENT_MUTED) else Color.Transparent,
                RoundedCornerShape(LineTheme.SHAPE_SM.dp)
            )
            .clickable(onClick = onSelect)
            .padding(LineTheme.MD.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    Color(LineTheme.SURFACE_LIGHT),
                    RoundedCornerShape(LineTheme.SHAPE_MD.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            DrawerIcon(
                type = IconButtonView.MESSAGE_SQUARE,
                color = if (active) LineTheme.ACCENT else LineTheme.TEXT_TERTIARY,
                containerSize = 16.dp,
                iconSize = 16
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = LineTheme.SM.dp)
        ) {
            Text(
                text = conversation.title,
                color = Color(if (active) LineTheme.ACCENT else LineTheme.TEXT),
                fontSize = LineTheme.FONT_SM.sp,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = conversation.time,
                modifier = Modifier.padding(top = 2.dp),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            DrawerIcon(
                type = IconButtonView.TRASH_2,
                color = LineTheme.TEXT_TERTIARY,
                containerSize = 22.dp,
                iconSize = 14
            )
        }
    }
}

@Composable
private fun DrawerFiles(
    state: DrawerUiState,
    onAction: (DrawerUiAction) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val projectGesture = if (state.projectRemovable) {
        Modifier.pointerInput(state.projectLabel, state.projectPath) {
            detectTapGestures(
                onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAction(DrawerUiAction.RequestRemoveProject)
                }
            )
        }
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = LineTheme.LG.dp,
                    end = LineTheme.LG.dp,
                    bottom = LineTheme.SM.dp
                )
                .background(
                    Color(LineTheme.SURFACE_LIGHT),
                    RoundedCornerShape(LineTheme.SHAPE_SM.dp)
                )
                .border(
                    1.dp,
                    Color(LineTheme.BORDER_LIGHT),
                    RoundedCornerShape(LineTheme.SHAPE_SM.dp)
                )
                .then(projectGesture)
                .padding(LineTheme.SM.dp)
        ) {
            Text(
                text = state.projectLabel,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_SM.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.projectPath,
                modifier = Modifier.padding(top = 2.dp),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        val rows = state.fileRows
        if (rows == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = stringResource(R.string.drawer_files_preparing),
                    color = Color(LineTheme.TEXT_TERTIARY),
                    fontSize = LineTheme.FONT_SM.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        start = LineTheme.SM.dp,
                        top = LineTheme.SM.dp,
                        end = LineTheme.SM.dp
                    )
            ) {
                itemsIndexed(rows) { _, row ->
                    DrawerFileRow(row = row, onAction = onAction)
                }
            }
        }
    }
}

@Composable
private fun DrawerFileRow(
    row: DrawerFileUi,
    onAction: (DrawerUiAction) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(row.path, row.name, row.directory, row.root) {
                detectTapGestures(
                    onTap = {
                        onAction(DrawerUiAction.SelectFile(row.path, row.directory))
                    },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAction(
                            DrawerUiAction.LongPressFile(
                                path = row.path,
                                name = row.name,
                                directory = row.directory,
                                root = row.root
                            )
                        )
                    }
                )
            }
            .padding(
                start = (LineTheme.SM + row.depth * 16).dp,
                top = 4.dp,
                end = LineTheme.SM.dp,
                bottom = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawerIcon(
            type = when (row.icon) {
                DrawerFileIcon.FOLDER -> IconButtonView.FOLDER
                DrawerFileIcon.FOLDER_OPEN -> IconButtonView.FOLDER_OPEN
                DrawerFileIcon.CODE -> IconButtonView.FILE_CODE
                DrawerFileIcon.TEXT -> IconButtonView.FILE_TEXT
                DrawerFileIcon.FILE -> IconButtonView.FILE
            },
            color = when (row.color) {
                DrawerFileColor.ACCENT -> LineTheme.ACCENT
                DrawerFileColor.SECONDARY -> LineTheme.TEXT_SECONDARY
                DrawerFileColor.TERTIARY -> LineTheme.TEXT_TERTIARY
                DrawerFileColor.WARNING -> LineTheme.WARNING
                DrawerFileColor.CODE_YELLOW ->
                    android.graphics.Color.parseColor("#F0DB4F")
            },
            containerSize = if (row.directory) 16.dp else 14.dp,
            iconSize = if (row.directory) 16 else 14
        )
        Text(
            text = row.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = LineTheme.SM.dp),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_SM.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (row.root) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clickable {
                        onAction(
                            DrawerUiAction.LongPressFile(
                                path = row.path,
                                name = row.name,
                                directory = true,
                                root = true
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                DrawerIcon(
                    type = IconButtonView.PLUS,
                    color = LineTheme.TEXT_TERTIARY,
                    containerSize = 22.dp,
                    iconSize = 14
                )
            }
        }
    }
}

@Composable
private fun DrawerIcon(
    type: Int,
    color: Int,
    containerSize: Dp,
    iconSize: Int
) {
    key(type) {
        AndroidView(
            modifier = Modifier.size(containerSize),
            factory = { context ->
                IconButtonView(context, type).apply {
                    setIconColor(color)
                    setIconSizeDp(containerSize.value.toInt(), iconSize)
                    isClickable = false
                    isFocusable = false
                    setPadding(0, 0, 0, 0)
                    minimumWidth = 0
                    minimumHeight = 0
                }
            },
            update = { icon ->
                icon.setIconColor(color)
                icon.setIconSizeDp(containerSize.value.toInt(), iconSize)
            }
        )
    }
}
