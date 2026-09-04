package cn.lineai.ui.component;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.lineai.R;
import cn.lineai.ui.theme.IconButtonView;
import cn.lineai.ui.theme.LineCards;
import cn.lineai.ui.theme.LineTheme;

/**
 * Preview card for the single image staged for the next message.
 *
 * <p>Extracted from {@code ComposerView}: it owns both the thumbnail row and the pending
 * base64 payload, so the composer no longer carries four loose {@code pendingImage*}
 * fields. Rendered as an M3 outlined card on {@link LineTheme#SHAPE_MD} with a
 * {@link LineTheme#SHAPE_SM} thumbnail.</p>
 */
final class ComposerImagePreview extends LinearLayout {

    /** Notified when the staged image is set or cleared. */
    interface Listener {
        void onImageStateChanged();
    }

    private static final int THUMB_DP = 56;

    private final ImageView thumbnail;
    private final TextView label;
    private Listener listener;
    private Uri imageUri;
    private String base64 = "";
    private String mimeType = "";
    private String name = "";

    ComposerImagePreview(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackground(LineCards.cardBackground(context, LineTheme.SURFACE_ELEVATED, LineTheme.BORDER_LIGHT));
        LineTheme.padding(this, LineTheme.SM, LineTheme.SM, LineTheme.SM, LineTheme.SM);
        setVisibility(GONE);

        int thumbSize = LineTheme.dp(context, THUMB_DP);
        thumbnail = new ImageView(context);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setBackground(LineTheme.rounded(context, LineTheme.SURFACE_LIGHT, LineTheme.SHAPE_SM));
        thumbnail.setClipToOutline(true);
        addView(thumbnail, new LinearLayout.LayoutParams(thumbSize, thumbSize));

        label = LineTheme.text(context, "", LineTheme.TYPE_BODY, LineTheme.TEXT_SECONDARY, Typeface.NORMAL);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        label.setMaxWidth(LineTheme.dp(context, 220));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f);
        labelParams.leftMargin = LineTheme.dp(context, LineTheme.SM);
        addView(label, labelParams);

        IconButtonView close = new IconButtonView(context, IconButtonView.CLOSE);
        close.setContentDescription(context.getString(R.string.composer_image_remove_desc));
        close.setIconColor(LineTheme.TEXT_TERTIARY);
        close.setIconSizeDp(28, 16);
        close.setBackground(LineCards.pillBackground(context, android.graphics.Color.TRANSPARENT));
        LineTheme.attachStateLayer(close);
        close.setOnClickListener(v -> clear());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LineTheme.dp(context, 28), LineTheme.dp(context, 28));
        closeParams.leftMargin = LineTheme.dp(context, LineTheme.SM);
        addView(close, closeParams);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Stages an image: shows the thumbnail and remembers the encoded payload. */
    void show(Uri uri, String encodedBase64, String encodedMimeType, String displayName) {
        imageUri = uri;
        base64 = encodedBase64 == null ? "" : encodedBase64;
        mimeType = encodedMimeType == null ? "" : encodedMimeType;
        name = displayName == null ? "" : displayName;
        if (uri != null) {
            try {
                thumbnail.setImageURI(uri);
            } catch (Exception ignored) {
                thumbnail.setImageDrawable(null);
            }
        } else {
            thumbnail.setImageDrawable(null);
        }
        label.setText(name.length() > 0 ? name : getContext().getString(R.string.composer_image_default_name));
        setVisibility(VISIBLE);
        notifyChanged();
    }

    /** Drops the staged image and hides the card. */
    void clear() {
        imageUri = null;
        base64 = "";
        mimeType = "";
        name = "";
        thumbnail.setImageDrawable(null);
        setVisibility(GONE);
        notifyChanged();
    }

    boolean hasImage() {
        return base64.length() > 0;
    }

    Uri uri() {
        return imageUri;
    }

    String base64() {
        return base64;
    }

    String mimeType() {
        return mimeType;
    }

    String name() {
        return name;
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onImageStateChanged();
        }
    }
}
