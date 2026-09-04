#!/usr/bin/env python3
"""Static verifier for the Material Design 3 chat-screen rework.

Companion to `verify_m3_tokens.py` (which guards the token layer from PR #4). This script
guards the *chat* work: bubbles, action bars, composer, FAB and empty state must be built
from the M3 shape / type tokens instead of hardcoded radii and font sizes, the composer
decomposition must keep its public contract, and the new strings must exist in all three
locales.

Runs without a JDK or Android SDK: it parses the real sources with `javalang` and inspects
the resources with ElementTree. CI stays the source of truth; this is a fast pre-push guard.
"""
import re
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

try:
    import javalang
except ImportError:  # pragma: no cover - developer convenience
    print("javalang is required: pip3 install --user --break-system-packages javalang")
    sys.exit(2)

ROOT = Path(__file__).resolve().parent.parent
CHECKS = []

SHAPE_CALLS = ("LineTheme.rounded(", "LineTheme.roundedStroke(", "LineTheme.roundedTop(")


def check(name, condition, detail=""):
    CHECKS.append((name, bool(condition), detail))
    return bool(condition)


def read(relative):
    return (ROOT / relative).read_text(encoding="utf-8")


def parses(relative):
    """The file is valid Java 11 as far as javalang can tell."""
    try:
        javalang.parse.parse(read(relative))
        return True, ""
    except Exception as error:  # noqa: BLE001 - report whatever javalang raised
        return False, str(error)


def split_args(text):
    """Split a top-level argument list (parens already stripped) on commas."""
    args, depth, current = [], 0, []
    for char in text:
        if char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
        if char == "," and depth == 0:
            args.append("".join(current))
            current = []
        else:
            current.append(char)
    args.append("".join(current))
    return args


def literal_radii(source):
    """Every numeric literal passed as the radius argument of a rounded* helper."""
    found, index = [], 0
    while True:
        positions = [p for p in (source.find(call, index) for call in SHAPE_CALLS) if p != -1]
        if not positions:
            return found
        start = min(positions)
        open_paren = source.index("(", start)
        depth, cursor = 1, open_paren + 1
        while depth:
            if source[cursor] in "([{":
                depth += 1
            elif source[cursor] in ")]}":
                depth -= 1
            cursor += 1
        args = split_args(source[open_paren + 1:cursor - 1])
        if len(args) >= 3 and re.fullmatch(r"\s*\d+\s*", args[2]):
            found.append(int(args[2].strip()))
        index = cursor


# ------------------------------------------------------- ui-theme: bubbles + LineCards
theme = read("ui-theme/src/main/java/cn/lineai/ui/theme/LineTheme.java")
check("LineTheme exposes bubbleCornerRadii(float,float,boolean)",
      "public static float[] bubbleCornerRadii(float largePx, float smallPx, boolean tailOnEnd)" in theme)
check("LineTheme keeps userBubble(Context)", "public static GradientDrawable userBubble(Context context)" in theme)
check("LineTheme adds assistantBubble(Context)",
      "public static GradientDrawable assistantBubble(Context context)" in theme)
check("bubbles are built from a large radius plus a SHAPE_XS tail",
      "bubbleCornerRadii(dp(context, largeDp), dp(context, SHAPE_XS), tailOnEnd)" in theme)
check("userBubble keeps the tail on the end side",
      "return bubble(context, USER_BUBBLE, true, SHAPE_MD);" in theme)
check("assistantBubble mirrors it", "return bubble(context, AI_BUBBLE, false);" in theme)

ok, detail = parses("ui-theme/src/main/java/cn/lineai/ui/theme/LineTheme.java")
check("LineTheme.java parses", ok, detail)

cards_path = "ui-theme/src/main/java/cn/lineai/ui/theme/LineCards.java"
cards = read(cards_path)
ok, detail = parses(cards_path)
check("LineCards.java parses", ok, detail)
for helper in ("cardBackground", "card", "clickableCard", "pillBackground", "segmentBackground",
               "primaryButton", "secondaryButton", "textButton", "dangerButton",
               "title", "desc", "badge", "applyFab"):
    check("LineCards exposes %s()" % helper, ("%s(" % helper) in cards)
check("LineCards lives in :ui-theme (no :app imports)", "cn.lineai.ui.component" not in cards)
check("LineCards cards use SHAPE_MD", "LineTheme.SHAPE_MD" in cards)
check("LineCards pills use SHAPE_FULL", "LineTheme.SHAPE_FULL" in cards)
check("LineCards segments use SHAPE_SM", "LineTheme.SHAPE_SM" in cards)
check("LineCards FAB uses SHAPE_LG", "LineTheme.SHAPE_LG" in cards)
check("LineCards buttons attach state layers", cards.count("attachStateLayer") >= 4)

# ------------------------------------------------------------------- Chat: bubbles
user_view = read("app/src/main/java/cn/lineai/ui/component/UserMessageView.java")
check("UserMessageView keeps the M3 user bubble", "LineTheme.userBubble(context)" in user_view)
check("UserMessageView text uses a type token",
      "LineTheme.TYPE_TITLE, LineTheme.TEXT_ON_COLOR" in user_view)
check("UserMessageView attachment chips use TYPE_BODY_SMALL",
      "LineTheme.TYPE_BODY_SMALL" in user_view)
check("UserMessageView chips are SHAPE_FULL pills", "LineCards.pillBackground" in user_view)

assistant_view = read("app/src/main/java/cn/lineai/ui/component/AssistantMessageView.java")
check("AssistantMessageView renders the mirrored AI bubble",
      "LineTheme.assistantBubble(context)" in assistant_view)
check("AI bubble stays MATCH_PARENT for code blocks / tables",
      "addView(contentView, new LayoutParams(LayoutParams.MATCH_PARENT" in assistant_view)
for contract in ("ToolReviewListener", "MarkdownLinkHandler", "MessageActionListener",
                 "ThinkingBlockView", "ToolCallBlockView"):
    check("AssistantMessageView keeps %s wiring" % contract, contract in assistant_view)

# --------------------------------------------------------------- Chat: action bar
action_bar = read("app/src/main/java/cn/lineai/ui/component/MessageActionBarView.java")
check("MessageActionBarView icons carry a state layer", "LineTheme.attachStateLayer(icon)" in action_bar)
check("MessageActionBarView icons are SHAPE_FULL containers",
      "LineCards.pillBackground(context, Color.TRANSPARENT)" in action_bar)
check("MessageActionBarView icons are focusable", "icon.setFocusable(true)" in action_bar)
for listener in ("ActionListener", "SelectListener", "RecallListener"):
    check("MessageActionBarView keeps %s" % listener, "interface %s" % listener in action_bar)
check("action bar carries no container plate",
      "CONTAINER_ALPHA" not in action_bar and "setBackground(LineTheme.rounded(" not in action_bar,
      "a filled pill under every message competes with the bubble")
check("action bar icons are readable (TEXT_SECONDARY, not TERTIARY)",
      "icon.setIconColor(LineTheme.TEXT_SECONDARY)" in action_bar
      and "TEXT_TERTIARY" not in action_bar)
check("action row is compact but still touchable",
      "ROW_HEIGHT_DP = 26" in action_bar and "ICON_WIDTH_DP = 27" in action_bar)
check("action row has no extra horizontal padding",
      "LineTheme.padding(this, 0, 0, 0, 0)" in action_bar)

# The message views must not pin the action row to the old 22dp height, which would
# clip the taller touch targets.
for relative in ("app/src/main/java/cn/lineai/ui/component/UserMessageView.java",
                 "app/src/main/java/cn/lineai/ui/component/AssistantMessageView.java"):
    source = read(relative)
    check("%s lets the action row wrap its content" % Path(relative).name,
          "LineTheme.dp(context, 22))" not in source)
    check("%s scales the bubble padding" % Path(relative).name,
          "LineTheme.chatPadding(" in source)

# A one-word outgoing bubble must not read as a fat capsule.
user_view = read("app/src/main/java/cn/lineai/ui/component/UserMessageView.java")
check("outgoing bubble padding is trimmed",
      "LineTheme.chatPadding(contentText, LineTheme.MD, LineTheme.SM, LineTheme.MD, LineTheme.SM)" in user_view)
check("outgoing bubble no longer runs to the edge",
      "availableWidth * 0.74f" in user_view)
theme_for_bubble = read("ui-theme/src/main/java/cn/lineai/ui/theme/LineTheme.java")
check("outgoing bubble uses its own smaller radius",
      "bubble(context, USER_BUBBLE, true, SHAPE_MD)" in theme_for_bubble)
check("incoming bubble keeps SHAPE_LG",
      "bubble(context, color, tailOnEnd, SHAPE_LG)" in theme_for_bubble)

# ------------------------------------------------------------ Tool cards: error state
base_card = read("tool-ui/src/main/java/cn/lineai/tool/ui/view/BaseToolCallView.java")
check("tool cards expose a state-aware background",
      "protected void applyCardBackground(boolean error)" in base_card)
check("the failed card is a tonal container, not a flood of DANGER",
      "ERROR_FILL_ALPHA" in base_card and "ERROR_STROKE_ALPHA" in base_card)
card_views = sorted((ROOT / "tool-ui/src/main/java/cn/lineai/tool/ui/view").glob("ToolCall*View.java"))
check("every tool card has views to check", len(card_views) >= 7)
for card in card_views:
    src = read("tool-ui/src/main/java/cn/lineai/tool/ui/view/%s" % card.name)
    if "boolean error" not in src:
        continue
    check("%s applies the error container" % card.stem,
          "applyCardBackground(error)" in src)
read_card = read("tool-ui/src/main/java/cn/lineai/tool/ui/view/ToolCallReadView.java")
check("failed read/list cards keep their label and path legible",
      "int actionColor = LineTheme.TEXT_SECONDARY;" in read_card
      and "ForegroundColorSpan(error ? LineTheme.DANGER" not in read_card)
generic_card = read("tool-ui/src/main/java/cn/lineai/tool/ui/view/ToolCallGenericView.java")
check("failed generic cards keep the tool name legible",
      "LineTheme.FONT_SM, LineTheme.TEXT, Typeface.NORMAL)" in generic_card)

# ----------------------------------------------------- Chat: list, FAB, empty state
list_view = read("app/src/main/java/cn/lineai/ui/component/ChatMessageListView.java")
check("scroll-to-bottom is a real M3 FAB",
      "LineCards.applyFab(scrollToBottomButton, LineTheme.ACCENT)" in list_view)
check("empty-state CTA reuses the shared pill buttons",
      "LineCards.primaryButton(context, label)" in list_view
      and "LineCards.secondaryButton(context, label)" in list_view)
check("empty-state title uses TYPE_HEADLINE", "LineTheme.TYPE_HEADLINE, LineTheme.TEXT, Typeface.BOLD" in list_view)
check("empty-state body uses TYPE_BODY", "LineTheme.TYPE_BODY," in list_view)
check("multi-select export button has a state layer",
      "attachStateLayer(exportButton" in list_view)
for contract in ("ChatUiState", "MessageActionListener", "MultiSelectListener",
                 "ToolReviewListener", "MarkdownLinkHandler", "EmptyStateListener"):
    check("ChatMessageListView keeps %s" % contract, contract in list_view)
check("ChatMessageListView keeps its row cache", "rowCache" in list_view and "trimCache" in list_view)

# ------------------------------------------------------------------ Chat: composer
composer_path = "app/src/main/java/cn/lineai/ui/component/ComposerView.java"
composer = read(composer_path)
ok, detail = parses(composer_path)
check("ComposerView.java parses", ok, detail)
composer_lines = composer.count("\n") + 1
check("ComposerView shrank below 1300 lines (was 1444)", composer_lines < 1300,
      "%d lines" % composer_lines)
check("composer panel uses SHAPE_XL", "LineTheme.SHAPE_XL, LineTheme.BORDER" in composer)
check("attach button is a pill with a state layer",
      "applyTonalIconButton(attachButton)" in composer)
check("image button is a pill with a state layer", "applyTonalIconButton(imageButton)" in composer)
check("send button uses pill backgrounds", "LineCards.pillBackground(getContext()" in composer)
check("send-button queue colors are named constants",
      "QUEUE_STOP_COLOR" in composer and "QUEUE_APPEND_COLOR" in composer)
check("model selector has a state layer", "attachStateLayer(modelSelectorButton)" in composer)
check("mode selector has a state layer", "attachStateLayer(modeSelectorButton)" in composer)

# Several palettes define inputBg == surfaceLight, so a SURFACE_LIGHT pill drawn on the
# composer panel is invisible. Containers sitting on the panel must not use it.
check("attach button is a visible tonal icon button",
      "LineCards.applyTonalIconButton(attachButton)" in composer)
check("image button is a visible tonal icon button",
      "LineCards.applyTonalIconButton(imageButton)" in composer)
check("model selector is an outlined chip", "LineCards.chipBackground(context)" in composer)
check("mode selector is an outlined chip", "LineCards.chipBackground(getContext())" in composer)
check("idle send button does not use SURFACE_LIGHT",
      "hasContent ? LineTheme.ACCENT : LineTheme.BORDER_LIGHT" in composer)
check("no control on the composer panel uses SURFACE_LIGHT",
      "LineTheme.SURFACE_LIGHT" not in composer)

# Public API of the composer must be unchanged by the decomposition.
PUBLIC_API = [
    "public void setListener(Listener listener)",
    "public void setQuoteDismissListener(QuoteDismissListener listener)",
    "public void onImagePicked(Uri uri, String base64, String mimeType, String displayName)",
    "public void clearImage()",
    "public boolean hasPendingImage()",
    "public void showQuote(String previewText)",
    "public void hideQuote()",
    "public void setDraft(String text)",
    "public void setDraft(String text, List<InputAttachment> nextAttachments)",
    "public List<InputAttachment> getAttachments()",
    "public List<String> selectedAttachmentPaths(String source)",
    "public void toggleAttachment(InputAttachment attachment)",
    "public void render(ChatUiState state)",
    "public void setQuoteText(String text)",
    "public void clearQuote()",
    "public void dismissSlashPopup()",
]
for signature in PUBLIC_API:
    check("ComposerView keeps `%s`" % signature.replace("public ", ""), signature in composer)
check("ComposerView still implements QuotePreview",
      "implements QuoteController.QuotePreview" in composer)
for callback in ("onSend", "onSendWithImage", "onAttachClick", "onImagePickerClick",
                 "onModeChanged", "onStop", "onModelQuickSwitch", "onModelManageClick",
                 "onAiReasoningEffortChanged", "onQueryModelCount"):
    check("ComposerView.Listener keeps %s" % callback, callback in composer)

# ------------------------------------------------- Composer decomposition components
for relative, needles in {
    "app/src/main/java/cn/lineai/ui/component/ComposerQueue.java": [
        "MAX_VISIBLE_ROWS", "PREVIEW_MAX_CHARS", "static String truncate(",
        "int overflowCount()", "Item poll()",
    ],
    "app/src/main/java/cn/lineai/ui/component/ComposerAttachmentStrip.java": [
        "pathsForSource", "toggle(", "replaceAll(", "LineCards.pillBackground",
    ],
    "app/src/main/java/cn/lineai/ui/component/ComposerImagePreview.java": [
        "void show(", "void clear()", "boolean hasImage()", "LineCards.cardBackground",
    ],
    "app/src/main/java/cn/lineai/ui/component/ComposerPendingQueueView.java": [
        "void refresh()", "R.string.composer_queue_overflow", "LineTheme.SHAPE_SM",
    ],
}.items():
    ok, detail = parses(relative)
    check("%s parses" % Path(relative).name, ok, detail)
    source = read(relative)
    for needle in needles:
        check("%s contains `%s`" % (Path(relative).name, needle), needle in source)

queue = read("app/src/main/java/cn/lineai/ui/component/ComposerQueue.java")
check("ComposerQueue stays Android-free (JVM testable)",
      "import android." not in queue)
check("ComposerQueue collapses whitespace before truncating",
      'replaceAll("\\\\s+", " ")' in queue)

# ------------------------------------------------ No magic radii left in the UI layer
offenders = {}
for path in sorted((ROOT / "app/src/main/java/cn/lineai/ui").rglob("*.java")):
    radii = literal_radii(path.read_text(encoding="utf-8"))
    if radii:
        offenders[path.relative_to(ROOT).as_posix()] = radii
check("no hardcoded corner radii under app/.../ui/", not offenders,
      "; ".join("%s:%s" % (name, radii) for name, radii in list(offenders.items())[:5]))

# ------------------------------------------------------------- Shared card helpers
check("LineCards exposes applyIconButton/applyTonalIconButton/chipBackground",
      "applyIconButton(" in cards and "applyTonalIconButton(" in cards and "chipBackground(" in cards)

# Palette-independent colors: no raw ARGB literals left in the chat components.
for relative in ("app/src/main/java/cn/lineai/ui/component/ComposerView.java",
                 "app/src/main/java/cn/lineai/ui/component/TextSelectionDialog.java"):
    source = read(relative)
    raw = re.findall(r"setBackgroundColor\(0x[0-9A-Fa-f]{8}\)", source)
    check("%s has no hardcoded background color" % Path(relative).name, not raw, str(raw))

card_helper = read("app/src/main/java/cn/lineai/ui/component/CardViewHelper.java")
check("CardViewHelper delegates to LineCards",
      "LineCards.cardBackground(context)" in card_helper
      and "LineCards.title(" in card_helper
      and "LineCards.desc(" in card_helper
      and "LineCards.badge(" in card_helper)
check("CardViewHelper cards carry a state layer", "attachStateLayer(card)" in card_helper)

action_row = read("app/src/main/java/cn/lineai/ui/component/ActionRowView.java")
check("ActionRowView carries a state layer when clickable", "attachStateLayer(this," in action_row)
check("ActionRowView uses type tokens",
      "LineTheme.TYPE_TITLE" in action_row and "LineTheme.TYPE_BODY_SMALL" in action_row)

# --------------------------------------------------------------- Strings / locales
NEW_STRINGS = ["composer_image_default_name", "composer_queue_overflow", "composer_queue_remove_desc"]
locale_names = {}
for locale in ["values", "values-ru", "values-zh"]:
    path = ROOT / ("app/src/main/res/%s/strings.xml" % locale)
    try:
        tree = ElementTree.parse(path)
    except ElementTree.ParseError as error:
        check("%s strings.xml well-formed" % locale, False, str(error))
        continue
    check("%s strings.xml well-formed" % locale, True)
    elements = list(tree.iter("string"))
    names = [element.get("name") for element in elements]
    locale_names[locale] = set(names)
    check("%s has no duplicate string names" % locale, len(names) == len(set(names)))
    for needed in NEW_STRINGS:
        check("%s declares %s" % (locale, needed), needed in names)
    for element in elements:
        if element.get("name") in NEW_STRINGS:
            text = element.text or ""
            check("%s %s is aapt2-safe (no stray backslash)" % (locale, element.get("name")),
                  "\\" not in text)
            if element.get("name") == "composer_queue_overflow":
                check("%s composer_queue_overflow keeps the %%1$d placeholder" % locale,
                      "%1$d" in text)

if len(locale_names) == 3:
    check("all three locales declare the same string set",
          locale_names["values"] == locale_names["values-ru"] == locale_names["values-zh"],
          "symmetric difference: %s" % sorted(
              (locale_names["values"] ^ locale_names["values-ru"])
              | (locale_names["values"] ^ locale_names["values-zh"]))[:5])

# ----------------------------------------------------------------------- Tests / CI
queue_test = "app/src/test/java/cn/lineai/ui/component/ComposerQueueTest.java"
ok, detail = parses(queue_test)
check("ComposerQueueTest parses", ok, detail)
check("ComposerQueueTest imports no Robolectric/Mockito",
      not re.search(r"import\s+org\.(robolectric|mockito)", read(queue_test)))

bubble_test = "ui-theme/src/test/java/cn/lineai/ui/theme/LineThemeBubbleShapeTest.java"
ok, detail = parses(bubble_test)
check("LineThemeBubbleShapeTest parses", ok, detail)
check("LineThemeBubbleShapeTest imports no Robolectric/Mockito",
      not re.search(r"import\s+org\.(robolectric|mockito)", read(bubble_test)))

app_gradle = read("app/build.gradle.kts")
for banned in ("com.google.android.material", "androidx.appcompat", "robolectric", "mockito"):
    check("app/build.gradle.kts adds no %s dependency" % banned, banned not in app_gradle.lower())
theme_gradle = read("ui-theme/build.gradle.kts")
for banned in ("com.google.android.material", "robolectric"):
    check("ui-theme/build.gradle.kts adds no %s dependency" % banned, banned not in theme_gradle.lower())

ci = read(".github/workflows/ci.yml")
check("CI runs :app:testDebugUnitTest", ":app:testDebugUnitTest" in ci)
check("CI runs :ui-theme:testDebugUnitTest", ":ui-theme:testDebugUnitTest" in ci)
check("CI runs lint", ":app:lintDebug" in ci)
check("CI builds both APKs",
      ":app:assembleDebug" in ci and ":app:assembleDebugUserCert" in ci)
check("CI never pushes to branches", "git push" not in ci)

# ------------------------------------------------------------------ Chat scale feature
scale_model = "core-model/src/main/java/cn/lineai/model/ChatScale.java"
ok, detail = parses(scale_model)
check("ChatScale parses", ok, detail)
scale_src = read(scale_model)
check("ChatScale is free of Android types",
      "android." not in scale_src and "androidx." not in scale_src)
for token in ("MODE_ULTRA_COMPACT", "MODE_COMPACT", "MODE_NORMAL", "MODE_LARGE", "MIN_SCALE", "MAX_SCALE"):
    check("ChatScale declares %s" % token, "public static final" in scale_src and token in scale_src)
check("ChatScale exposes normalizeMode/clamp/forMode",
      all(sig in scale_src for sig in ("static ChatScale forMode(",
                                       "static String normalizeMode(",
                                       "static float clamp(")))
check("ChatScale guards NaN", "Float.isNaN" in scale_src)

scale_test = "core-model/src/test/java/cn/lineai/model/ChatScaleTest.java"
ok, detail = parses(scale_test)
check("ChatScaleTest parses", ok, detail)
scale_test_src = read(scale_test)
check("ChatScaleTest imports no Robolectric/Mockito",
      not re.search(r"import\s+org\.(robolectric|mockito)", scale_test_src))
check("ChatScaleTest covers null / unknown / case / clamp",
      all(frag in scale_test_src for frag in ("normalizeMode(null)", '"gigantic"', '"COMPACT"', "Float.NaN")))
check("ChatScaleTest has at least 8 cases", scale_test_src.count("@Test") >= 8)

theme_src = read("ui-theme/src/main/java/cn/lineai/ui/theme/LineTheme.java")
check("LineTheme exposes chat scale fields",
      "CHAT_TEXT_SCALE" in theme_src and "CHAT_DENSITY_SCALE" in theme_src)
check("LineTheme chat scale defaults to 1f",
      "CHAT_TEXT_SCALE = 1f" in theme_src and "CHAT_DENSITY_SCALE = 1f" in theme_src)
for helper in ("chatSp(", "chatDp(", "chatText(", "chatTextMedium(", "chatPadding(", "applyChatScale("):
    check("LineTheme provides %s" % helper, "static " in theme_src and helper in theme_src)
check("applyChatScale tolerates a null scale", "if (scale == null)" in theme_src)
check("chatSp never returns below 1sp", "scaled < 1f ? 1f : scaled" in theme_src)
check("global dp()/text() stay unscaled",
      "CHAT_DENSITY_SCALE" not in theme_src.split("public static int dp(Context context, float value) {", 1)[1][:400])

# The chat scale must not leak into non-chat surfaces.
for screen in ("SettingsScreenView", "SimpleSettingsScreenView", "SecuritySettingsScreenView",
               "DataSettingsScreenView", "ToolSettingsScreenView"):
    path = "app/src/main/java/cn/lineai/ui/component/%s.java" % screen
    if (ROOT / path).exists():
        check("%s does not use chat scaling" % screen,
              not re.search(r"LineTheme\.chat(Sp|Dp|Text|TextMedium|Padding)\(", read(path)))

# Chat surfaces must actually consume it, otherwise the setting is invisible.
check("markdown scales its text",
      "LineTheme.chatSp(sizeSp)" in read("markdown/src/main/java/cn/lineai/ui/markdown/MarkdownTextBlockView.java"))
check("chatDp keeps hairlines visible", "value > 0f && scaled < 1" in theme_src)
check("markdown has no unscaled LineTheme.dp(/padding( left",
      not any(re.search(r"LineTheme\.(dp|padding)\(", read("markdown/src/main/java/cn/lineai/ui/markdown/%s" % f.name))
              for f in (ROOT / "markdown/src/main/java/cn/lineai/ui/markdown").glob("*.java")))
check("markdown has no unscaled LineTheme.text( left",
      not any("LineTheme.text(" in read("markdown/src/main/java/cn/lineai/ui/markdown/%s" % f.name)
              for f in (ROOT / "markdown/src/main/java/cn/lineai/ui/markdown").glob("*.java")))
for view in ("UserMessageView", "AssistantMessageView"):
    src = read("app/src/main/java/cn/lineai/ui/component/%s.java" % view)
    check("%s scales its padding" % view, "LineTheme.chatPadding(" in src)
check("UserMessageView scales its bubble text",
      "LineTheme.chatText(context, \"\", LineTheme.TYPE_TITLE" in read("app/src/main/java/cn/lineai/ui/component/UserMessageView.java"))
check("MessageActionBarView scales its row",
      "LineTheme.chatDp(context, ROW_HEIGHT_DP)" in read("app/src/main/java/cn/lineai/ui/component/MessageActionBarView.java"))
check("ComposerView scales its input text",
      "LineTheme.chatSp(LineTheme.TYPE_TITLE)" in read("app/src/main/java/cn/lineai/ui/component/ComposerView.java"))

repo_src = read("app/src/main/java/cn/lineai/data/repository/ThemeSettingsRepository.java")
check("ThemeSettingsRepository persists the chat scale",
      'KEY_CHAT_SCALE = "@lineai_chat_scale"' in repo_src)
check("ThemeSettingsRepository exposes get/set for the chat scale",
      "getChatScale()" in repo_src and "setChatScaleMode(" in repo_src)
check("chat scale reads default to normal",
      "KEY_CHAT_SCALE, ChatScale.MODE_NORMAL" in repo_src)

controller_src = read("app/src/main/java/cn/lineai/mvp/SettingsManagementController.java")
check("SettingsStore declares the chat scale hooks",
      "String getChatScaleMode();" in controller_src and "void applyChatScaleMode(String mode);" in controller_src)
check("changing the chat scale recreates the theme scope",
      re.search(r"setChatScaleMode\(String mode\)\s*\{[^}]*applyChatScaleMode\(mode\);[^}]*recreateForTheme\(\"theme\"\)",
                controller_src, re.S) is not None)
check("the store applies the scale to LineTheme",
      "LineTheme.applyChatScale(themeSettingsRepository.setChatScaleMode(mode))" in controller_src)
check("startup restores the saved chat scale",
      "LineTheme.applyChatScale(themeSettingsRepository.getChatScale())"
      in read("app/src/main/java/cn/lineai/mvp/MainDependencies.java"))

theme_screen = read("app/src/main/java/cn/lineai/ui/component/ThemeSettingsScreenView.java")
check("theme screen lists exactly four chat scale presets",
      theme_screen.count("new ScaleOption(") == 4)
check("the densest preset is listed first",
      theme_screen.index("ChatScale.MODE_ULTRA_COMPACT") < theme_screen.index("ChatScale.MODE_COMPACT,"))
check("chat scale presets are rendered as OptionRowView rows",
      "addChatScaleModes(" in theme_screen and "listener.onChatScaleModeChanged(option.mode)" in theme_screen)
check("chat scale section sits under the theme list",
      theme_screen.index("addThemeModes(content,") < theme_screen.index("addChatScaleModes(content,"))
check("theme screen keeps a 3-arg constructor for compatibility",
      "this(context, state, ChatScale.MODE_NORMAL, listener);" in theme_screen)
check("ThemeSettingsController exposes the chat scale",
      "String getChatScaleMode();" in read("app/src/main/java/cn/lineai/mvp/ThemeSettingsController.java"))
check("ScreenFactories passes the saved mode into the screen",
      "controller.getChatScaleMode()" in read("app/src/main/java/cn/lineai/ui/component/ScreenFactories.java"))

for locale in ("values", "values-ru", "values-zh"):
    src = read("app/src/main/res/%s/strings.xml" % locale)
    missing = [name for name in ("screen_theme_section_chat_scale",
                                 "screen_theme_scale_ultra_compact", "screen_theme_scale_ultra_compact_desc",
                                 "screen_theme_scale_compact",
                                 "screen_theme_scale_compact_desc", "screen_theme_scale_normal",
                                 "screen_theme_scale_normal_desc", "screen_theme_scale_large",
                                 "screen_theme_scale_large_desc")
               if 'name="%s"' % name not in src]
    check("%s defines all chat scale strings" % locale, not missing, ", ".join(missing))

# --------------------------------------------------------------- Chat list performance
list_path = "app/src/main/java/cn/lineai/ui/component/ChatMessageListView.java"
ok, detail = parses(list_path)
check("ChatMessageListView parses", ok, detail)
list_src = read(list_path)

check("tail following runs through setFollowTail()",
      list_src.count("private void setFollowTail(boolean enabled)") == 1)
check("no raw writes to followTailEnabled outside setFollowTail",
      list_src.count("followTailEnabled = ") == 1,
      "transcript mode would desync from the flag")
check("following the tail uses AbsListView transcript mode",
      "AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL" in list_src)
check("render() no longer posts a scroll on every streaming flush",
      "listView.post(() -> scrollToBottomInternal(false))" not in list_src)
check("scroll requests are coalesced", "scrollToBottomPending" in list_src)
check("visibility refreshes are coalesced", "visibilityUpdatePending" in list_src)
check("the tail correction has a laid-out fast path",
      "listView.scrollListBy(delta)" in list_src and "int laidOutIndex" in list_src)
check("bringToFront() only runs on a visibility transition",
      "if (scrollToBottomButton.getVisibility() == visibility)" in list_src)
check("the scroll FAB is re-styled only when the palette moves",
      "scrollButtonStyled" in list_src and "styledAccent" in list_src)
check("isAtBottom() does not convert dp on every scroll frame",
      "LineTheme.dp(getContext(), 2)" not in list_src and "bottomTolerancePx" in list_src)
check("the tool result map is skipped when nothing calls tools",
      "boolean anyToolCalls" in list_src)
check("the row cache is pruned only when the row set changes",
      "boolean rowSetChanged" in list_src)

assistant_src = read("app/src/main/java/cn/lineai/ui/component/AssistantMessageView.java")
check("the working indicator is not torn down on every bind",
      "workingStatusView.getVisibility() != GONE || workingStatusView.isWorking()" in assistant_src)
working_src = read("app/src/main/java/cn/lineai/ui/component/WorkingStatusView.java")
check("WorkingStatusView follows the chat scale",
      "LineTheme.chatDp(context, 16)" in working_src and "LineTheme.chatSp(LineTheme.FONT_SM)" in working_src)

# ---------------------------------------------------------------- No XML layouts added
check("no XML layouts introduced", not list((ROOT / "app/src/main/res").glob("layout*/*.xml")))

# ------------------------------------------------------------------------- Report
failed = [entry for entry in CHECKS if not entry[1]]
for name, ok_flag, detail in CHECKS:
    print("%s %s%s" % ("OK " if ok_flag else "FAIL", name,
                       ("  [%s]" % detail) if detail and not ok_flag else ""))
print("%d/%d OK" % (len(CHECKS) - len(failed), len(CHECKS)))
sys.exit(1 if failed else 0)
