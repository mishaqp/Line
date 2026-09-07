package cn.lineai.ui.component;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import cn.lineai.model.InputSettings;
import org.junit.Assert;
import org.junit.Test;

public final class EnterKeyImePolicyTest {
    @Test
    public void sendModeUsesImeMultilineAndSendAction() {
        Assert.assertEquals(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE,
                EnterKeyImePolicy.rawInputType(InputSettings.ENTER_SEND)
        );
        Assert.assertEquals(EditorInfo.IME_ACTION_SEND, EnterKeyImePolicy.imeOptions(InputSettings.ENTER_SEND));
        Assert.assertFalse(
                (EnterKeyImePolicy.rawInputType(InputSettings.ENTER_SEND)
                        & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        );
    }

    @Test
    public void newlineModeUsesMultilineAndNoImeAction() {
        Assert.assertEquals(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                EnterKeyImePolicy.rawInputType(InputSettings.ENTER_NEWLINE)
        );
        Assert.assertEquals(EditorInfo.IME_ACTION_NONE, EnterKeyImePolicy.imeOptions(InputSettings.ENTER_NEWLINE));
    }

    @Test
    public void sendModeConsumesLoneImeNewlineButNotPaste() {
        Assert.assertTrue(
                EnterKeyImePolicy.shouldConsumeInsertedNewline(
                        InputSettings.ENTER_SEND, "hi\n", 2, 1)
        );
        Assert.assertTrue(
                EnterKeyImePolicy.shouldConsumeInsertedNewline(
                        InputSettings.ENTER_SEND, "hi\r\n", 2, 2)
        );
        Assert.assertFalse(
                EnterKeyImePolicy.shouldConsumeInsertedNewline(
                        InputSettings.ENTER_SEND, "hi\nthere", 2, 6)
        );
        Assert.assertFalse(
                EnterKeyImePolicy.shouldConsumeInsertedNewline(
                        InputSettings.ENTER_NEWLINE, "hi\n", 2, 1)
        );
    }
}
