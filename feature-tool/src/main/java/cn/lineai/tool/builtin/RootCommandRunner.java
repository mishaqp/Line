package cn.lineai.tool.builtin;

import java.util.List;

/**
 * 以 root 身份执行一条 shell 脚本的最小抽象。
 *
 * <p>生产实现是 {@link RootShellExecutor}（{@code su -c}）；单元测试注入伪造实现，
 * 因此 root 命令的拼装、解析和错误处理都可以在 JVM 上验证，无需真实 root 设备。</p>
 */
public interface RootCommandRunner {

    /**
     * 以 root 身份执行 {@code script}（不向子进程写入任何 stdin）。
     */
    default Result run(String script, long timeoutMs) throws Exception {
        return run(script, null, timeoutMs);
    }

    /**
     * 以 root 身份执行 {@code script}。
     *
     * @param script    交给 {@code sh -c} 的脚本内容
     * @param stdin     写入子进程标准输入的内容；为 null 时 stdin 直接关闭
     *                  （绝不能让它保持打开：KernelSU 未授权时 {@code su} 会等待输入而挂起）
     * @param timeoutMs 超时（毫秒）
     * @return 合并 stdout/stderr 后的输出与退出码
     * @throws Exception 进程无法启动、超时或被中断
     */
    Result run(String script, byte[] stdin, long timeoutMs) throws Exception;

    /** 一次 root 命令的结果。 */
    final class Result {
        private final String output;
        private final int exitCode;

        public Result(String output, int exitCode) {
            this.output = output == null ? "" : output;
            this.exitCode = exitCode;
        }

        public String getOutput() {
            return output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        /** 首行去空白后的内容，便于解析 {@code id -u}、{@code wc -c} 这类单值输出。 */
        public String firstLine() {
            int index = output.indexOf('\n');
            String line = index < 0 ? output : output.substring(0, index);
            return line.trim();
        }
    }

    /** 供 {@code su -c} 使用的单引号转义。 */
    static String quote(String value) {
        return "'" + (value == null ? "" : value).replace("'", "'\\''") + "'";
    }

    /** 供参数数组使用的默认实现入口。 */
    static List<String> suArguments(String script) {
        java.util.ArrayList<String> args = new java.util.ArrayList<>(3);
        args.add("su");
        args.add("-c");
        args.add(script);
        return args;
    }
}
