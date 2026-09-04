package cn.lineai.tool.builtin;

import cn.lineai.tool.ExceptionUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 通过 {@code su -c} 在已 root 的设备（KernelSU / KernelSU Next / Magisk）上执行命令。
 *
 * <p>关键约束：</p>
 * <ul>
 *   <li>stdin 始终重定向自 {@code /dev/null}：KernelSU 未授权时 {@code su} 会等待输入，
 *       不重定向会让工具调用永久挂起（参见 shell_execute 之前的挂起修复）。</li>
 *   <li>先做 {@code su -c 'id -u'} 健康检查并缓存结果，root 未授予时立即返回明确错误，
 *       而不是让模型拿到一条空输出。</li>
 *   <li>stdout/stderr 合并读取，超时后销毁进程并返回已收集到的输出。</li>
 * </ul>
 */
public final class RootShellExecutor implements RootCommandRunner, RootSupport.Availability {

    /** 健康检查结果的缓存时长，避免每条命令都额外 fork 一次 su。 */
    private static final long AVAILABILITY_CACHE_MS = 10_000L;

    private final Object lock = new Object();
    private long availabilityCheckedAt;
    private boolean availabilityCached;
    private boolean availabilityValue;
    private RootProbe.Status availabilityStatus = RootProbe.Status.UNKNOWN;

    /** 忘记缓存的健康检查结果（设置页切换执行目标、授权状态变化时调用）。 */
    @Override
    public void invalidateAvailability() {
        synchronized (lock) {
            availabilityCached = false;
            availabilityValue = false;
            availabilityStatus = RootProbe.Status.UNKNOWN;
            availabilityCheckedAt = 0L;
        }
    }

    /** root 是否可用（{@code su -c 'id -u'} 返回 0）。 */
    @Override
    public boolean isRootAvailable(long timeoutMs) {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            if (availabilityCached && now - availabilityCheckedAt < AVAILABILITY_CACHE_MS) {
                return availabilityValue;
            }
        }
        RootProbe.Status status;
        boolean available;
        try {
            Result result = runScript("id -u", timeoutMs);
            status = RootProbe.fromResult(result);
            available = status == RootProbe.Status.READY;
        } catch (Exception e) {
            ExceptionUtils.restoreInterrupt(e);
            status = RootProbe.fromFailure(e);
            available = false;
        }
        synchronized (lock) {
            availabilityCached = true;
            availabilityValue = available;
            availabilityStatus = status;
            availabilityCheckedAt = System.currentTimeMillis();
        }
        return available;
    }

    @Override
    public RootProbe.Status probe(long timeoutMs) {
        isRootAvailable(timeoutMs);
        synchronized (lock) {
            return availabilityStatus;
        }
    }

    @Override
    public Result run(String script, byte[] stdin, long timeoutMs) throws Exception {
        return runScript(script, stdin, timeoutMs);
    }

    private Result runScript(String script, long timeoutMs) throws Exception {
        return runScript(script, null, timeoutMs);
    }

    private Result runScript(String script, byte[] stdin, long timeoutMs) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("su", "-c", script == null ? "" : script);
        builder.redirectErrorStream(true);
        if (stdin == null) {
            builder.redirectInput(ProcessBuilder.Redirect.from(new java.io.File("/dev/null")));
        }
        Process process = builder.start();
        if (stdin != null) {
            java.io.OutputStream pipe = process.getOutputStream();
            try {
                pipe.write(stdin);
                pipe.flush();
            } finally {
                pipe.close();
            }
        }
        Reader reader = new Reader(process.getInputStream());
        reader.start();
        boolean finished = process.waitFor(Math.max(500L, timeoutMs), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroy();
            if (!process.waitFor(200L, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
            reader.join(500L);
            String partial = reader.snapshot();
            throw new RootTimeoutException(partial);
        }
        reader.join(1000L);
        return new Result(reader.snapshot(), process.exitValue());
    }

    /** root 命令超时；携带超时前已经产生的输出。 */
    public static final class RootTimeoutException extends Exception {
        private final String partialOutput;

        public RootTimeoutException(String partialOutput) {
            super("root command timed out");
            this.partialOutput = partialOutput == null ? "" : partialOutput;
        }

        public String getPartialOutput() {
            return partialOutput;
        }
    }

    /** 后台读取进程输出，避免管道写满导致子进程阻塞。 */
    private static final class Reader extends Thread {
        private final InputStream input;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Reader(InputStream input) {
            super("root-command-reader");
            this.input = input;
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] chunk = new byte[8192];
            try {
                int read;
                while ((read = input.read(chunk)) != -1) {
                    synchronized (buffer) {
                        buffer.write(chunk, 0, read);
                    }
                }
            } catch (Exception ignored) {
                // 进程被销毁时读取会失败，已收集的内容仍然可用
            }
        }

        String snapshot() {
            synchronized (buffer) {
                return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }
}
