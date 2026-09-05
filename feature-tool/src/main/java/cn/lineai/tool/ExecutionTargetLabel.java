package cn.lineai.tool;

import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.tool.builtin.RootProbe;

/**
 * One-line execution-target caption for the chat header:
 * {@code Root · /data/project} or {@code Root · su denied · /data/project}.
 */
public final class ExecutionTargetLabel {
    private static volatile String currentMode = "";

    private ExecutionTargetLabel() {
    }

    /** Last execution mode read or written by ToolSettingsRepository. */
    public static void remember(String executionMode) {
        currentMode = executionMode == null ? "" : executionMode;
    }

    public static String current() {
        return currentMode;
    }

    public static String format(String executionMode, String cwd) {
        return format(executionMode, cwd, null);
    }

    public static String format(String executionMode, String cwd, RootProbe.Status rootStatus) {
        String mode = executionMode == null ? "" : executionMode;
        String target;
        if (ToolSettingsStore.EXECUTION_ROOT.equals(mode)) {
            target = "Root";
        } else if (ToolSettingsStore.EXECUTION_SSH.equals(mode)) {
            target = "SSH";
        } else if (ToolSettingsStore.EXECUTION_TERMINAL_PROVIDER.equals(mode)) {
            target = "IPC";
        } else {
            target = "Local";
        }
        String path = shorten(cwd);
        if (ToolSettingsStore.EXECUTION_ROOT.equals(mode)
                && rootStatus != null
                && rootStatus != RootProbe.Status.READY
                && rootStatus != RootProbe.Status.UNKNOWN) {
            return target + " · " + rootStatus.shortLabel() + (path.isEmpty() ? "" : " · " + path);
        }
        return path.isEmpty() ? target : target + " · " + path;
    }

    static String shorten(String cwd) {
        if (cwd == null) {
            return "";
        }
        String value = cwd.trim();
        if (value.length() <= 28) {
            return value;
        }
        int slash = value.lastIndexOf('/');
        if (slash >= 0 && slash < value.length() - 1) {
            return "…" + value.substring(slash);
        }
        return "…" + value.substring(value.length() - 24);
    }
}
