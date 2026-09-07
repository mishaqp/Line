package cn.lineai.ui.component

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.ComposerImagePreviewUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ComposerImagePreviewContent(
    state: ComposerImagePreviewUiState,
    onRemove: () -> Unit
) {
    if (!state.visible) return

    val cardShape = RoundedCornerShape(LineTheme.SHAPE_MD.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .border(1.dp, Color(LineTheme.BORDER_LIGHT), cardShape)
            .padding(LineTheme.SM.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = LineTheme.rounded(
                        context,
                        LineTheme.SURFACE_LIGHT,
                        LineTheme.SHAPE_SM
                    )
                    clipToOutline = true
                }
            },
            update = { imageView ->
                val uri = state.uri?.let(Uri::parse)
                if (uri == null) {
                    imageView.setImageDrawable(null)
                } else {
                    try {
                        imageView.setImageURI(uri)
                    } catch (_: Exception) {
                        imageView.setImageDrawable(null)
                    }
                }
            },
            modifier = Modifier.size(56.dp)
        )

        Text(
            text = state.name.ifEmpty {
                stringResource(R.string.composer_image_default_name)
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = LineTheme.SM.dp)
                .widthIn(max = 220.dp),
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.TYPE_BODY.sp,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .padding(start = LineTheme.SM.dp)
                .size(28.dp)
        ) {
            Icon(
                painter = painterResource(cn.lineai.ui.theme.R.drawable.ic_lucide_x),
                contentDescription = stringResource(R.string.composer_image_remove_desc),
                modifier = Modifier.size(16.dp),
                tint = Color(LineTheme.TEXT_TERTIARY)
            )
        }
    }
}
