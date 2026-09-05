#!/usr/bin/env python3
from pathlib import Path
import subprocess
path = Path("app/src/main/java/cn/lineai/ui/component/ComposerView.java")
text = subprocess.check_output([
    "git", "show",
    "a0bdfd276f7422bc294d44c605dbabe0d32b5fbe:app/src/main/java/cn/lineai/ui/component/ComposerView.java"
], text=True)
if "private PopupWindow modelSubPopup;" not in text.split("private SlashCommandPopup", 1)[0]:
    text = text.replace(
        "    private PopupWindow modelPopup;\n    private SlashCommandPopup",
        "    private PopupWindow modelPopup;\n    private PopupWindow modelSubPopup;\n    private SlashCommandPopup",
        1,
    )
old_show = """        popup.setClippingEnabled(true);
        popup.setElevation(LineTheme.dp(context, 10));
        popup.showAtLocation(this, Gravity.NO_GRAVITY, x, y);"""
new_show = """        popup.setClippingEnabled(true);
        popup.setElevation(LineTheme.dp(context, 10));
        if (popup.isShowing()) {
            popup.update(x, y, popupWidth, Math.max(popupHeight, 1));
        } else {
            popup.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
        }"""
if old_show not in text:
    raise SystemExit("show helper not found")
text = text.replace(old_show, new_show, 1)
text = text.replace(
    """    private void showReasoningPopup(View anchor) {
        if (streaming) return;
        dismissSlashPopup();""",
    """    private void showReasoningPopup(View anchor) {
        if (streaming) return;
        dismissComposerPopups();""",
    1,
)
text = text.replace(
    """    private void showPlusPopup(View anchor) {
        if (streaming) return;
        dismissSlashPopup();""",
    """    private void showPlusPopup(View anchor) {
        if (streaming) return;
        dismissComposerPopups();""",
    1,
)
idx = text.find("    private void showModelPopup(View anchor)")
start = text.rfind("    private PopupWindow modelSubPopup;", 0, idx)
if start < 0:
    start = idx
end = text.find("    private LinearLayout modelOptionRow(")
if start < 0 or end < 0:
    raise SystemExit(f"markers missing {start} {end}")
NEW = Path("scripts/model_popup_block.java").read_text()
text = text[:start] + NEW + text[end:]
path.write_text(text)
assert "fillModelSubList" in text
assert "PLACEHOLDER" not in text
print("patched", path, "bytes", path.stat().st_size)
