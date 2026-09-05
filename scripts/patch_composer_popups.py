from pathlib import Path

p = Path("app/src/main/java/cn/lineai/ui/component/ComposerView.java")
s = p.read_text()

def must_replace(old, new, label):
    global s
    if old not in s:
        raise SystemExit("missing: " + label)
    s = s.replace(old, new, 1)

must_replace(
    "        popup.showAsDropDown(anchor, 0, LineTheme.dp(context, 6));\n    }\n\n    private void showPlusPopup(View anchor) {",
    "        showPopupAboveComposer(popup, anchor, LineTheme.dp(context, 196), 0, false);\n    }\n\n    private void showPlusPopup(View anchor) {",
    "reason showAsDropDown",
)
must_replace(
    "        popup.showAsDropDown(anchor, -LineTheme.dp(context, 170), LineTheme.dp(context, 6));\n    }",
    "        showPopupAboveComposer(popup, anchor, LineTheme.dp(context, 210), 0, true);\n    }",
    "plus showAsDropDown",
)
must_replace(
    '        TextView manageItem = LineTheme.textMedium(ctx, "\\u2699 \\u7ba1\\u7406\\u6a21\\u578b...", LineTheme.FONT_XS, LineTheme.TEXT_TERTIARY);',
    "        TextView manageItem = LineTheme.textMedium(ctx, ctx.getString(R.string.composer_model_manage), LineTheme.FONT_XS, LineTheme.TEXT_TERTIARY);",
    "manage chinese",
)
must_replace(
    "        int popupWidth = LineTheme.dp(ctx, 140);",
    "        int popupWidth = LineTheme.dp(ctx, 200);",
    "popup width",
)
must_replace(
    """        int[] location = new int[2];\n        anchor.getLocationOnScreen(location);\n        int screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;\n        int centeredX = location[0] + (anchor.getWidth() - popupWidth) / 2;\n        int popupX = Math.max(LineTheme.dp(ctx, LineTheme.SM), Math.min(centeredX, screenWidth - popupWidth - LineTheme.dp(ctx, LineTheme.SM)));\n        modelPopup.showAtLocation(this, Gravity.NO_GRAVITY, popupX, Math.max(0, location[1] - popupHeight - LineTheme.dp(ctx, 8)));\n    }""",
    """        showPopupAboveComposer(modelPopup, anchor, popupWidth, popupHeight, false);\n    }""",
    "model showAtLocation",
)
must_replace(
    """        // Position to the right of the source row\n        int[] loc = new int[2];\n        sourceRow.getLocationOnScreen(loc);\n        int subX = loc[0] + sourceRow.getWidth() + LineTheme.dp(ctx, 4);\n        int screenW = ctx.getResources().getDisplayMetrics().widthPixels;\n        if (subX + subWidth > screenW - LineTheme.dp(ctx, 8)) {\n            subX = loc[0] - subWidth - LineTheme.dp(ctx, 4);\n        }\n        modelSubPopup.showAtLocation(this, Gravity.NO_GRAVITY, subX, loc[1]);\n    }""",
    """        int[] loc = new int[2];\n        sourceRow.getLocationInWindow(loc);\n        int gap = LineTheme.dp(ctx, 8);\n        int screenW = ctx.getResources().getDisplayMetrics().widthPixels;\n        int subX = loc[0] + sourceRow.getWidth() + LineTheme.dp(ctx, 4);\n        if (subX + subWidth > screenW - gap) {\n            subX = Math.max(gap, loc[0] - subWidth - LineTheme.dp(ctx, 4));\n        }\n        int subY = loc[1];\n        int screenH = ctx.getResources().getDisplayMetrics().heightPixels;\n        if (subY + subHeight > screenH - gap) {\n            subY = Math.max(gap, loc[1] + sourceRow.getHeight() - subHeight);\n        }\n        modelSubPopup.setElevation(LineTheme.dp(ctx, 10));\n        modelSubPopup.showAtLocation(this, Gravity.NO_GRAVITY, subX, subY);\n    }""",
    "submenu position",
)

helper = '''\n    private void showPopupAboveComposer(PopupWindow popup, View anchor, int popupWidth, int popupHeight) {\n        showPopupAboveComposer(popup, anchor, popupWidth, popupHeight, false);\n    }\n\n    private void showPopupAboveComposer(PopupWindow popup, View anchor, int popupWidth, int popupHeight, boolean alignEnd) {\n        View content = popup.getContentView();\n        if (popupHeight <= 0 && content != null) {\n            content.measure(\n                    View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),\n                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));\n            popupHeight = content.getMeasuredHeight();\n        }\n        Context context = getContext();\n        int gap = LineTheme.dp(context, 8);\n        int[] composer = new int[2];\n        getLocationInWindow(composer);\n        int[] a = new int[2];\n        anchor.getLocationInWindow(a);\n        int screenW = getResources().getDisplayMetrics().widthPixels;\n        int x = alignEnd ? a[0] + anchor.getWidth() - popupWidth : a[0];\n        x = Math.max(gap, Math.min(x, screenW - popupWidth - gap));\n        int y = composer[1] - popupHeight - gap;\n        if (y < gap) {\n            y = Math.max(gap, a[1] - popupHeight - gap);\n        }\n        popup.setClippingEnabled(true);\n        popup.setElevation(LineTheme.dp(context, 10));\n        popup.showAtLocation(this, Gravity.NO_GRAVITY, x, y);\n    }\n\n'''
needle = "    private void showReasoningPopup(View anchor) {"
if needle not in s:
    raise SystemExit("missing showReasoningPopup")
s = s.replace(needle, helper + needle, 1)

p.write_text(s)
if "showAsDropDown" in s:
    raise SystemExit("showAsDropDown remains")
if "composer_model_manage" not in s:
    raise SystemExit("manage string missing")
print("patched", p.stat().st_size)
