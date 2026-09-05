#!/usr/bin/env python3
from pathlib import Path

cv = Path("app/src/main/java/cn/lineai/ui/component/ComposerView.java")
text = cv.read_text()

dead_fields = '''    private LinearLayout modeSelectorButton;
    private TextView modeSelectorText;
    private IconButtonView modeSelectorChevron;
'''
if dead_fields in text:
    text = text.replace(dead_fields, "", 1)

for field in [
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
    end = text.find("    private TextView compactPopupItem", start)
    # mode popup is after plus; find next method after showModePopup
    nxt = text.find("\n    private ", start + 10)
    if nxt > start:
        text = text[:start] + text[nxt+1:]

cv.write_text(text)
print("composer patched", "updateModeButtons" in text, "modeSelectorButton" in text)

init = Path("app/src/main/java/cn/lineai/mvp/MainControllerInitializer.java")
s = init.read_text()
old = '''                        coordinator.viewProxy().showConfirmationDialog(
                                "\u8986\u76d6\u5bfc\u5165 .linecode",
                                "\u5c06\u4ece\u300c" + sourceName + "\u300d\u6062\u590d\u6570\u636e\u5e93\u3001\u804a\u5929\u8bb0\u5f55\u3001\u914d\u7f6e\u548c .linecode \u5de5\u4f5c\u533a\u6587\u4ef6\u3002\u5f53\u524d\u672c\u673a\u6570\u636e\u4f1a\u88ab\u8986\u76d6\u3002",
                                context.getString(R.string.common_confirm),
                                true,
                                "data:import_linecode"
                        );'''
new = '''                        coordinator.viewProxy().showConfirmationDialog(
                                context.getString(R.string.screen_data_import_linecode),
                                context.getString(R.string.screen_data_import_linecode_desc) + "\n\n" + sourceName,
                                context.getString(R.string.common_confirm),
                                true,
                                "data:import_linecode"
                        );'''
# also match raw CJK
old_cjk = '''                        coordinator.viewProxy().showConfirmationDialog(
                                "覆盖导入 .linecode",
                                "将从「" + sourceName + "」恢复数据库、聊天记录、配置和 .linecode 工作区文件。当前本机数据会被覆盖。",
                                context.getString(R.string.common_confirm),
                                true,
                                "data:import_linecode"
                        );'''
if old in s:
    s = s.replace(old, new, 1)
    print("init escaped replaced")
elif old_cjk in s:
    s = s.replace(old_cjk, new, 1)
    print("init cjk replaced")
else:
    if "screen_data_import_linecode" in s and "data:import_linecode" in s:
        print("init already localized")
    else:
        i = s.find("data:import_linecode")
        print("init marker context:\n", s[max(0,i-400):i+80])
        raise SystemExit("import dialog block not found")
init.write_text(s)

test = Path("app/src/test/java/cn/lineai/mvp/SettingsManagementControllerTest.java")
tt = test.read_text()
needle = '''    @Test
    public void toneChangeDoesNotForceRender() {'''
insert = '''    @Test
    public void reasoningEffortChangeRenders() {
        Fixture fixture = new Fixture();

        fixture.controller.setAiReasoningEffort(AiBehaviorSettings.REASONING_HIGH);

        Assert.assertTrue(fixture.host.rendered);
    }

''' + needle
if "reasoningEffortChangeRenders" not in tt:
    if needle not in tt:
        raise SystemExit("test insert point missing")
    test.write_text(tt.replace(needle, insert, 1))
    print("test inserted")
else:
    print("test already present")
