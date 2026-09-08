package cn.lineai.ui.component;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import cn.lineai.model.InputSettings;

/**
 * Maps the persisted Enter-key setting onto EditText IME flags.
 * Software keyboards ignore {@link EditorInfo#IME_ACTION_SEND} when
 * {@link InputType#TYPE_TEXT_FLAG_MULTI_LINE} is set, so send-mode uses
 * {@link InputType#TYPE_TEXT_FLAG_IME_MULTI_LINE} instead and treats a lone
 * inserted newline as submit.
 */
public final class EnterKeyImePolicy {
    private EnterKeyImePolicy() {
    }

    public static int rawInputType(String behavior) {
        int type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        if (InputSettings.ENTER_NEWLINE.equals(InputSettings.normalizeEnterKeyBehavior(behavior))) {
            return type | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        }
        return type | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE;
    }

    public static int imeOptions(String behavior) {
        return InputSettings.ENTER_SEND.equals(InputSettings.normalizeEnterKeyBehavior(behavior))
                ? EditorInfo.IME_ACTION_SEND
                : EditorInfo.IME_ACTION_NONE;
    }

    public static boolean shouldConsumeInsertedNewline(
            String behavior,
            CharSequence text,
            int start,
            int count
    ) {
        if (!InputSettings.ENTER_SEND.equals(InputSettings.normalizeEnterKeyBehavior(behavior))) {
            return false;
        }
        if (text == null || count <= 0 || start < 0 || start + count > text.length()) {
            return false;
        }
        if (count == 1) {
            return text.charAt(start) == '\n';
        }
        return count == 2
                && text.charAt(start) == '\r'
                && text.charAt(start + 1) == '\n';
    }
}
