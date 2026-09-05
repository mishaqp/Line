package cn.lineai.ui.component;
import cn.lineai.ui.theme.IconButtonView;
import cn.lineai.ui.theme.LineCards;
import cn.lineai.ui.theme.LineTheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import cn.lineai.R;
import cn.lineai.model.AiBehaviorSettings;
import cn.lineai.model.ChatMode;
import cn.lineai.model.ChatUiState;
import cn.lineai.model.InputAttachment;
import cn.lineai.model.InputSettings;
import cn.lineai.model.ModelConfig;
import cn.lineai.mvp.QuoteController;
import cn.lineai.ui.util.SlashCommandCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ComposerView extends LinearLayout implements QuoteController.QuotePreview {
    public interface Listener {
        void onSend(String text, List<InputAttachment> attachments);

        void onSendWithImage(String text, List<InputAttachment> attachments,
                             String imageBase64, String imageMimeType, String imageName);

        void onAttachClick();

        void onImagePickerClick();

        void onCompactClick();

        void onModeChanged(String mode);

        void onStop();

        void onModelQuickSwitch(String modelId);

        void onModelManageClick();

        void onAiReasoningEffortChanged(String effort);

        int onQueryModelCount(String baseUrl) throws Exception;
    }

    /**
     * 关闭引用预览时的回调，便于 QuoteController 清理自身状态。
     */
    public interface QuoteDismissListener {
        void onQuoteDismissed();
    }

    /** Send button tint while a queued message would stop generation and flush the queue. */
    private static final int QUEUE_STOP_COLOR = 0xFFFF8800;
    /** Send button tint while pressing would append the draft to the queue. */
    private static final int QUEUE_APPEND_COLOR = 0xFFFFAA33;

    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearLayout quotePreviewLayout;
    private TextView quotePreviewText;
    private IconButtonView quoteCloseButton;
    private QuoteDismissListener quoteDismissListener;
    private LinearLayout modelSelectorButton;
    private ComposerAttachmentStrip attachmentStrip;
    private IconButtonView attachButton;
    private IconButtonView imageButton;
    private ComposerImagePreview imagePreview;
    private EditText input;
    private IconButtonView sendButton;
    private PopupWindow modePopup;
    private PopupWindow modelPopup;
    private IconButtonView reasoningButton;
    private String currentReasoningEffort = AiBehaviorSettings.REASONING_MEDIUM;
    private PopupWindow modelSubPopup;
    private SlashCommandPopup slashPopup;
    private String lastSlashSignature = null;
    private boolean streaming;
    private String chatMode = ChatMode.DEFAULT;
    private String enterKeyBehavior = InputSettings.ENTER_SEND;
    private String selectedModelId = "";
    private List<ModelConfig> availableModels = Collections.emptyList();
    private Listener listener;
    private String quoteText = null;
    private LinearLayout quoteBlock;
    private final ComposerQueue pendingQueue = new ComposerQueue();
    private ComposerPendingQueueView pendingQueueView;

    public ComposerView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(LineTheme.BG);
        setWillNotDraw(false);
        LineTheme.padding(this, LineTheme.LG, LineTheme.SM, LineTheme.LG, LineTheme.LG);

        buildQuotePreview();

        attachmentStrip = new ComposerAttachmentStrip(context);
        attachmentStrip.setListener(this::updateSendButton);
        LinearLayout.LayoutParams attachmentParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        attachmentParams.bottomMargin = LineTheme.dp(context, LineTheme.SM);
        addView(attachmentStrip, attachmentParams);

        imagePreview = new ComposerImagePreview(context);
        imagePreview.setListener(this::updateSendButton);
        LinearLayout.LayoutParams imagePreviewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        imagePreviewParams.bottomMargin = LineTheme.dp(context, LineTheme.SM);
        addView(imagePreview, imagePreviewParams);

        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(VERTICAL);
        panel.setMinimumHeight(LineTheme.dp(context, 112));
        panel.setBackground(LineTheme.roundedStroke(context, LineTheme.INPUT_BG, LineTheme.SHAPE_XL, LineTheme.BORDER));
        addView(panel, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        modelSelectorButton = new LinearLayout(context);
        modelSelectorButton.setOrientation(HORIZONTAL);
        modelSelectorButton.setGravity(Gravity.CENTER);
        modelSelectorButton.setClickable(true);
        modelSelectorButton.setFocusable(true);
        modelSelectorButton.setOnClickListener(v -> showModelPopup(modelSelectorButton));
        LineTheme.attachStateLayer(modelSelectorButton);
        IconButtonView modelIcon = new IconButtonView(context, IconButtonView.SPARKLES);
        modelIcon.setIconColor(LineTheme.ACCENT);
        modelIcon.setIconSizeDp(40, 20);
        modelIcon.setClickable(false);
        modelSelectorButton.addView(modelIcon, new LinearLayout.LayoutParams(
                LineTheme.dp(context, 40), LineTheme.dp(context, 40)));


        // Quote block (hidden by default)
        quoteBlock = new LinearLayout(context);
        quoteBlock.setOrientation(HORIZONTAL);
        quoteBlock.setGravity(Gravity.CENTER_VERTICAL);
        quoteBlock.setBackgroundColor(LineTheme.SURFACE_ELEVATED);
        LineTheme.padding(quoteBlock, LineTheme.MD, LineTheme.SM, LineTheme.SM, LineTheme.SM);
        quoteBlock.setVisibility(GONE);
        android.view.View quoteBar = new android.view.View(context);
        quoteBar.setBackgroundColor(LineTheme.ACCENT);
        quoteBlock.addView(quoteBar, new LinearLayout.LayoutParams(LineTheme.dp(context, 3), LayoutParams.MATCH_PARENT));
        TextView quotePreview = LineTheme.text(context, "", LineTheme.FONT_XS, LineTheme.TEXT_SECONDARY, Typeface.ITALIC);
        quotePreview.setSingleLine(true);
        quotePreview.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams qtp = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        qtp.leftMargin = LineTheme.dp(context, LineTheme.SM);
        quoteBlock.addView(quotePreview, qtp);
        IconButtonView quoteClose = new IconButtonView(context, IconButtonView.CLOSE);
        quoteClose.setIconColor(LineTheme.TEXT_TERTIARY);
        quoteClose.setIconSizeDp(24, 14);
        quoteClose.setOnClickListener(v -> clearQuote());
        quoteBlock.addView(quoteClose, new LinearLayout.LayoutParams(LineTheme.dp(context, 24), LineTheme.dp(context, 24)));
        panel.addView(quoteBlock, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // Pending queue container (vertical stack, each queued message is a row)
        pendingQueueView = new ComposerPendingQueueView(context, pendingQueue);
        pendingQueueView.setListener(this::updateSendButton);
        panel.addView(pendingQueueView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout inputRow = new LinearLayout(context);
        inputRow.setOrientation(HORIZONTAL);
        inputRow.setGravity(Gravity.TOP);
        LineTheme.padding(inputRow, LineTheme.SM, LineTheme.SM, LineTheme.SM, 0);
        panel.addView(inputRow, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        attachButton = new IconButtonView(context, IconButtonView.PLUS);
        attachButton.setIconColor(LineTheme.ACCENT);
        attachButton.setIconSizeDp(40, 22);
        LineCards.applyTonalIconButton(attachButton);
        attachButton.setOnClickListener(v -> {
            if (!streaming && listener != null) {
                listener.onAttachClick();
            }
        });

        imageButton = new IconButtonView(context, IconButtonView.IMAGE);
        imageButton.setIconColor(LineTheme.ACCENT);
        imageButton.setIconSizeDp(40, 22);
        LineCards.applyTonalIconButton(imageButton);
        imageButton.setContentDescription(context.getString(R.string.composer_image_button_desc));
        imageButton.setOnClickListener(v -> {
            if (!streaming && listener != null) {
                listener.onImagePickerClick();
            }
        });

        input = new EditText(context);
        input.setTextColor(LineTheme.TEXT);
        input.setHintTextColor(LineTheme.TEXT_TERTIARY);
        input.setHint(context.getString(R.string.composer_hint_default));
        input.setTextSize(LineTheme.chatSp(LineTheme.TYPE_TITLE));
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setMaxLines(6);
        input.setMinHeight(LineTheme.dp(context, 68));
        input.setMaxHeight(LineTheme.dp(context, 152));
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        input.setIncludeFontPadding(false);
        input.setPadding(LineTheme.dp(context, LineTheme.SM), LineTheme.dp(context, LineTheme.SM),
                LineTheme.dp(context, LineTheme.SM), LineTheme.dp(context, LineTheme.SM));
        input.setOnEditorActionListener((view, actionId, event) -> {
            if (!InputSettings.ENTER_SEND.equals(enterKeyBehavior)) {
                return false;
            }
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                return submitCurrentInput();
            }
            if (isPlainEnterDown(event)) {
                return submitCurrentInput();
            }
            return false;
        });
        input.setOnKeyListener((view, keyCode, event) -> {
            if (!InputSettings.ENTER_SEND.equals(enterKeyBehavior)) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_ENTER && isPlainEnterDown(event)) {
                return submitCurrentInput();
            }
            return false;
        });
        input.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                if (modePopup != null && modePopup.isShowing()) {
                    modePopup.dismiss();
                }
                if (modelPopup != null && modelPopup.isShowing()) {
                    modelPopup.dismiss();
                }
            } else {
                dismissSlashPopup();
            }
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        inputRow.addView(input, inputParams);

        IconButtonView expandButton = new IconButtonView(context, IconButtonView.EXPAND);
        expandButton.setIconColor(LineTheme.TEXT_SECONDARY);
        expandButton.setIconSizeDp(40, 19);
        expandButton.setContentDescription(context.getString(R.string.composer_expand_desc));
        LineTheme.attachStateLayer(expandButton);
        expandButton.setOnClickListener(v -> {
            boolean expanded = input.getMaxLines() > 6;
            input.setMinLines(expanded ? 2 : 6);
            input.setMaxLines(expanded ? 6 : 14);
            input.setMaxHeight(LineTheme.dp(context, expanded ? 152 : 360));
            input.requestFocus();
        });
        inputRow.addView(expandButton, new LinearLayout.LayoutParams(
                LineTheme.dp(context, 40), LineTheme.dp(context, 40)));

        sendButton = new IconButtonView(context, IconButtonView.ARROW_UP);
        sendButton.setIconSizeDp(40, 22);
        sendButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (streaming) {
                if (canSend()) {
                    // 输入框有内容：追加排队（不打断AI）
                    queueCurrentInput();
                } else {
                    // 输入框为空：停止AI（render()检测到停止后自动发送队列）
                    if (listener != null) listener.onStop();
                }
                return;
            }
            submitCurrentInput();
        });

        LinearLayout toolbarRow = new LinearLayout(context);
        toolbarRow.setOrientation(HORIZONTAL);
        toolbarRow.setGravity(Gravity.CENTER_VERTICAL);
        LineTheme.padding(toolbarRow, LineTheme.SM, 0, LineTheme.SM, LineTheme.SM);
        panel.addView(toolbarRow, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams modelParams = new LinearLayout.LayoutParams(
                LineTheme.dp(context, 40), LineTheme.dp(context, 40));
        modelParams.rightMargin = LineTheme.dp(context, LineTheme.XS);
        toolbarRow.addView(modelSelectorButton, modelParams);

        reasoningButton = new IconButtonView(context, IconButtonView.BRAIN);
        reasoningButton.setIconColor(LineTheme.TEXT_SECONDARY);
        reasoningButton.setIconSizeDp(40, 20);
        LineTheme.attachStateLayer(reasoningButton);
        reasoningButton.setOnClickListener(v -> showReasoningPopup(reasoningButton));
        toolbarRow.addView(reasoningButton, new LinearLayout.LayoutParams(
                LineTheme.dp(context, 40), LineTheme.dp(context, 40)));

        View spacer = new View(context);
        toolbarRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        IconButtonView plusButton = new IconButtonView(context, IconButtonView.PLUS);
        plusButton.setIconColor(LineTheme.TEXT_SECONDARY);
        plusButton.setIconSizeDp(40, 21);
        LineTheme.attachStateLayer(plusButton);
        plusButton.setOnClickListener(v -> showPlusPopup(plusButton));
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(
                LineTheme.dp(context, 40), LineTheme.dp(context, 40));
        plusParams.rightMargin = LineTheme.dp(context, LineTheme.XS);
        toolbarRow.addView(plusButton, plusParams);

        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
                LineTheme.dp(context, 40), LineTheme.dp(context, 40));
        toolbarRow.addView(sendButton, sendParams);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButton();
                updateSlashPopup();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        slashPopup = new SlashCommandPopup(context);
        updateSendButton();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setQuoteDismissListener(QuoteDismissListener listener) {
        this.quoteDismissListener = listener;
    }

    private void buildQuotePreview() {
        Context context = getContext();
        quotePreviewLayout = new LinearLayout(context);
        quotePreviewLayout.setOrientation(HORIZONTAL);
        quotePreviewLayout.setGravity(Gravity.CENTER_VERTICAL);
        quotePreviewLayout.setBackground(LineCards.cardBackground(context, LineTheme.SURFACE_ELEVATED, LineTheme.BORDER_LIGHT));
        LineTheme.padding(quotePreviewLayout, LineTheme.SM, LineTheme.SM, LineTheme.SM, LineTheme.SM);

        View quoteBar = new View(context);
        quoteBar.setBackground(LineTheme.rounded(context, LineTheme.ACCENT, LineTheme.SHAPE_XS));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(LineTheme.dp(context, 3), LayoutParams.MATCH_PARENT);
        barParams.rightMargin = LineTheme.dp(context, LineTheme.SM);
        quotePreviewLayout.addView(quoteBar, barParams);

        quotePreviewText = LineTheme.text(context, "", LineTheme.FONT_SM, LineTheme.TEXT_SECONDARY, Typeface.NORMAL);
        quotePreviewText.setMaxLines(2);
        quotePreviewText.setEllipsize(TextUtils.TruncateAt.END);
        quotePreviewLayout.addView(quotePreviewText, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        quoteCloseButton = new IconButtonView(context, IconButtonView.CLOSE);
        quoteCloseButton.setIconColor(LineTheme.TEXT_TERTIARY);
        quoteCloseButton.setIconSizeDp(28, 16);
        quoteCloseButton.setOnClickListener(v -> {
            hideQuote();
            if (quoteDismissListener != null) {
                quoteDismissListener.onQuoteDismissed();
            }
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(LineTheme.dp(context, 28), LineTheme.dp(context, 28));
        closeParams.leftMargin = LineTheme.dp(context, LineTheme.SM);
        quotePreviewLayout.addView(quoteCloseButton, closeParams);

        quotePreviewLayout.setVisibility(GONE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.bottomMargin = LineTheme.dp(context, LineTheme.SM);
        addView(quotePreviewLayout, params);
    }

    /**
     * 选择图片后调用，显示缩略图预览并暂存 base64 数据。
     */
    public void onImagePicked(Uri uri, String base64, String mimeType, String displayName) {
        imagePreview.show(uri, base64, mimeType, displayName);
    }

    /**
     * 清除当前选中的图片。
     */
    public void clearImage() {
        imagePreview.clear();
    }

    public boolean hasPendingImage() {
        return imagePreview.hasImage();
    }

    @Override
    public void showQuote(String previewText) {
        if (quotePreviewLayout == null) {
            return;
        }
        quotePreviewText.setText(previewText == null ? "" : previewText);
        quotePreviewLayout.setVisibility(VISIBLE);
    }

    @Override
    public void hideQuote() {
        if (quotePreviewLayout == null) {
            return;
        }
        quotePreviewLayout.setVisibility(GONE);
    }

    public void setDraft(String text) {
        setDraft(text, Collections.emptyList());
    }

    public void setDraft(String text, List<InputAttachment> nextAttachments) {
        String value = text == null ? "" : text;
        input.setText(value);
        attachmentStrip.replaceAll(nextAttachments);
        input.setSelection(input.getText().length());
        input.requestFocus();
        updateSendButton();
    }

    public List<InputAttachment> getAttachments() {
        return attachmentStrip.attachments();
    }

    public List<String> selectedAttachmentPaths(String source) {
        return attachmentStrip.pathsForSource(source);
    }

    public void toggleAttachment(InputAttachment attachment) {
        attachmentStrip.toggle(attachment);
    }

    public void render(ChatUiState state) {
        boolean wasStreamingBefore = streaming;
        streaming = state.isStreaming();
        selectedModelId = state.getSelectedModelId();
        availableModels = state.getAvailableModels();
        chatMode = state.getChatMode();
        updateEnterKeyBehavior(state.getEnterKeyBehavior());
        if (streaming && modePopup != null) {
            modePopup.dismiss();
        }
        // Auto-send queued message when streaming finishes
        if (wasStreamingBefore && !streaming && !pendingQueue.isEmpty()) {
            post(() -> sendPending());
        }
        if (streaming && modelPopup != null) {
            modelPopup.dismiss();
        }
        if (streaming) {
            dismissSlashPopup();
        } else {
            updateSlashPopup();
        }
        input.setEnabled(true); // Allow typing while AI is streaming
        attachButton.setEnabled(!streaming);
        attachButton.setAlpha(streaming ? 0.62f : 1f);
        imageButton.setEnabled(!streaming);
        imageButton.setAlpha(streaming ? 0.62f : 1f);
        input.setHint(state.hasConfiguredModel()
                ? getContext().getString(R.string.composer_hint_default)
                : getContext().getString(R.string.composer_hint_no_model));
        currentReasoningEffort = AiBehaviorSettings.normalizeReasoningEffort(state.getReasoningEffort());
        updateModelSelector();
        updateReasoningButton();
        updateSendButton();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        borderPaint.setColor(LineTheme.BORDER);
        borderPaint.setStrokeWidth(1f);
        canvas.drawLine(0, 0, getWidth(), 0, borderPaint);
    }

    private void updateSendButton() {
        boolean hasContent = canSend();
        if (streaming) {
            if (!pendingQueue.isEmpty() && !hasContent) {
                // 有队列 + 输入框空：红色停止按钮（按=停止AI并发送队列）
                sendButton.setIconType(IconButtonView.STOP);
                sendButton.setIconColor(LineTheme.TEXT_ON_COLOR);
                sendButton.setIconSizeDp(40, 18);
                sendButton.setBackground(LineCards.pillBackground(getContext(), QUEUE_STOP_COLOR));
            } else if (hasContent) {
                // 有内容：橙色箭头（按=追加排队）
                sendButton.setIconType(IconButtonView.ARROW_UP);
                sendButton.setIconColor(LineTheme.TEXT_ON_COLOR);
                sendButton.setIconSizeDp(40, 22);
                sendButton.setBackground(LineCards.pillBackground(getContext(), QUEUE_APPEND_COLOR));
            } else {
                // 无内容无队列：红色停止
                sendButton.setIconType(IconButtonView.STOP);
                sendButton.setIconColor(LineTheme.TEXT_ON_COLOR);
                sendButton.setIconSizeDp(40, 18);
                sendButton.setBackground(LineCards.pillBackground(getContext(), LineTheme.DANGER));
            }
        } else {
            sendButton.setIconType(IconButtonView.ARROW_UP);
            sendButton.setIconColor(hasContent ? LineTheme.TEXT_ON_COLOR : LineTheme.TEXT_TERTIARY);
            sendButton.setIconSizeDp(40, 22);
            // BORDER_LIGHT (not SURFACE_LIGHT): several palettes make surfaceLight equal to
            // inputBg, which would render the idle send button invisible on the panel.
            sendButton.setBackground(LineCards.pillBackground(getContext(),
                    hasContent ? LineTheme.ACCENT : LineTheme.BORDER_LIGHT));
        }
        LineTheme.attachStateLayer(sendButton, LineTheme.TEXT_ON_COLOR);
        sendButton.setEnabled(streaming || hasContent);
        sendButton.setAlpha(sendButton.isEnabled() ? 1f : 0.72f);
    }

    private boolean canSend() {
        return input.getText().toString().trim().length() > 0
                || !attachmentStrip.isEmpty()
                || hasPendingImage();
    }

    private boolean submitCurrentInput() {
        if (!canSend()) {
            return true;
        }
        if (streaming) {
            // Enter键在streaming时：追加排队
            queueCurrentInput();
            return true;
        }
        String text = input.getText().toString();
        // Prepend quote if present
        if (quoteText != null && quoteText.length() > 0) {
            String quoted = "> " + quoteText.replace("\n", "\n> ") + "\n\n";
            text = quoted + text;
            clearQuote();
        }
        SlashCommandCatalog.Parsed parsed = SlashCommandCatalog.parse(text);
        if (parsed != null) {
            if (listener == null) {
                input.setText("");
                clearAttachments();
                clearImage();
                dismissSlashPopup();
                return true;
            }
            if (parsed.kind == SlashCommandCatalog.Kind.MODE) {
                listener.onModeChanged(parsed.mode);
            } else if (parsed.kind == SlashCommandCatalog.Kind.MODEL) {
                listener.onModelQuickSwitch(parsed.modelId);
                if (parsed.reasoningEffort != null) {
                    listener.onAiReasoningEffortChanged(parsed.reasoningEffort);
                }
            }
            input.setText("");
            clearAttachments();
            clearImage();
            dismissSlashPopup();
            return true;
        }
        if (listener != null) {
            if (hasPendingImage()) {
                listener.onSendWithImage(text, getAttachments(),
                        imagePreview.base64(), imagePreview.mimeType(), imagePreview.name());
            } else {
                listener.onSend(text, getAttachments());
            }
        }
        input.setText("");
        clearAttachments();
        clearImage();
        return true;
    }

    private boolean isPlainEnterDown(KeyEvent event) {
        return event != null
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                && !event.isShiftPressed();
    }

    public void setQuoteText(String text) {
        quoteText = text;
        if (text != null && text.length() > 0) {
            TextView preview = (TextView) quoteBlock.getChildAt(1);
            preview.setText(text.length() > 80 ? text.substring(0, 80) + "..." : text);
            quoteBlock.setVisibility(VISIBLE);
            input.requestFocus();
        } else {
            quoteBlock.setVisibility(GONE);
        }
    }

    public void clearQuote() {
        quoteText = null;
        quoteBlock.setVisibility(GONE);
    }

    private void queueCurrentInput() {
        String text = input.getText().toString();
        if (quoteText != null && quoteText.length() > 0) {
            String quoted = "> " + quoteText.replace("\n", "\n> ") + "\n\n";
            text = quoted + text;
            clearQuote();
        }
        pendingQueue.add(text, attachmentStrip.attachments());
        input.setText("");
        clearAttachments();
        updatePendingBlock();
        updateSendButton();
    }

    /** Re-renders the queued-message rows shown inside the composer panel. */
    private void updatePendingBlock() {
        pendingQueueView.refresh();
    }

    private void sendPending() {
        ComposerQueue.Item item = pendingQueue.poll();
        if (item == null) {
            return;
        }
        updatePendingBlock();
        if (listener != null) {
            listener.onSend(item.text(), item.attachments());
        }
    }

    private void clearPending() {
        pendingQueue.clear();
        updatePendingBlock();
        updateSendButton();
    }

    private void updateEnterKeyBehavior(String behavior) {
        enterKeyBehavior = InputSettings.normalizeEnterKeyBehavior(behavior);
        input.setImeOptions(InputSettings.ENTER_SEND.equals(enterKeyBehavior)
                ? EditorInfo.IME_ACTION_SEND
                : EditorInfo.IME_ACTION_NONE);
    }

    private void clearAttachments() {
        attachmentStrip.clear();
    }

    private void updateSlashPopup() {
        if (slashPopup == null) {
            return;
        }
        if (streaming) {
            dismissSlashPopup();
            return;
        }
        String text = input.getText() == null ? "" : input.getText().toString();
        SlashState state = resolveSlashState(text);
        if (state == null) {
            dismissSlashPopup();
            return;
        }
        String signature = state.signature + "|" + chatMode;
        if (signature.equals(lastSlashSignature) && slashPopup.isShowing()) {
            return;
        }
        lastSlashSignature = signature;
        List<SlashCommandPopup.Row> rows = buildSlashRows(state);
        if (rows.isEmpty()) {
            dismissSlashPopup();
            return;
        }
        slashPopup.setSelectedIndex(state.selectedIndex);
        slashPopup.show(state.title, rows);
        slashPopup.showAtAnchor(this);
    }

    public void dismissSlashPopup() {
        if (slashPopup != null) {
            slashPopup.dismiss();
        }
        lastSlashSignature = null;
    }

    /**
     * 把当前输入文本解析为 slash popup 状态。返回 null 表示不应展示 popup。
     * 三种状态：主命令（MAIN）、模型 id（MODEL_ID）、思考等级（REASONING）。
     */
    private SlashState resolveSlashState(String text) {
        if (text == null || text.length() == 0) {
            return null;
        }
        if (text.charAt(0) != '/') {
            return null;
        }
        String[] tokens = text.split("\\s+", -1);
        String head = tokens[0].toLowerCase();
        if ("/model".equals(head)) {
            if (tokens.length < 2 || tokens[1].length() == 0) {
                return modelIdState("");
            }
            String modelId = tokens[1];
            if (!containsModelId(modelId)) {
                return mainState("");
            }
            String reasonQuery = tokens.length >= 3 ? tokens[2] : "";
            return reasoningState(modelId, reasonQuery);
        }
        return mainState(head.substring(1));
    }

    private boolean containsModelId(String id) {
        for (ModelConfig model : availableModels) {
            if (model != null && model.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private SlashState mainState(String query) {
        List<SlashCommandCatalog.Definition> defs = SlashCommandCatalog.filterMain(query);
        int selected = -1;
        for (int i = 0; i < defs.size(); i++) {
            SlashCommandCatalog.Definition def = defs.get(i);
            if (def.kind == SlashCommandCatalog.Kind.MODE && def.token.substring(1).equalsIgnoreCase(chatMode)) {
                selected = i;
                break;
            }
        }
        return new SlashState(SlashState.Kind.MAIN,
                getContext().getString(R.string.slash_command_main_title),
                defs, query, selected, null,
                Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    private SlashState modelIdState(String query) {
        List<String> ids = new ArrayList<>();
        String needle = query == null ? "" : query.trim().toLowerCase();
        for (ModelConfig model : availableModels) {
            if (model != null) {
                if (needle.length() > 0 && !modelMatchesQuery(model, needle)) {
                    continue;
                }
                ids.add(model.getId());
            }
        }
        List<String> filtered = SlashCommandCatalog.filterModelIds(ids, query);
        int selected = -1;
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).equals(selectedModelId)) {
                selected = i;
                break;
            }
        }
        return new SlashState(SlashState.Kind.MODEL_ID,
                getContext().getString(R.string.slash_command_model_title),
                Collections.<SlashCommandCatalog.Definition>emptyList(), query, selected, null,
                filtered, Collections.<String>emptyList());
    }

    private SlashState reasoningState(String modelId, String query) {
        List<String> levels = SlashCommandCatalog.filterReasoningLevels(query);
        return new SlashState(SlashState.Kind.REASONING,
                getContext().getString(R.string.slash_command_reasoning_title, modelId),
                Collections.<SlashCommandCatalog.Definition>emptyList(), query, -1, modelId,
                Collections.<String>emptyList(), levels);
    }

    private List<SlashCommandPopup.Row> buildSlashRows(SlashState state) {
        List<SlashCommandPopup.Row> rows = new ArrayList<>();
        if (state.kind == SlashState.Kind.MAIN) {
            for (SlashCommandCatalog.Definition def : state.definitions) {
                String label = def.token;
                String description = mainDescription(def.token);
                Runnable action = () -> applyMainSelection(def);
                rows.add(new SlashCommandPopup.Row(label, description, action));
            }
        } else if (state.kind == SlashState.Kind.MODEL_ID) {
            for (String id : state.modelIds) {
                String label = modelDisplayName(id);
                String description = modelDisplayDetail(id);
                Runnable action = () -> applyModelIdSelection(id);
                rows.add(new SlashCommandPopup.Row(label, description, action));
            }
        } else {
            for (String level : state.levels) {
                String description = reasoningDescription(level);
                Runnable action = () -> applyReasoningSelection(state.modelId, level);
                rows.add(new SlashCommandPopup.Row(level, description, action));
            }
        }
        return rows;
    }

    private ModelConfig findModel(String id) {
        for (ModelConfig model : availableModels) {
            if (model != null && model.getId().equals(id)) {
                return model;
            }
        }
        return null;
    }

    private String modelDisplayName(String id) {
        ModelConfig model = findModel(id);
        if (model == null) return id;
        String name = model.getName();
        if (name.length() > 0 && !name.equals(id)) return name;
        String apiId = model.getModelId();
        if (apiId.length() > 0) return apiId;
        return model.getProviderLabel();
    }

    private String modelDisplayDetail(String id) {
        ModelConfig model = findModel(id);
        if (model == null) return "";
        String label = model.getProviderLabel();
        String apiId = model.getModelId();
        if (apiId.length() > 0 && !apiId.equals(modelDisplayName(id))) {
            return label + " · " + apiId;
        }
        return label;
    }

    private static boolean modelMatchesQuery(ModelConfig model, String needle) {
        if (model.getName().toLowerCase().contains(needle)) return true;
        if (model.getModelId().toLowerCase().contains(needle)) return true;
        if (model.getId().toLowerCase().contains(needle)) return true;
        if (model.getProviderLabel().toLowerCase().contains(needle)) return true;
        return false;
    }

    private String mainDescription(String token) {
        Context ctx = getContext();
        if ("/chat".equals(token)) {
            return ctx.getString(R.string.slash_command_chat_desc);
        }
        if ("/plan".equals(token)) {
            return ctx.getString(R.string.slash_command_plan_desc);
        }
        if ("/agent".equals(token)) {
            return ctx.getString(R.string.slash_command_agent_desc);
        }
        if ("/control".equals(token)) {
            return ctx.getString(R.string.slash_command_control_desc);
        }
        if ("/model".equals(token)) {
            return ctx.getString(R.string.slash_command_model_desc);
        }
        return "";
    }

    private String reasoningDescription(String level) {
        Context ctx = getContext();
        if (AiBehaviorSettings.REASONING_OFF.equals(level)) {
            return ctx.getString(R.string.slash_command_reasoning_off_desc);
        }
        if (AiBehaviorSettings.REASONING_LOW.equals(level)) {
            return ctx.getString(R.string.slash_command_reasoning_low_desc);
        }
        if (AiBehaviorSettings.REASONING_MEDIUM.equals(level)) {
            return ctx.getString(R.string.slash_command_reasoning_medium_desc);
        }
        if (AiBehaviorSettings.REASONING_HIGH.equals(level)) {
            return ctx.getString(R.string.slash_command_reasoning_high_desc);
        }
        if (AiBehaviorSettings.REASONING_MAX.equals(level)) {
            return ctx.getString(R.string.slash_command_reasoning_max_desc);
        }
        return level;
    }

    private void applyMainSelection(SlashCommandCatalog.Definition def) {
        String replacement;
        if (def.kind == SlashCommandCatalog.Kind.MODEL) {
            replacement = "/model ";
        } else {
            replacement = def.token + " ";
        }
        input.setText(replacement);
        input.setSelection(replacement.length());
        lastSlashSignature = null;
    }

    private void applyModelIdSelection(String modelId) {
        String replacement = "/model " + modelId + " ";
        input.setText(replacement);
        input.setSelection(replacement.length());
        lastSlashSignature = null;
    }

    private void applyReasoningSelection(String modelId, String level) {
        String replacement = "/model " + modelId + " " + level;
        input.setText(replacement);
        input.setSelection(replacement.length());
        lastSlashSignature = null;
    }

    private static final class SlashState {
        enum Kind { MAIN, MODEL_ID, REASONING }
        final Kind kind;
        final String title;
        final List<SlashCommandCatalog.Definition> definitions;
        final List<String> modelIds;
        final List<String> levels;
        final String query;
        final int selectedIndex;
        final String modelId;
        final String signature;

        SlashState(Kind kind, String title,
                   List<SlashCommandCatalog.Definition> definitions,
                   String query, int selectedIndex, String modelId,
                   List<String> modelIds, List<String> levels) {
            this.kind = kind;
            this.title = title;
            this.definitions = definitions == null ? Collections.<SlashCommandCatalog.Definition>emptyList() : definitions;
            this.modelIds = modelIds == null ? Collections.<String>emptyList() : modelIds;
            this.levels = levels == null ? Collections.<String>emptyList() : levels;
            this.query = query == null ? "" : query;
            this.selectedIndex = selectedIndex;
            this.modelId = modelId == null ? "" : modelId;
            this.signature = kind.name() + ":" + this.query + ":" + this.modelId + ":"
                    + this.definitions.size() + ":" + this.modelIds.size() + ":" + this.levels.size();
        }
    }

    private void updateModelSelector() {
        modelSelectorButton.setEnabled(!streaming);
        modelSelectorButton.setAlpha(streaming ? 0.62f : 1f);
    }



    private void updateReasoningButton() {
        if (reasoningButton == null) return;
        boolean off = AiBehaviorSettings.REASONING_OFF.equals(currentReasoningEffort);
        boolean strong = AiBehaviorSettings.REASONING_HIGH.equals(currentReasoningEffort)
                || AiBehaviorSettings.REASONING_MAX.equals(currentReasoningEffort);
        reasoningButton.setIconColor(off ? LineTheme.TEXT_TERTIARY
                : (strong ? LineTheme.ACCENT : LineTheme.TEXT_SECONDARY));
        reasoningButton.setAlpha(off ? 0.72f : 1f);
        reasoningButton.setContentDescription(reasoningLabel(currentReasoningEffort));
    }

    private String reasoningLabel(String effort) {
        String value = AiBehaviorSettings.normalizeReasoningEffort(effort);
        int res;
        if (AiBehaviorSettings.REASONING_OFF.equals(value)) res = R.string.composer_reasoning_off;
        else if (AiBehaviorSettings.REASONING_AUTO.equals(value)) res = R.string.composer_reasoning_auto;
        else if (AiBehaviorSettings.REASONING_LOW.equals(value)) res = R.string.composer_reasoning_low;
        else if (AiBehaviorSettings.REASONING_HIGH.equals(value)) res = R.string.composer_reasoning_high;
        else if (AiBehaviorSettings.REASONING_MAX.equals(value)) res = R.string.composer_reasoning_max;
        else res = R.string.composer_reasoning_medium;
        return getContext().getString(R.string.composer_reasoning_title) + ": " + getContext().getString(res);
    }

    private void showPopupAboveComposer(PopupWindow popup, View anchor, int popupWidth, int popupHeight) {
        showPopupAboveComposer(popup, anchor, popupWidth, popupHeight, false);
    }

    private void showPopupAboveComposer(PopupWindow popup, View anchor, int popupWidth, int popupHeight, boolean alignEnd) {
        View content = popup.getContentView();
        if (popupHeight <= 0 && content != null) {
            content.measure(
                    View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            popupHeight = content.getMeasuredHeight();
        }
        Context context = getContext();
        int gap = LineTheme.dp(context, 8);
        int[] composer = new int[2];
        getLocationInWindow(composer);
        int[] a = new int[2];
        anchor.getLocationInWindow(a);
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int x = alignEnd ? a[0] + anchor.getWidth() - popupWidth : a[0];
        x = Math.max(gap, Math.min(x, screenW - popupWidth - gap));
        int y = composer[1] - popupHeight - gap;
        if (y < gap) {
            y = Math.max(gap, a[1] - popupHeight - gap);
        }
        popup.setClippingEnabled(true);
        popup.setElevation(LineTheme.dp(context, 10));
        if (popup.isShowing()) {
            popup.update(x, y, popupWidth, Math.max(popupHeight, 1));
        } else {
            popup.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
        }
    }

    private void showReasoningPopup(View anchor) {
        if (streaming) return;
        dismissComposerPopups();
        input.clearFocus();
        Context context = getContext();
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(VERTICAL);
        content.setBackground(LineTheme.roundedStroke(context, LineTheme.SURFACE_ELEVATED,
                LineTheme.SHAPE_LG, LineTheme.BORDER));
        LineTheme.padding(content, LineTheme.SM, LineTheme.SM, LineTheme.SM, LineTheme.SM);
        final PopupWindow popup = new PopupWindow(content, LineTheme.dp(context, 196),
                LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView title = LineTheme.textMedium(context,
                context.getString(R.string.composer_reasoning_title), LineTheme.FONT_XS, LineTheme.TEXT_SECONDARY);
        LineTheme.padding(title, LineTheme.SM, LineTheme.XS, LineTheme.SM, LineTheme.SM);
        content.addView(title, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        String[] values = {
                AiBehaviorSettings.REASONING_OFF,
                AiBehaviorSettings.REASONING_AUTO,
                AiBehaviorSettings.REASONING_LOW,
                AiBehaviorSettings.REASONING_MEDIUM,
                AiBehaviorSettings.REASONING_HIGH,
                AiBehaviorSettings.REASONING_MAX
        };
        int[] labels = {
                R.string.composer_reasoning_off,
                R.string.composer_reasoning_auto,
                R.string.composer_reasoning_low,
                R.string.composer_reasoning_medium,
                R.string.composer_reasoning_high,
                R.string.composer_reasoning_max
        };
        for (int i = 0; i < values.length; i++) {
            final String effort = values[i];
            boolean selected = effort.equals(currentReasoningEffort);
            TextView item = compactPopupItem(context, context.getString(labels[i]));
            item.setTextColor(selected ? LineTheme.TEXT_ON_COLOR : LineTheme.TEXT);
            item.setBackground(LineTheme.rounded(context,
                    selected ? LineTheme.ACCENT : android.graphics.Color.TRANSPARENT, LineTheme.SHAPE_MD));
            item.setOnClickListener(v -> {
                popup.dismiss();
                currentReasoningEffort = effort;
                updateReasoningButton();
                if (listener != null) listener.onAiReasoningEffortChanged(effort);
            });
            content.addView(item, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LineTheme.dp(context, 40)));
        }
        showPopupAboveComposer(popup, anchor, LineTheme.dp(context, 196), 0, false);
    }

    private void showPlusPopup(View anchor) {
        if (streaming) return;
        dismissComposerPopups();
        input.clearFocus();
        Context context = getContext();
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(VERTICAL);
        content.setBackground(LineTheme.roundedStroke(context, LineTheme.SURFACE_ELEVATED,
                LineTheme.SHAPE_LG, LineTheme.BORDER));
        LineTheme.padding(content, LineTheme.SM, LineTheme.SM, LineTheme.SM, LineTheme.SM);
        final PopupWindow popup = new PopupWindow(content, LineTheme.dp(context, 210),
                LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView file = compactPopupItem(context, context.getString(R.string.composer_plus_file));
        file.setOnClickListener(v -> {
            popup.dismiss();
            if (listener != null) listener.onAttachClick();
        });
        content.addView(file, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LineTheme.dp(context, 40)));

        TextView image = compactPopupItem(context, context.getString(R.string.composer_plus_image));
        image.setOnClickListener(v -> {
            popup.dismiss();
            if (listener != null) listener.onImagePickerClick();
        });
        content.addView(image, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LineTheme.dp(context, 40)));

        TextView compact = compactPopupItem(context, context.getString(R.string.composer_plus_compact));
        compact.setOnClickListener(v -> {
            popup.dismiss();
            if (listener != null) listener.onCompactClick();
        });
        content.addView(compact, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LineTheme.dp(context, 40)));
        showPopupAboveComposer(popup, anchor, LineTheme.dp(context, 210), 0, true);
    }

    private TextView compactPopupItem(Context context, String label) {
        TextView item = LineTheme.textMedium(context, label, LineTheme.FONT_SM, LineTheme.TEXT);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setClickable(true);
        item.setFocusable(true);
        LineTheme.padding(item, LineTheme.MD, 0, LineTheme.MD, 0);
        LineTheme.attachStateLayer(item);
        return item;
    }

    private View modelPopupAnchor;
    private LinearLayout modelPopupContent;

    private void dismissComposerPopups() {
        if (modelSubPopup != null && modelSubPopup.isShowing()) {
            modelSubPopup.dismiss();
        }
        modelSubPopup = null;
        if (modelPopup != null && modelPopup.isShowing()) {
            modelPopup.dismiss();
        }
        if (modePopup != null && modePopup.isShowing()) {
            modePopup.dismiss();
        }
        dismissSlashPopup();
    }

    private void showModelPopup(View anchor) {
        if (streaming) return;
        if (modelPopup != null && modelPopup.isShowing()) {
            modelPopup.dismiss();
            return;
        }
        dismissComposerPopups();
        input.clearFocus();
        Context ctx = getContext();
        java.util.LinkedHashMap<String, String> sources = collectModelSources();
        if (sources.isEmpty()) {
            if (listener != null) listener.onModelManageClick();
            return;
        }

        modelPopupAnchor = anchor;
        modelPopupContent = new LinearLayout(ctx);
        modelPopupContent.setOrientation(VERTICAL);
        modelPopupContent.setBackground(LineCards.cardBackground(ctx, LineTheme.INPUT_BG, LineTheme.BORDER_LIGHT));
        LineTheme.padding(modelPopupContent, 4, 4, 4, 4);
        fillModelSourceList(modelPopupContent, sources);

        int popupWidth = LineTheme.dp(ctx, 220);
        modelPopup = new PopupWindow(modelPopupContent, popupWidth, LayoutParams.WRAP_CONTENT, true);
        modelPopup.setOutsideTouchable(true);
        modelPopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        modelPopup.setOnDismissListener(() -> {
            if (modelSubPopup != null && modelSubPopup.isShowing()) modelSubPopup.dismiss();
        });
        showPopupAboveComposer(modelPopup, anchor, popupWidth, 0, false);
    }

    private java.util.LinkedHashMap<String, String> collectModelSources() {
        java.util.LinkedHashMap<String, String> sources = new java.util.LinkedHashMap<>();
        for (ModelConfig m : availableModels) {
            String key = m.getProviderLabel().length() > 0 ? m.getProviderLabel() : "Other";
            if (!sources.containsKey(key)) {
                sources.put(key, m.getBaseUrl());
            }
        }
        return sources;
    }

    private void fillModelSourceList(LinearLayout content, java.util.LinkedHashMap<String, String> sources) {
        content.removeAllViews();
        Context ctx = getContext();
        int rowHeight = LineTheme.dp(ctx, 40);
        int manageRowHeight = LineTheme.dp(ctx, 36);
        for (String sName : sources.keySet()) {
            String currentModelName = "";
            for (ModelConfig m : availableModels) {
                String pk = m.getProviderLabel().length() > 0 ? m.getProviderLabel() : "Other";
                if (pk.equals(sName) && m.getId().equals(selectedModelId)) {
                    currentModelName = m.getName().length() > 0 ? m.getName() : m.getModelId();
                    break;
                }
            }
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            boolean isActive = currentModelName.length() > 0;
            row.setBackground(LineTheme.rounded(ctx, isActive ? LineTheme.ACCENT_DIM : android.graphics.Color.TRANSPARENT, LineTheme.SHAPE_SM));
            LineTheme.padding(row, LineTheme.SM, 0, LineTheme.SM, 0);
            row.setClickable(true);
            TextView nameView = LineTheme.textMedium(ctx, sName, LineTheme.FONT_SM, isActive ? LineTheme.ACCENT : LineTheme.TEXT);
            nameView.setSingleLine(true);
            row.addView(nameView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            TextView arrow = LineTheme.text(ctx, "\u203A", LineTheme.FONT_SM, LineTheme.TEXT_TERTIARY, Typeface.NORMAL);
            row.addView(arrow, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            final String sourceName = sName;
            final String baseUrl = sources.get(sName);
            row.setOnClickListener(v -> fillModelSubList(content, sourceName, baseUrl));
            content.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));
        }
        View div = new View(ctx);
        div.setBackgroundColor(LineTheme.BORDER_LIGHT);
        content.addView(div, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1));
        TextView manageItem = LineTheme.textMedium(ctx, ctx.getString(R.string.composer_model_manage), LineTheme.FONT_XS, LineTheme.TEXT_TERTIARY);
        manageItem.setGravity(Gravity.CENTER_VERTICAL);
        manageItem.setPadding(LineTheme.dp(ctx, LineTheme.SM), 0, 0, 0);
        manageItem.setClickable(true);
        manageItem.setOnClickListener(v -> {
            if (modelPopup != null) modelPopup.dismiss();
            post(() -> { if (listener != null) listener.onModelManageClick(); });
        });
        content.addView(manageItem, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, manageRowHeight));
        relayoutModelPopup();
    }

    private void fillModelSubList(LinearLayout content, String sourceName, String baseUrl) {
        content.removeAllViews();
        Context ctx = getContext();
        int rowHeight = LineTheme.dp(ctx, 40);

        TextView back = LineTheme.textMedium(ctx, "\u2190  " + sourceName, LineTheme.FONT_SM, LineTheme.TEXT);
        back.setGravity(Gravity.CENTER_VERTICAL);
        LineTheme.padding(back, LineTheme.SM, 0, LineTheme.SM, 0);
        back.setClickable(true);
        back.setOnClickListener(v -> fillModelSourceList(content, collectModelSources()));
        content.addView(back, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));

        TextView queryBtn = LineTheme.textMedium(ctx, ctx.getString(R.string.composer_model_submenu_query_button), LineTheme.FONT_XS, LineTheme.ACCENT);
        queryBtn.setGravity(Gravity.CENTER);
        queryBtn.setBackground(LineTheme.roundedStroke(ctx, LineTheme.ACCENT_MUTED, LineTheme.SHAPE_SM, LineTheme.ACCENT));
        LineTheme.padding(queryBtn, 0, 3, 0, 3);
        queryBtn.setClickable(true);
        queryBtn.setOnClickListener(v -> {
            queryBtn.setText(R.string.screen_model_add_query_button_loading);
            queryModelCount(baseUrl, queryBtn, ctx);
        });
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LineTheme.dp(ctx, 32));
        qp.leftMargin = LineTheme.dp(ctx, LineTheme.SM);
        qp.rightMargin = LineTheme.dp(ctx, LineTheme.SM);
        qp.bottomMargin = LineTheme.dp(ctx, 4);
        content.addView(queryBtn, qp);

        java.util.List<ModelConfig> models = new java.util.ArrayList<>();
        for (ModelConfig m : availableModels) {
            String pk = m.getProviderLabel().length() > 0 ? m.getProviderLabel() : "Other";
            if (pk.equals(sourceName)) models.add(m);
        }
        for (ModelConfig m : models) {
            boolean sel = m.getId().equals(selectedModelId);
            TextView item = LineTheme.textMedium(ctx,
                    m.getName().length() > 0 ? m.getName() : m.getModelId(),
                    LineTheme.FONT_SM, sel ? LineTheme.TEXT_ON_COLOR : LineTheme.TEXT);
            item.setSingleLine(true);
            item.setEllipsize(TextUtils.TruncateAt.END);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setBackground(LineTheme.rounded(ctx, sel ? LineTheme.ACCENT : android.graphics.Color.TRANSPARENT, LineTheme.SHAPE_SM));
            LineTheme.padding(item, LineTheme.SM, 0, LineTheme.SM, 0);
            item.setClickable(true);
            final String mid = m.getId();
            item.setOnClickListener(v2 -> {
                if (modelPopup != null) modelPopup.dismiss();
                post(() -> { if (listener != null && !mid.equals(selectedModelId)) listener.onModelQuickSwitch(mid); });
            });
            content.addView(item, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));
        }
        relayoutModelPopup();
    }

    private void relayoutModelPopup() {
        if (modelPopup == null || !modelPopup.isShowing() || modelPopupAnchor == null) return;
        View content = modelPopup.getContentView();
        if (content == null) return;
        int popupWidth = modelPopup.getWidth();
        if (popupWidth <= 0) popupWidth = LineTheme.dp(getContext(), 220);
        content.measure(
                View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        modelPopup.update(popupWidth, content.getMeasuredHeight());
        showPopupAboveComposer(modelPopup, modelPopupAnchor, popupWidth, content.getMeasuredHeight(), false);
    }

    private void showModelSubMenu(View sourceRow, String sourceName, String baseUrl) {
        if (modelPopupContent != null) {
            fillModelSubList(modelPopupContent, sourceName, baseUrl);
        }
    }

    private void queryModelCount(String baseUrl, TextView queryBtn, Context ctx) {
        new Thread(() -> {
            try {
                int count = listener != null ? listener.onQueryModelCount(baseUrl) : 0;
                post(() -> {
                    queryBtn.setText(ctx.getString(R.string.composer_model_submenu_count_label, count));
                    android.widget.Toast.makeText(ctx, ctx.getString(R.string.composer_model_submenu_query_done_toast, count), android.widget.Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                post(() -> queryBtn.setText(R.string.toast_query_failed));
            }
        }, "linecode-model-query").start();
    }

    private LinearLayout modelOptionRow(Context ctx, ModelConfig model, boolean selected) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(LineTheme.dp(ctx, LineTheme.MD), 0, LineTheme.dp(ctx, LineTheme.MD), 0);
        row.setBackground(LineTheme.rounded(ctx, selected ? LineTheme.ACCENT : android.graphics.Color.TRANSPARENT, LineTheme.SHAPE_MD));
        row.setClickable(true);
        row.setOnClickListener(v -> {
            if (modelPopup != null) modelPopup.dismiss();
            final String mid = model.getId();
            post(() -> {
                if (!mid.equals(selectedModelId) && listener != null) {
                    listener.onModelQuickSwitch(mid);
                }
            });
        });
        View dot = new View(ctx);
        dot.setBackground(LineTheme.rounded(ctx, selected ? LineTheme.TEXT_ON_COLOR : LineTheme.BORDER, LineTheme.SHAPE_XS));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(LineTheme.dp(ctx, 7), LineTheme.dp(ctx, 7));
        dotParams.rightMargin = LineTheme.dp(ctx, LineTheme.SM);
        row.addView(dot, dotParams);
        String displayName = model.getName().length() > 0 ? model.getName() : model.getModelId();
        TextView name = LineTheme.textMedium(ctx, displayName, LineTheme.FONT_SM, selected ? LineTheme.TEXT_ON_COLOR : LineTheme.TEXT_SECONDARY);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(name, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView provider = LineTheme.text(ctx, model.getProviderLabel(), LineTheme.FONT_XS, selected ? LineTheme.TEXT_ON_COLOR : LineTheme.TEXT_TERTIARY, Typeface.NORMAL);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        pp.leftMargin = LineTheme.dp(ctx, LineTheme.SM);
        row.addView(provider, pp);
        return row;
    }

    private TextView modeOption(Context context, String label, String mode) {
        boolean selected = mode.equals(chatMode);
        TextView item = LineTheme.textMedium(context, label, LineTheme.FONT_SM,
                selected ? LineTheme.TEXT_ON_COLOR : LineTheme.TEXT_SECONDARY);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setSingleLine(true);
        item.setPadding(LineTheme.dp(context, LineTheme.MD), 0, LineTheme.dp(context, LineTheme.MD), 0);
        item.setBackground(LineTheme.rounded(context, selected ? LineTheme.ACCENT : android.graphics.Color.TRANSPARENT, LineTheme.SHAPE_MD));
        item.setClickable(true);
        item.setOnClickListener(v -> {
            if (modePopup != null) {
                modePopup.dismiss();
            }
            if (!mode.equals(chatMode) && listener != null) {
                listener.onModeChanged(mode);
            }
        });
        return item;
    }

    private String modeLabel(String mode) {
        if (ChatMode.CHAT.equals(mode)) {
            return "Chat";
        }
        if (ChatMode.PLAN.equals(mode)) {
            return "Plan";
        }
        if (ChatMode.CONTROL.equals(mode)) {
            return "\u63a7\u5236";
        }
        return "Agent";
    }
}
