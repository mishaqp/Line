#!/usr/bin/env python3
"""Static verifier for the Material Design 3 token work.

Runs in environments without a JDK (the sandbox / quick local checks). It parses the
REAL sources (no snapshots) and re-evaluates the color expressions with Java float
semantics (float32 rounding after every op, Math.round = floor(x + 0.5) with ties up)
so the numbers match what the JVM-based unit tests assert. CI remains the source of
truth; this script is a fast pre-push guard.
"""
import re
import struct
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CHECKS = []


def check(name, condition, detail=""):
    CHECKS.append((name, bool(condition), detail))
    return bool(condition)


def f32(x):
    """Round a Python double to the nearest IEEE-754 binary32 (Java float) value."""
    return struct.unpack("f", struct.pack("f", x))[0]


def jround(x):
    """Java Math.round(float): floor(x + 0.5f) with .5 rounding up."""
    return int(f32(f32(x) + 0.5) // 1)


def with_alpha(color, alpha):
    a = f32(alpha)
    return ((color & 0x00FFFFFF) | ((jround(f32(a * 255.0)) & 0xFF) << 24)) & 0xFFFFFFFF


def argb(alpha, r, g, b):
    return ((alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF)) & 0xFFFFFFFF


def rgba(r, g, b, alpha):
    return argb(jround(f32(f32(alpha) * 255.0)), r, g, b)


def clamp(value, lo, hi):
    return lo if value < lo else (hi if value > hi else value)


def channel(value):
    r = jround(f32(f32(value) * 255.0))
    return 0 if r < 0 else (255 if r > 255 else r)


def rgb_to_hsv(color):
    red = f32(((color >> 16) & 0xFF) / 255.0)
    green = f32(((color >> 8) & 0xFF) / 255.0)
    blue = f32((color & 0xFF) / 255.0)
    mx = max(red, green, blue)
    mn = min(red, green, blue)
    delta = f32(mx - mn)
    if delta == 0.0:
        hue = 0.0
    elif mx == red:
        hue = f32(f32(f32(green - blue) / delta) % 6.0)
    elif mx == green:
        hue = f32(f32(blue - red) / delta + 2.0)
    else:
        hue = f32(f32(red - green) / delta + 4.0)
    if hue < 0.0:
        hue = f32(hue + 6.0)
    return f32(hue * 60.0), (0.0 if mx == 0.0 else f32(delta / mx)), mx


def hsv_to_color(hue, saturation, value, alpha=1.0):
    saturation = f32(saturation)
    value = f32(value)
    sector_position = f32(f32(hue) / 60.0)
    sector = int(sector_position) % 6
    fraction = f32(sector_position - int(sector_position))
    p = f32(value * f32(1.0 - saturation))
    q = f32(value * f32(1.0 - f32(saturation * fraction)))
    t = f32(value * f32(1.0 - f32(saturation * f32(1.0 - fraction))))
    if sector == 0:
        r, g, b = value, t, p
    elif sector == 1:
        r, g, b = q, value, p
    elif sector == 2:
        r, g, b = p, value, t
    elif sector == 3:
        r, g, b = p, q, value
    elif sector == 4:
        r, g, b = t, p, value
    else:
        r, g, b = value, p, q
    return argb(jround(f32(f32(f32(alpha)) * 255.0)), channel(r), channel(g), channel(b))


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


# ---------------------------------------------------------------- LineTheme tokens
line_theme = read("ui-theme/src/main/java/cn/lineai/ui/theme/LineTheme.java")
for token, expected in [
    ("SHAPE_XS", 4), ("SHAPE_SM", 8), ("SHAPE_MD", 12),
    ("SHAPE_LG", 16), ("SHAPE_XL", 28), ("SHAPE_FULL", 999),
]:
    m = re.search(r"int " + token + r" = (\d+)", line_theme)
    check("LineTheme.%s" % token, m and int(m.group(1)) == expected,
          m.group(1) if m else "missing")

for token, expected in [
    ("TYPE_DISPLAY", 36), ("TYPE_HEADLINE", 24), ("TYPE_TITLE", 16),
    ("TYPE_BODY", 14), ("TYPE_BODY_SMALL", 12), ("TYPE_LABEL", 14),
]:
    m = re.search(r"int " + token + r" = (\d+)", line_theme)
    check("LineTheme.%s" % token, m and int(m.group(1)) == expected,
          m.group(1) if m else "missing")

for token, expected in [
    ("STATE_LAYER_ALPHA_HOVER", 0.08), ("STATE_LAYER_ALPHA_FOCUS", 0.10),
    ("STATE_LAYER_ALPHA_PRESSED", 0.10), ("STATE_LAYER_ALPHA_DRAGGED", 0.16),
]:
    m = re.search(r"float " + token + r" = ([\d.]+)f", line_theme)
    check("LineTheme.%s" % token, m and abs(float(m.group(1)) - expected) < 1e-9,
          m.group(1) if m else "missing")

check("LineTheme.withAlpha uses Math.round",
      "Math.round(alpha * 255f)" in line_theme)
check("LineTheme.pressedLayerColor delegates",
      "stateLayerColor(color, STATE_LAYER_ALPHA_PRESSED)" in line_theme)
check("LineTheme.attachStateLayer builds ripple state layers",
      "RippleDrawable" in line_theme
      and "state_pressed" in line_theme
      and "state_focused" in line_theme
      and "state_hovered" in line_theme)

# withAlpha values asserted by LineThemeTokensTest, recomputed with Java float math.
color = 0xFF3FB950
check("withAlpha(0.10f) == 0x1A", with_alpha(color, 0.10) == 0x1A3FB950,
      hex(with_alpha(color, 0.10)))
check("withAlpha(0.08f) == 0x14", with_alpha(color, 0.08) == 0x143FB950,
      hex(with_alpha(color, 0.08)))
check("withAlpha(0.16f) == 0x29", with_alpha(color, 0.16) == 0x293FB950,
      hex(with_alpha(color, 0.16)))
check("withAlpha(0.50f) == 0x80", ((with_alpha(0xFF123456, 0.5) >> 24) & 0xFF) == 128)

# ------------------------------------------------------------- ThemePalette.dynamic
palette = read("core-model/src/main/java/cn/lineai/model/ThemePalette.java")
check("MODE_DYNAMIC_COLOR constant",
      'MODE_DYNAMIC_COLOR = "dynamicColor"' in palette)
check("normalizeMode accepts dynamicColor",
      re.search(r"MODE_DYNAMIC_COLOR\.equals\(value\)", palette) is not None)
check("forMode branches to dynamic()",
      re.search(r"MODE_DYNAMIC_COLOR\.equals\(normalized\)\) return dynamic\(0, false\)", palette)
      is not None)
check("dynamic() zero-seed falls back to dark()",
      re.search(r"public static ThemePalette dynamic\(int seed, boolean darkScheme\)", palette)
      and "if (seed == 0) {\n            return dark();" in palette)

dark_body = palette.split("private static ThemePalette dark()")[1]
dynamic_dark_body = palette.split("private static ThemePalette dynamicDark")[1]
dynamic_light_body = palette.split("private static ThemePalette dynamicLight")[1]
for expr in ["rgba(248, 81, 73, 0.15f)", "rgba(248, 81, 73, 0.20f)", "rgba(255, 152, 0, 0.10f)"]:
    check("dynamicDark keeps dark() semantic muted colors (%s)" % expr,
          expr in dynamic_dark_body and expr in dark_body)

SEED = 0xFF48B95C


def dynamic_accent(seed, dark_scheme):
    h, s, _v = rgb_to_hsv(seed)
    sat = clamp(s, 0.45 if dark_scheme else 0.55, 0.85 if dark_scheme else 0.95)
    return hsv_to_color(h, sat, 0.82 if dark_scheme else 0.52)


def dynamic_bg(seed, dark_scheme):
    h, _s, _v = rgb_to_hsv(seed)
    if dark_scheme:
        return hsv_to_color(h, 0.18, 0.05)
    return hsv_to_color(h, 0.10, 0.965)


red_accent = dynamic_accent(0xFFFF0000, True)
check("dynamic red seed keeps red dominant",
      ((red_accent >> 16) & 0xFF) > ((red_accent >> 8) & 0xFF)
      and ((red_accent >> 16) & 0xFF) > (red_accent & 0xFF),
      hex(red_accent))
blue_accent = dynamic_accent(0xFF0000FF, True)
check("dynamic blue seed keeps blue dominant",
      (blue_accent & 0xFF) > ((blue_accent >> 16) & 0xFF)
      and (blue_accent & 0xFF) > ((blue_accent >> 8) & 0xFF),
      hex(blue_accent))
check("dynamic dark/light backgrounds differ",
      dynamic_bg(SEED, True) != dynamic_bg(SEED, False))
check("dynamic accents are opaque",
      ((dynamic_accent(SEED, True) >> 24) & 0xFF) == 0xFF
      and ((dynamic_accent(SEED, False) >> 24) & 0xFF) == 0xFF)

# accentMuted / accentMuted2 literals parsed from the real dynamicDark/dynamicLight bodies.
muted = re.findall(r"withAlpha\(accent, ([\d.]+)f\)", dynamic_dark_body + dynamic_light_body)
check("dynamic muted accents use 0.10/0.15 state layers",
      sorted(set(muted)) == ["0.10", "0.15"], str(sorted(set(muted))))
check("withAlpha(0.10) byte == 26 (Java rounds 25.5 up)",
      ((with_alpha(0xFF48B95C, 0.10) >> 24) & 0xFF) == 26)
check("withAlpha(0.15) byte == 38",
      ((with_alpha(0xFF48B95C, 0.15) >> 24) & 0xFF) == 38)
check("rgba overlay alpha 0.60 byte == 153",
      ((rgba(0, 0, 0, 0.60) >> 24) & 0xFF) == 153)

# ------------------------------------------------------------------- Unit tests
dynamic_test = read("core-model/src/test/java/cn/lineai/model/ThemePaletteDynamicTest.java")
tokens_test = read("ui-theme/src/test/java/cn/lineai/ui/theme/LineThemeTokensTest.java")
check("ThemePaletteDynamicTest has 9 @Test methods",
      len(re.findall(r"@Test", dynamic_test)) == 9)
check("LineThemeTokensTest has 6 @Test methods",
      len(re.findall(r"@Test", tokens_test)) == 6)
check("tests assert Java half-up rounding (0x1A3FB950 / 128)",
      "0x1A3FB950" in tokens_test and "assertEquals(128" in tokens_test)

# ------------------------------------------------------------------- UI wiring
for view in [
    "app/src/main/java/cn/lineai/ui/component/MCPSettingsScreenView.java",
    "app/src/main/java/cn/lineai/ui/component/ToolSettingsScreenView.java",
]:
    src = read(view)
    check("%s actionButton is a full pill" % Path(view).name,
          "SHAPE_FULL" in src and "attachStateLayer(button)" in src)
    check("%s cards use SHAPE_MD" % Path(view).name,
          re.search(r"rounded\(context, LineTheme\.SURFACE_ELEVATED, LineTheme\.SHAPE_MD\)", src)
          is not None)

mcp_view = read("app/src/main/java/cn/lineai/ui/component/MCPSettingsScreenView.java")
check("MCP execution segment uses SHAPE_SM",
      "SURFACE_LIGHT, LineTheme.SHAPE_SM" in mcp_view)
tool_view = read("app/src/main/java/cn/lineai/ui/component/ToolSettingsScreenView.java")
check("ToolSettings provider segment uses SHAPE_SM",
      "SURFACE_LIGHT, LineTheme.SHAPE_SM" in tool_view)

theme_view = read("app/src/main/java/cn/lineai/ui/component/ThemeSettingsScreenView.java")
check("theme picker lists Material You option",
      "MODE_DYNAMIC_COLOR" in theme_view and "R.string.screen_theme_dynamic_color" in theme_view)

# ------------------------------------------------------------------- App wiring
provider = read("core-api/src/main/java/cn/lineai/resource/SystemConfigProvider.java")
check("SystemConfigProvider exposes getDynamicAccentColor()",
      "int getDynamicAccentColor();" in provider)
main_activity = read("app/src/main/java/cn/lineai/MainActivity.java")
check("MainActivity supplies the dynamic seed",
      "getDynamicAccentColor" in main_activity and "DynamicColors" in main_activity)
main_deps = read("app/src/main/java/cn/lineai/mvp/MainDependencies.java")
check("MainDependencies supplies the dynamic seed",
      "getDynamicAccentColor" in main_deps and "DynamicColors" in main_deps)
repo = read("app/src/main/java/cn/lineai/data/repository/ThemeSettingsRepository.java")
check("ThemeSettingsRepository resolves MODE_DYNAMIC_COLOR",
      "MODE_DYNAMIC_COLOR" in repo and "getDynamicAccentColor()" in repo)
dynamic_colors = read("app/src/main/java/cn/lineai/resource/DynamicColors.java")
check("DynamicColors reads system_accent1_400 on S+ and 0 otherwise",
      "system_accent1_400" in dynamic_colors
      and "Build.VERSION_CODES.S" in dynamic_colors
      and "return 0" in dynamic_colors)

# ------------------------------------------------------------------- Strings / CI
for locale in ["values", "values-ru", "values-zh"]:
    path = ROOT / ("app/src/main/res/%s/strings.xml" % locale)
    try:
        tree = ElementTree.parse(path)
        names = {element.get("name") for element in tree.iter("string")}
        check("%s declares screen_theme_dynamic_color(_desc)" % locale,
              "screen_theme_dynamic_color" in names
              and "screen_theme_dynamic_color_desc" in names)
        for element in tree.iter("string"):
            if element.get("name", "").startswith("screen_theme_dynamic_color"):
                check("%s %s has no backslash (aapt2-safe)" % (locale, element.get("name")),
                      "\\" not in (element.text or ""))
    except ElementTree.ParseError as error:
        check("%s strings.xml well-formed" % locale, False, str(error))

ci = read(".github/workflows/ci.yml")
check("CI runs :core-model:test", "./gradlew :core-model:test" in ci)
check("CI runs :ui-theme:testDebugUnitTest",
      "./gradlew :ui-theme:testDebugUnitTest" in ci)
check("CI keeps :feature-tool test task", "./gradlew :feature-tool:testDebugUnitTest" in ci)
check("CI never pushes to branches", "git push" not in ci)

gradle = read("core-model/build.gradle.kts")
check("core-model test classpath has junit", "testImplementation(libs.junit)" in gradle)
convention = read(
    "build-logic/src/main/kotlin/cn/lineai/build/LineCodeConventionPlugin.kt")
check("library unit tests may touch android.jar stubs",
      "isReturnDefaultValues = true" in convention)

# ------------------------------------------------------------------- Report
failed = [entry for entry in CHECKS if not entry[1]]
for name, ok_flag, detail in CHECKS:
    print("%s %s%s" % ("OK " if ok_flag else "FAIL", name, ("  [%s]" % detail) if detail and not ok_flag else ""))
print("%d/%d OK" % (len(CHECKS) - len(failed), len(CHECKS)))
sys.exit(1 if failed else 0)
