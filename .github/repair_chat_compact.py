import base64
import json
import os
import urllib.parse
import urllib.request

REPO = os.environ["REPO"]
BRANCH = os.environ.get("BRANCH", "feat/chat-compact")
TOKEN = os.environ["GITHUB_TOKEN"]
SOURCE_SHA = "8dc4fe1ae32056452294120d310fab7bcded60f4"
COMPOSER = "app/src/main/java/cn/lineai/ui/component/ComposerView.java"
MAIN = "app/src/main/java/cn/lineai/ui/MainChatView.java"


def request(url, method="GET", payload=None, auth=True):
    headers = {
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "line-chat-compact-repair",
    }
    if auth:
        headers["Authorization"] = f"Bearer {TOKEN}"
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req) as response:
        raw = response.read()
        return raw


def raw_file(path, ref):
    url = f"https://raw.githubusercontent.com/{REPO}/{ref}/{path}"
    return request(url, auth=False).decode("utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


def update_full(path, text, message):
    quoted = urllib.parse.quote(path, safe="/")
    ref = urllib.parse.quote(BRANCH, safe="")
    meta_url = f"https://api.github.com/repos/{REPO}/contents/{quoted}?ref={ref}"
    meta = json.loads(request(meta_url).decode("utf-8"))
    current_sha = meta.get("sha")
    if not current_sha:
        raise RuntimeError(f"Cannot resolve current blob SHA for {path}")
    payload = {
        "message": message,
        "content": base64.b64encode(text.encode("utf-8")).decode("ascii"),
        "sha": current_sha,
        "branch": BRANCH,
    }
    result = json.loads(request(
        f"https://api.github.com/repos/{REPO}/contents/{quoted}",
        method="PUT",
        payload=payload,
    ).decode("utf-8"))
    commit_sha = result.get("commit", {}).get("sha", "")
    print(f"UPDATED {path}: commit={commit_sha} chars={len(text)}")
    return commit_sha


# --- ComposerView: always rebuild from the exact raw SHA requested by the user. ---
composer = raw_file(COMPOSER, SOURCE_SHA)
if len(composer) < 20000:
    raise RuntimeError(f"ComposerView from {SOURCE_SHA} is unexpectedly short: {len(composer)}")
if not composer.startswith("package cn.lineai.ui.component;"):
    raise RuntimeError("ComposerView raw source has an unexpected header")
if composer.strip() in {"PLACEHOLDER", "see-file"}:
    raise RuntimeError("Refusing placeholder ComposerView source")

composer = replace_once(
    composer,
    "        void onImagePickerClick();\n\n        void onModeChanged(String mode);",
    "        void onImagePickerClick();\n\n        void onCompactClick();\n\n        void onModeChanged(String mode);",
    "ComposerView.Listener.onCompactClick",
)

# Remove the old top metaRow (model chip + context label + divider). The model
# selector itself is rebuilt below as the compact sparkles button.
meta_start = composer.index("        LinearLayout metaRow = new LinearLayout(context);")
meta_end_marker = "        panel.addView(divider, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1));\n"
meta_end = composer.index(meta_end_marker, meta_start) + len(meta_end_marker)
compact_model = """        modelSelectorButton = new LinearLayout(context);
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

"""
composer = composer[:meta_start] + compact_model + composer[meta_end:]
composer = replace_once(
    composer,
    "        panel.setMinimumHeight(LineTheme.dp(context, 148));",
    "        panel.setMinimumHeight(LineTheme.dp(context, 112));",
    "compact panel height",
)

# Input row: text + expand icon. Expand toggles between normal and tall editor.
input_anchor = """        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        inputRow.addView(input, inputParams);
"""
input_replacement = input_anchor + """
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
"""
composer = replace_once(composer, input_anchor, input_replacement, "input expand button")

# Replace bottom attach/image/mode-chip row with:
# sparkles(model), brain(reasoning), spacer, plus menu, send.
row_start = composer.index("        LinearLayout modeRow = new LinearLayout(context);")
row_end_marker = "        modeRow.addView(sendButton, sendParams);\n"
row_end = composer.index(row_end_marker, row_start) + len(row_end_marker)
compact_toolbar = """        LinearLayout toolbarRow = new LinearLayout(context);
        toolbarRow.setOrientation(HORIZONTAL);
        toolbarRow.setGravity(Gravity.CENTER_VERTICAL);
        LineTheme.padding(toolbarRow, LineTheme.SM, 0, LineTheme.SM, LineTheme.SM);
        panel.addView(toolbarRow, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams modelParams = new LinearLayout.LayoutParams(
                LineTheme.dp(context, 40), LineTheme.dp(context, 40));
        modelParams.rightMargin = LineTheme.dp(context, LineTheme.XS);
        toolbarRow.addView(modelSelectorButton, modelParams);

        IconButtonView reasoningButton = new IconButtonView(context, IconButtonView.BRAIN);
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
"""
composer = composer[:row_start] + compact_toolbar + composer[row_end:]

# The removed metadata/mode-chip controls must no longer be touched during render.
composer = replace_once(composer, "        modelText.setText(state.getModelLabel());\n", "", "model label render")
composer = replace_once(
    composer,
    "        contextText.setText(state.getContextLabel());\n        contextText.setTextColor(state.getContextPercent() >= 80 ? LineTheme.WARNING : LineTheme.TEXT_TERTIARY);\n",
    "",
    "context meta render",
)
composer = replace_once(composer, "        updateModeButtons();\n", "", "bottom mode state update")
composer = replace_once(
    composer,
    "        modelChevron.setIconColor(streaming ? LineTheme.TEXT_TERTIARY : LineTheme.TEXT_SECONDARY);\n",
    "",
    "model chevron state update",
)

# Compact reasoning and plus popups. Existing model popup is reused unchanged.
helper_anchor = "    private PopupWindow modelSubPopup;\n"
helpers = r'''    private void showReasoningPopup(View anchor) {
        if (streaming) return;
        dismissSlashPopup();
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
            TextView item = compactPopupItem(context, context.getString(labels[i]));
            item.setOnClickListener(v -> {
                popup.dismiss();
                if (listener != null) listener.onAiReasoningEffortChanged(effort);
            });
            content.addView(item, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LineTheme.dp(context, 40)));
        }
        popup.showAsDropDown(anchor, 0, LineTheme.dp(context, 6));
    }

    private void showPlusPopup(View anchor) {
        if (streaming) return;
        dismissSlashPopup();
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
        popup.showAsDropDown(anchor, -LineTheme.dp(context, 170), LineTheme.dp(context, 6));
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

'''
composer = replace_once(composer, helper_anchor, helpers + helper_anchor, "compact popup helpers")

# Guardrails before the full create-or-update.
if "LinearLayout metaRow" in composer:
    raise RuntimeError("metaRow is still present")
for required in (
    "void onCompactClick();",
    "IconButtonView.SPARKLES",
    "IconButtonView.BRAIN",
    "IconButtonView.EXPAND",
    "listener.onCompactClick()",
    "showReasoningPopup",
    "showPlusPopup",
):
    if required not in composer:
        raise RuntimeError(f"ComposerView missing required fragment: {required}")
if composer.strip() in {"PLACEHOLDER", "see-file"}:
    raise RuntimeError("ComposerView became a placeholder")

# --- MainChatView: patch current branch source only where requested. ---
main = raw_file(MAIN, BRANCH)
if len(main) < 20000 or not main.startswith("package cn.lineai.ui;"):
    raise RuntimeError("Unexpected MainChatView source")

header_anchor = """            @Override
            public void onProjectClick() {
                MainChatView.this.presenter.onProjectClick();
            }

"""
header_insert = header_anchor + """            @Override
            public void onModeChanged(String mode) {
                MainChatView.this.presenter.onChatModeChanged(mode);
            }

"""
if "public void onModeChanged(String mode)" not in main.split("composerView = new ComposerView", 1)[0]:
    main = replace_once(main, header_anchor, header_insert, "HeaderView.Listener.onModeChanged")

image_anchor = """            @Override
            public void onImagePickerClick() {
                MainChatView.this.presenter.onImagePickerRequested();
            }

"""
compact_insert = image_anchor + """            @Override
            public void onCompactClick() {
                MainChatView.this.presenter.onSheetOptionSelected("compact");
            }

"""
if "MainChatView.this.presenter.onSheetOptionSelected(\"compact\");" not in main:
    main = replace_once(main, image_anchor, compact_insert, "ComposerView.Listener.onCompactClick")

header_section = main.split("composerView = new ComposerView", 1)[0]
if "public void onModeChanged(String mode)" not in header_section or "presenter.onChatModeChanged(mode);" not in header_section:
    raise RuntimeError("HeaderView.Listener mode callback is missing")
if "MainChatView.this.presenter.onSheetOptionSelected(\"compact\");" not in main:
    raise RuntimeError("Compact presenter callback is missing")

composer_commit = update_full(COMPOSER, composer, "fix(ui): restore full ComposerView and compact toolbar")
main_commit = update_full(MAIN, main, "fix(ui): wire header mode and compact history action")

# Trigger the existing test-build-apk workflow on the now-updated branch.
dispatch_url = f"https://api.github.com/repos/{REPO}/actions/workflows/test-build-apk.yml/dispatches"
request(dispatch_url, method="POST", payload={"ref": BRANCH, "inputs": {"note": "feat/chat-compact composer repair"}})
print(f"DISPATCHED test-build-apk.yml on {BRANCH}; composer={composer_commit}; main={main_commit}")
