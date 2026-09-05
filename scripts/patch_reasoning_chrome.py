#!/usr/bin/env python3
from pathlib import Path
p = Path("app/src/main/java/cn/lineai/ui/component/ComposerView.java")
text = p.read_text()
if "updateReasoningButton()" in text:
    print("already patched")
    raise SystemExit(0)

old_fields = """    private PopupWindow modePopup;
    private PopupWindow modelPopup;"""
new_fields = """    private PopupWindow modePopup;
    private PopupWindow modelPopup;
    private IconButtonView reasoningButton;
    private String currentReasoningEffort = AiBehaviorSettings.REASONING_MEDIUM;"""
if old_fields not in text:
    raise SystemExit("fields block missing")
text = text.replace(old_fields, new_fields, 1)

old_btn = """        IconButtonView reasoningButton = new IconButtonView(context, IconButtonView.BRAIN);
        reasoningButton.setIconColor(LineTheme.TEXT_SECONDARY);
        reasoningButton.setIconSizeDp(40, 20);
        LineTheme.attachStateLayer(reasoningButton);
        reasoningButton.setOnClickListener(v -> showReasoningPopup(reasoningButton));"""
new_btn = """        reasoningButton = new IconButtonView(context, IconButtonView.BRAIN);
        reasoningButton.setIconColor(LineTheme.TEXT_SECONDARY);
        reasoningButton.setIconSizeDp(40, 20);
        LineTheme.attachStateLayer(reasoningButton);
        reasoningButton.setOnClickListener(v -> showReasoningPopup(reasoningButton));"""
if old_btn not in text:
    raise SystemExit("reasoning button block missing")
text = text.replace(old_btn, new_btn, 1)

old_render = """        updateModelSelector();
        updateSendButton();
    }"""
new_render = """        currentReasoningEffort = AiBehaviorSettings.normalizeReasoningEffort(state.getReasoningEffort());
        updateModelSelector();
        updateReasoningButton();
        updateSendButton();
    }"""
if old_render not in text:
    raise SystemExit("render tail missing")
text = text.replace(old_render, new_render, 1)

old_item = """            TextView item = compactPopupItem(context, context.getString(labels[i]));
            item.setOnClickListener(v -> {
                popup.dismiss();
                if (listener != null) listener.onAiReasoningEffortChanged(effort);
            });"""
new_item = """            boolean selected = effort.equals(currentReasoningEffort);
            TextView item = compactPopupItem(context, context.getString(labels[i]));
            item.setTextColor(selected ? LineTheme.TEXT_ON_COLOR : LineTheme.TEXT);
            item.setBackground(LineTheme.rounded(context,
                    selected ? LineTheme.ACCENT : android.graphics.Color.TRANSPARENT, LineTheme.SHAPE_MD));
            item.setOnClickListener(v -> {
                popup.dismiss();
                currentReasoningEffort = effort;
                updateReasoningButton();
                if (listener != null) listener.onAiReasoningEffortChanged(effort);
            });"""
if old_item not in text:
    raise SystemExit("popup item block missing")
text = text.replace(old_item, new_item, 1)

helper = """
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
"""
marker = "    private void showPopupAboveComposer(PopupWindow popup, View anchor, int popupWidth, int popupHeight) {"
if marker not in text:
    raise SystemExit("helper marker missing")
text = text.replace(marker, helper + "\n" + marker, 1)
p.write_text(text)
assert "updateReasoningButton()" in text
print("patched", p.stat().st_size)
