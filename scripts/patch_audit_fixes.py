#!/usr/bin/env python3
from pathlib import Path
import re

cv = Path("app/src/main/java/cn/lineai/ui/component/ComposerView.java")
text = cv.read_text()

for field in [
    "    private LinearLayout modeSelectorButton;\n",
    "    private TextView modeSelectorText;\n",
    "    private IconButtonView modeSelectorChevron;\n",
    "    private TextView modelText;\n",
    "    private IconButtonView modelChevron;\n",
    "    private TextView contextText;\n",
]:
    text = text.replace(field, "", 1)

start = text.find("    private void updateModeButtons() {")
if start >= 0:
    end = text.find("    private void updateModelSelector()", start)
    if end > start:
        text = text[:start] + text[end:]

start = text.find("    private void showModePopup(View anchor) {")
if start >= 0:
    nxt = text.find("\n    private ", start + 10)
    if nxt > start:
        text = text[:start] + text[nxt + 1:]

cv.write_text(text)
print("composer modeSelectorButton", "modeSelectorButton" in text, "updateModeButtons", "updateModeButtons" in text)

init = Path("app/src/main/java/cn/lineai/mvp/MainControllerInitializer.java")
s = init.read_text()
marker = '"data:import_linecode"'
i = s.find(marker)
if i < 0:
    raise SystemExit("import action id missing")
j = s.rfind("coordinator.viewProxy().showConfirmationDialog(", 0, i)
if j < 0:
    raise SystemExit("showConfirmationDialog not found")
k = s.find(");", i)
if k < 0:
    raise SystemExit("dialog end not found")
new_block = '''coordinator.viewProxy().showConfirmationDialog(
                                context.getString(R.string.screen_data_import_linecode),
                                context.getString(R.string.screen_data_import_linecode_desc) + "\\n\\n" + sourceName,
                                context.getString(R.string.common_confirm),
                                true,
                                "data:import_linecode"
                        )'''
s = s[:j] + new_block + s[k + 2:]
init.write_text(s)
print("init localized", "screen_data_import_linecode" in s)

test = Path("app/src/test/java/cn/lineai/mvp/SettingsManagementControllerTest.java")
tt = test.read_text()
needle = '''    @Test\n    public void toneChangeDoesNotForceRender() {'''
insert = '''    @Test\n    public void reasoningEffortChangeRenders() {\n        Fixture fixture = new Fixture();\n\n        fixture.controller.setAiReasoningEffort(AiBehaviorSettings.REASONING_HIGH);\n\n        Assert.assertTrue(fixture.host.rendered);\n    }\n\n''' + needle
if "reasoningEffortChangeRenders" not in tt:
    if needle not in tt:
        raise SystemExit("test insert point missing")
    test.write_text(tt.replace(needle, insert, 1))
    print("test inserted")
else:
    print("test already present")
