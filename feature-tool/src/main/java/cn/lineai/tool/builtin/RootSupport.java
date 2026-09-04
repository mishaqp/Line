package cn.lineai.tool.builtin;

import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.tool.ToolContext;

/**
 * Root 执行目标的共享入口。
 *
 * <p>文件工具与 {@code shell_execute} 都通过这里判断“当前是否 root 目标”，
 * 并复用同一个 root 命令执行器（{@link RootShellExecutor} 的健康检查结果带缓存，
 * 避免每条命令都额外 fork 一次 {@code su}）。</p>
 *
 * <p>单元测试通过 {@link #install(RunnerFactory)} 注入伪造执行器，
 * 因此 root 命令拼装、输出解析与错误分支都能在 JVM 上验证。</p>
 */
public final class RootSupport {

    /** root 可用性探测；由 {@link RootShellExecutor} 实现。 */
    public interface Availability {
        boolean isRootAvailable(long timeoutMs);

        void invalidateAvailability();
    }

    /** 创建一对（命令执行器, 可用性探测）。 */
    public interface RunnerFactory {
        RootCommandRunner runner();

        Availability availability();
    }

    private static volatile RunnerFactory factory = defaultFactory();
    private static volatile RunnerFactory installed;

    private RootSupport() {
    }

    /** 当前执行目标是否为 Root (su)。 */
    public static boolean isRootMode(ToolContext context) {
        if (context == null) {
            return false;
        }
        ToolSettingsStore store = context.getToolSettingsStore();
        return store != null && ToolSettingsStore.EXECUTION_ROOT.equals(store.getExecutionMode());
    }

    /** 当前执行目标是否使用本机文件系统（本地工作区或 root）。 */
    public static boolean isLocalFileSystemMode(ToolContext context) {
        if (context == null) {
            return false;
        }
        ToolSettingsStore store = context.getToolSettingsStore();
        return store != null && ToolSettingsStore.isLocalFileSystemExecution(store.getExecutionMode());
    }

    /** 共享的 root 命令执行器。 */
    public static RootCommandRunner commandRunner() {
        return current().runner();
    }

    /** 共享的 root 可用性探测。 */
    public static Availability availability() {
        return current().availability();
    }

    /** 共享的 root 文件执行器。 */
    public static RootFileExecutor fileExecutor() {
        return new RootFileExecutor(commandRunner());
    }

    /** 解析 root 模式下的绝对路径；相对路径基于工作区根目录。 */
    public static String resolve(ToolContext context, String path) throws RootFileExecutor.RootFsException {
        String home = context == null ? "" : context.getHomePath();
        return RootFileExecutor.absolutePath(home, path);
    }

    /** root 模式下的展示路径（不抛异常）。 */
    public static String displayPath(ToolContext context, String path) {
        String home = context == null ? "" : context.getHomePath();
        return RootFileExecutor.displayPath(home, path);
    }

    /** 切换执行目标或 root 授权状态变化时，丢弃健康检查缓存。 */
    public static void invalidateAvailability() {
        current().availability().invalidateAvailability();
    }

    /** 注入自定义执行器工厂；传入 null 恢复默认实现。 */
    public static void install(RunnerFactory next) {
        installed = next;
    }

    private static RunnerFactory current() {
        RunnerFactory value = installed;
        return value != null ? value : factory;
    }

    private static RunnerFactory defaultFactory() {
        final RootShellExecutor executor = new RootShellExecutor();
        return new RunnerFactory() {
            @Override
            public RootCommandRunner runner() {
                return executor;
            }

            @Override
            public Availability availability() {
                return executor;
            }
        };
    }
}
