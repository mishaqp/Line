package cn.lineai.tool.builtin;
import cn.lineai.model.tool.ToolResult;

import android.content.Context;
import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.ipc.IpcProviderManager;
import cn.lineai.ipc.IpcProviderType;
import cn.lineai.ipc.terminal.TerminalIpcProvider;
import cn.lineai.ipc.terminal.TerminalShellCallback;
import cn.lineai.ipc.terminal.TerminalShellResult;
import cn.lineai.ssh.SshService;
import cn.lineai.tool.BaseTool;
import cn.lineai.tool.ExceptionUtils;
import cn.lineai.tool.R;
import cn.lineai.tool.ToolCategory;
import cn.lineai.tool.ToolContext;
import cn.lineai.tool.ToolDisplayCategory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ShellExecuteTool extends BaseTool {
    public static final String NAME = "shell_execute";
    private final SshService sshService;
    private final IpcProviderManager ipcProviderManager;

    public ShellExecuteTool(Context context) {
        this(context, null);
    }

    public ShellExecuteTool(Context context, IpcProviderManager ipcProviderManager) {
        sshService = context == null ? null : new SshService(context);
        this.ipcProviderManager = ipcProviderManager;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Execute a shell command via the current execution target: local mode runs it on the device shell, SSH mode uses SSH, terminal provider mode uses IPC. The command requires user confirmation before execution.";
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.SYSTEM;
    }

    @Override
    public ToolDisplayCategory getDisplayCategory() {
        return ToolDisplayCategory.SHELL;
    }

    @Override
    public boolean needsConfirmation() {
        return true;
    }

    @Override
    public boolean isAllowedInReadonlyMode() {
        return true;
    }

    @Override
    public String promptSupplement(String executionMode, boolean isSsh) {
        if (ToolSettingsStore.EXECUTION_ROOT.equals(executionMode)) {
            return "shell_execute runs on this device as root via su; it can manage packages, settings and any path. "
                    + "It runs in the current workspace directory by default; set cwd explicitly to switch temporarily. "
                    + "Never leave a command waiting for interactive input.";
        }
        if (isSsh) {
            return "shell_execute runs in the current workspace directory by default; set cwd explicitly to switch temporarily.";
        }
        if (ToolSettingsStore.EXECUTION_LOCAL.equals(executionMode)) {
            return "shell_execute runs locally on the device via the app shell (Termux/system PATH); it runs in the current workspace directory by default; set cwd explicitly to switch temporarily. Commands that need root may require a su-capable shell.";
        }
        return "shell_execute runs via the terminal provider IPC; it runs in the current workspace directory by default; set cwd explicitly to switch temporarily.";
    }

    @Override
    public JSONObject getParameters() throws org.json.JSONException {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("command", new JSONObject()
                                .put("type", "string")
                                .put("description", "The shell command to execute"))
                        .put("cwd", new JSONObject()
                                .put("type", "string")
                                .put("description", "Optional working directory; the command runs after cd into it"))
                        .put("timeoutMs", new JSONObject()
                                .put("type", "number")
                                .put("description", "Optional timeout in milliseconds, default 30000, max 300000")))
                .put("required", new JSONArray().put("command"));
    }

    @Override
    public ToolResult execute(JSONObject input, ToolContext context) {
        String inputCommand = input.optString("command", "");
        String inputCwd = input.optString("cwd", "");
        if (inputCommand.trim().length() == 0) {
            return error(context.getString(R.string.tool_shell_command_empty));
        }
        int timeoutMs = Math.max(1000, Math.min(input.optInt("timeoutMs", 30000), 300000));
        String cwd = inputCwd.trim().length() > 0
                ? inputCwd.trim()
                : context == null ? "" : context.getHomePath().trim();
        if (RootSupport.isRootMode(context)) {
            return executeViaRoot(inputCommand, cwd, timeoutMs, context);
        }
        ToolSettingsStore settings = resolveSettings(context);
        if (isTerminalProviderMode(settings)) {
            return executeViaTerminalProvider(inputCommand, cwd, timeoutMs, context);
        }
        if (isLocalMode(settings)) {
            return executeViaLocalShell(inputCommand, cwd, timeoutMs, context);
        }
        return executeViaSsh(inputCommand, cwd, timeoutMs, context);
    }

    /**
     * Root 执行目标：{@code su -c} 直接在本机以 root 身份执行。
     * stdin 由 {@link RootShellExecutor} 关闭/写入，避免 su 等待输入而挂起。
     */
    private ToolResult executeViaRoot(String command, String cwd, long timeoutMs, ToolContext context) {
        RootSupport.Availability availability = RootSupport.availability();
        RootProbe.Status probe = availability.probe(timeoutMs);
        if (probe != RootProbe.Status.READY) {
            String fallback = RootProbe.describe(probe);
            String localized = context.getString(R.string.tool_root_unavailable);
            return error(localized + "\n" + fallback);
        }
        String script = cwd.length() > 0
                ? "cd " + shellQuote(cwd) + " && " + command
                : command;
        if (context != null) {
            context.reportToolProgress(getName(), "", false);
        }
        try {
            RootCommandRunner.Result result = RootSupport.commandRunner().run(script, null, timeoutMs);
            String output = result.getOutput() == null ? "" : result.getOutput().trim();
            if (!result.isSuccess()) {
                String message = context.getString(R.string.tool_root_exec_failed, result.getExitCode(),
                        output.length() == 0 ? "(no output)" : output);
                return error(truncateOutput(message, context));
            }
            if (output.length() == 0) {
                return ok(context.getString(R.string.tool_shell_exec_no_output));
            }
            return ok(truncateOutput(output, context));
        } catch (RootShellExecutor.RootTimeoutException e) {
            return error(context.getString(R.string.tool_root_timeout, (int) timeoutMs));
        } catch (Exception e) {
            restoreInterrupt(e);
            return error(context.getString(R.string.tool_shell_exec_failed, describeException(e)));
        }
    }

    private ToolSettingsStore resolveSettings(ToolContext context) {
        if (context != null && context.getToolSettingsStore() != null) {
            return context.getToolSettingsStore();
        }
        return null;
    }

    private boolean isTerminalProviderMode(ToolSettingsStore settings) {
        return settings != null
                && ToolSettingsStore.EXECUTION_TERMINAL_PROVIDER.equals(settings.getExecutionMode());
    }

    private boolean isLocalMode(ToolSettingsStore settings) {
        return settings == null
                || ToolSettingsStore.EXECUTION_LOCAL.equals(settings.getExecutionMode());
    }

    /**
     * Local execution mode runs the command on the device shell. When the plain
     * app-shell attempt cannot find the command (127/126) and a su binary is
     * available, the command is retried under root so that rooted devices can
     * run commands outside the app's sandbox (nothing here bypasses Android
     * permission boundaries by itself - su must be granted by the device).
     */
    private ToolResult executeViaLocalShell(String command, String cwd, long timeoutMs, ToolContext context) {
        ProcessOutcome plain = runLocalProcess(command, cwd, timeoutMs, context, false);
        if (plain.exitCode != 127 && plain.exitCode != 126) {
            return localOutcome(plain, timeoutMs, context);
        }
        ProcessOutcome su = runLocalProcess(command, cwd, timeoutMs, context, true);
        if (su.exitCode == 127 || su.exitCode == 126) {
            // su is not usable; report the original attempt's failure.
            return localOutcome(plain, timeoutMs, context);
        }
        return localOutcome(su, timeoutMs, context);
    }

    private ProcessOutcome runLocalProcess(String command, String cwd, long timeoutMs, ToolContext context, boolean useSu) {
        ProcessBuilder builder;
        if (useSu) {
            String wrapped = cwd.length() > 0
                    ? "cd " + shellQuote(cwd) + " && " + command
                    : command;
            builder = new ProcessBuilder("su", "-c", wrapped);
        } else {
            builder = new ProcessBuilder("sh", "-c", command);
            String dir = cwd == null ? "" : cwd.trim();
            if (dir.length() > 0) {
                File workingDir = new File(dir);
                if (workingDir.isDirectory()) {
                    builder.directory(workingDir);
                }
            }
        }
        Map<String, String> env = builder.environment();
        if (env != null) {
            String augPath = appendPath(env.get("PATH"),
                    "/data/data/com.termux/files/usr/bin", "/system/xbin", "/vendor/bin");
            env.put("PATH", augPath);
            if (cwd.length() > 0) {
                env.put("HOME", cwd);
            }
        }
        if (context != null) {
            context.reportToolProgress(getName(), "", false);
        }
        builder.redirectErrorStream(true);
        // Never leave the child stdin pipe open: an interactive su without a
        // terminal would otherwise wait forever for a password.
        builder.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
        try {
            Process process = builder.start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream in = process.getInputStream();
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    if (context != null) {
                        context.reportToolProgress(getName(), new String(buffer, 0, read, StandardCharsets.UTF_8), false);
                    }
                }
            } finally {
                in.close();
            }
            long started = System.currentTimeMillis();
            boolean finished = process.waitFor(
                    Math.max(1000, timeoutMs - (System.currentTimeMillis() - started)),
                    TimeUnit.MILLISECONDS
            );
            if (!finished) {
                process.destroy();
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
                return new ProcessOutcome(-1, new String(output.toByteArray(), StandardCharsets.UTF_8).trim(), true);
            }
            return new ProcessOutcome(
                    process.exitValue(),
                    new String(output.toByteArray(), StandardCharsets.UTF_8).trim(),
                    false
            );
        } catch (InterruptedException e) {
            restoreInterrupt(e);
            return new ProcessOutcome(-1, describeException(e), false);
        } catch (Exception e) {
            restoreInterrupt(e);
            return new ProcessOutcome(-1, describeException(e), false);
        }
    }

    private ToolResult localOutcome(ProcessOutcome outcome, long timeoutMs, ToolContext context) {
        if (outcome.timedOut) {
            return error(text(context, R.string.tool_shell_exec_timed_out,
                    "Shell command timed out after " + timeoutMs + " ms"));
        }
        if (outcome.exitCode != 0) {
            String message = text(context, R.string.tool_shell_exec_failed_exit,
                    "Shell exited with code " + outcome.exitCode, outcome.exitCode);
            String output = truncateOutput(outcome.output, context);
            return error(output.length() == 0 ? message : output + "\n" + message);
        }
        String output = truncateOutput(outcome.output, context);
        if (output.length() == 0) {
            return ok(text(context, R.string.tool_shell_exec_no_output, "Command completed with no output."));
        }
        return ok(output);
    }

    private static String appendPath(String existing, String... extraDirs) {
        StringBuilder builder = new StringBuilder();
        if (existing != null && existing.length() > 0) {
            builder.append(existing);
        }
        for (String dir : extraDirs) {
            if (dir == null || dir.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(dir);
        }
        return builder.toString();
    }

    private String text(ToolContext context, int resId, String fallback) {
        if (context == null) {
            return fallback;
        }
        String value = context.getString(resId);
        return value.length() == 0 ? fallback : value;
    }

    private String text(ToolContext context, int resId, String fallback, Object... formatArgs) {
        if (context == null) {
            return fallback;
        }
        String value = context.getString(resId, formatArgs);
        return value.length() == 0 ? fallback : value;
    }

    private static final class ProcessOutcome {
        final int exitCode;
        final String output;
        final boolean timedOut;

        ProcessOutcome(int exitCode, String output, boolean timedOut) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
            this.timedOut = timedOut;
        }
    }

    private ToolResult executeViaTerminalProvider(String command, String cwd, long timeoutMs, ToolContext context) {
        if (ipcProviderManager == null) {
            return error(context.getString(R.string.tool_shell_ipc_not_init));
        }
        TerminalIpcProvider provider = ipcProviderManager.getProviderByType(IpcProviderType.TERMINAL) instanceof TerminalIpcProvider
                ? (TerminalIpcProvider) ipcProviderManager.getProviderByType(IpcProviderType.TERMINAL)
                : null;
        if (provider == null) {
            return error(context.getString(R.string.tool_shell_no_provider));
        }
        if (!provider.isBound()) {
            return error(context.getString(R.string.tool_shell_provider_not_bound));
        }
        if (context != null) {
            context.reportToolProgress(getName(), "", false);
        }
        StringBuilder streamedOutput = new StringBuilder();
        try {
            TerminalShellResult result = provider.executeShell(command, cwd, timeoutMs, new TerminalShellCallback() {
                @Override
                public void onOutput(String content) {
                    synchronized (streamedOutput) {
                        streamedOutput.append(content == null ? "" : content);
                    }
                    if (context != null) {
                        context.reportToolProgress(getName(), content, false);
                    }
                }

                @Override
                public void onError(String error) {
                }

                @Override
                public void onComplete(int exitCode) {
                }
            });
            String output = streamedOutput.toString().trim();
            if (!result.isSuccess()) {
                String message = context.getString(R.string.tool_shell_exec_failed_exit, result.getExitCode());
                return error(output.length() == 0 ? message : truncateOutput(output, context) + "\n" + message);
            }
            if (output.length() == 0) {
                return ok(context.getString(R.string.tool_shell_exec_no_output));
            }
            return ok(truncateOutput(output, context));
        } catch (Exception e) {
            restoreInterrupt(e);
            String existing;
            synchronized (streamedOutput) {
                existing = streamedOutput.toString().trim();
            }
            String message = context.getString(R.string.tool_shell_exec_failed, describeException(e));
            return error(existing.length() == 0 ? message : truncateOutput(existing, context) + "\n" + message);
        }
    }

    private ToolResult executeViaSsh(String inputCommand, String cwd, long timeoutMs, ToolContext context) {
        if (sshService == null) {
            return error(context.getString(R.string.tool_shell_ssh_not_init));
        }
        String command = cwd.length() > 0
                ? "cd " + shellQuote(cwd) + " && " + inputCommand
                : inputCommand;
        StringBuilder streamedOutput = new StringBuilder();
        if (context != null) {
            context.reportToolProgress(getName(), "", false);
        }
        try {
            String output = sshService.executeCommand(command, (int) timeoutMs, null, streamed -> {
                synchronized (streamedOutput) {
                    streamedOutput.append(streamed == null ? "" : streamed);
                }
                if (context != null) {
                    context.reportToolProgress(getName(), streamed, false);
                }
            });
            if (output.length() == 0) {
                return ok(context.getString(R.string.tool_shell_exec_no_output));
            }
            return ok(truncateOutput(output, context));
        } catch (Exception e) {
            restoreInterrupt(e);
            String existing;
            synchronized (streamedOutput) {
                existing = streamedOutput.toString().trim();
            }
            String message = context.getString(R.string.tool_shell_exec_failed, describeException(e));
            return error(existing.length() == 0 ? message : truncateOutput(existing, context) + "\n" + message);
        }
    }

    private String truncateOutput(String output, ToolContext context) {
        if (output == null || output.length() <= ToolResult.MAX_TOOL_RESULT_CHARS) {
            return output;
        }
        int lines = 1;
        for (int i = 0; i < output.length(); i++) {
            if (output.charAt(i) == '\n') lines++;
        }
        String truncated = ToolResult.truncateContent(output);
        return text(context, R.string.tool_shell_output_line_count,
                "[" + lines + " lines] ", lines) + truncated;
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static void restoreInterrupt(Exception error) {
        ExceptionUtils.restoreInterrupt(error);
    }

    private static String describeException(Exception error) {
        return ExceptionUtils.describeException(error);
    }

    @Override
    public Class<? extends cn.lineai.tool.ToolCallCardView> getToolCallViewClass() {
        return cn.lineai.tool.ui.ToolCallShellView.class;
    }
}
