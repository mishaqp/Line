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
import android.view.inputmethod.InputMethodManager;
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
