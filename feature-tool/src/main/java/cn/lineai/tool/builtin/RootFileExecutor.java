package cn.lineai.tool.builtin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Root 执行目标下的文件操作实现。
 *
 * <p>应用进程本身没有权限读写系统目录（{@code /data/data/...}、{@code /system}、其它应用的
 * 私有目录），因此这里所有元数据查询和读写都通过 {@link RootCommandRunner}（{@code su -c}）完成，
 * Java 侧只负责路径拼装、输出解析和结果排序。</p>
 *
 * <p>约定：</p>
 * <ul>
 *   <li>路径统一解析为绝对路径（相对路径基于工作区根目录），并做单引号转义；</li>
 *   <li>写入把 base64 内容通过 stdin 交给 {@code base64 -d}，
 *       既避免内容里的引号/换行破坏 shell 脚本，也不会撞上内核 ARG_MAX；</li>
 *   <li>元数据一次命令合并查询（{@code test -e / -d / wc -c}），减少 fork 次数。</li>
 * </ul>
 */
public final class RootFileExecutor {

    /** 单次读取/写入的上限，防止把超大文件整体拉进内存。 */
    private static final long MAX_TRANSFER_BYTES = 8L * 1024L * 1024L;

    private final RootCommandRunner runner;
    private final long timeoutMs;

    public RootFileExecutor(RootCommandRunner runner) {
        this(runner, 30_000L);
    }

    public RootFileExecutor(RootCommandRunner runner, long timeoutMs) {
        this.runner = runner;
        this.timeoutMs = Math.max(1000L, timeoutMs);
    }

    // ── 可用性 ──

    /** root 是否可用；不可用时抛出带说明的异常，由各工具转成 ToolResult 错误。 */
    public void requireRoot() throws RootFsException {
        if (!(runner instanceof RootShellExecutor) || ((RootShellExecutor) runner).isRootAvailable(timeoutMs)) {
            return;
        }
        throw new RootFsException("root unavailable: `su -c 'id -u'` did not return 0. "
                + "Grant root permission to this app in KernelSU/Magisk, then retry.");
    }

    // ── 路径 ──

    /** 把工作区相对路径解析为绝对路径（不做 canonical 校验：root 模式下允许访问整个文件系统）。 */
    public static String absolutePath(String homePath, String inputPath) throws RootFsException {
        String value = inputPath == null ? "" : inputPath.trim();
        if (value.length() == 0) {
            String home = homePath == null ? "" : homePath.trim();
            if (home.length() == 0) {
                throw new RootFsException("path is empty and no workspace path is set");
            }
            return home;
        }
        if (value.startsWith("file://")) {
            value = value.substring("file://".length());
        }
        if (value.startsWith("/")) {
            return value;
        }
        String home = homePath == null ? "" : homePath.trim();
        if (home.length() == 0) {
            throw new RootFsException("relative path given but no workspace path is set: " + inputPath);
        }
        String base = home.endsWith("/") ? home.substring(0, home.length() - 1) : home;
        return base + "/" + value;
    }

    // ── 元数据 ──

    /** 一次命令查询存在性 / 是否目录 / 字节大小。 */
    public Meta stat(String path) throws RootFsException {
        String quoted = RootCommandRunner.quote(path);
        String script = "if [ -e " + quoted + " ]; then printf 'exists=1\\n'; else printf 'exists=0\\n'; fi; "
                + "if [ -d " + quoted + " ]; then printf 'dir=1\\n'; else printf 'dir=0\\n'; fi; "
                + "printf 'size=%s\\n' \"$(wc -c < " + quoted + " 2>/dev/null | tr -d ' \\n')\"";
        RootCommandRunner.Result result = exec(script);
        boolean exists = false;
        boolean directory = false;
        long size = 0L;
        for (String line : lines(result.getOutput())) {
            if (line.startsWith("exists=")) {
                exists = "1".equals(line.substring("exists=".length()).trim());
            } else if (line.startsWith("dir=")) {
                directory = "1".equals(line.substring("dir=".length()).trim());
            } else if (line.startsWith("size=")) {
                size = parseLong(line.substring("size=".length()).trim());
            }
        }
        return new Meta(exists, directory, size);
    }

    // ── 读取 ──

    /** 读取 [startByte, endByte) 区间；字节数为 0 时返回空串。 */
    public String readRange(String path, long startByte, long length) throws RootFsException {
        if (length <= 0) {
            return "";
        }
        if (length > MAX_TRANSFER_BYTES) {
            throw new RootFsException("requested read size exceeds " + (MAX_TRANSFER_BYTES / 1024) + "KB");
        }
        String script = "dd if=" + RootCommandRunner.quote(path)
                + " bs=1 skip=" + Math.max(0L, startByte)
                + " count=" + length
                + " 2>/dev/null";
        return exec(script).getOutput();
    }

    /** 读取整个文件（受 {@link #MAX_TRANSFER_BYTES} 限制）。 */
    public String readAll(String path) throws RootFsException {
        return readRange(path, 0L, MAX_TRANSFER_BYTES);
    }

    // ── 写入 ──

    /** 以 root 身份写入文本/二进制内容，自动创建父目录，并把属主交还给调用方 uid。 */
    public WriteResult write(String path, byte[] content) throws RootFsException {
        if (content == null) {
            content = new byte[0];
        }
        if (content.length > MAX_TRANSFER_BYTES) {
            throw new RootFsException("content exceeds " + (MAX_TRANSFER_BYTES / 1024) + "KB");
        }
        Meta meta = stat(path);
        boolean existed = meta.exists && !meta.directory;
        if (meta.exists && meta.directory) {
            throw new RootFsException("path is a directory: " + path);
        }
        String encoded = Base64.getEncoder().encodeToString(content);
        String parent = parentOf(path);
        StringBuilder script = new StringBuilder();
        if (parent.length() > 0) {
            script.append("mkdir -p ").append(RootCommandRunner.quote(parent)).append(" || exit 3; ");
        }
        // base64 通过 stdin 传入：放进 argv 会超出内核 ARG_MAX，导致大文件写入直接失败。
        script.append("base64 -d > ").append(RootCommandRunner.quote(path)).append(" || exit 4; ");
        script.append("chown ").append(currentUid()).append(':').append(currentUid()).append(' ')
                .append(RootCommandRunner.quote(path)).append(" 2>/dev/null; exit 0");
        RootCommandRunner.Result result = exec(script.toString(), encoded.getBytes(StandardCharsets.US_ASCII));
        if (!result.isSuccess()) {
            throw new RootFsException("write failed (exit " + result.getExitCode() + "): " + firstLine(result));
        }
        return new WriteResult(existed, content.length);
    }

    // ── 删除 ──

    public void delete(String path) throws RootFsException {
        RootCommandRunner.Result result = exec("rm -rf " + RootCommandRunner.quote(path));
        if (!result.isSuccess()) {
            throw new RootFsException("delete failed (exit " + result.getExitCode() + "): " + firstLine(result));
        }
    }

    // ── 目录列举 ──

    /** 列出目录的直接子项（目录在前、名称不区分大小写排序）。 */
    public List<Entry> listChildren(String path) throws RootFsException {
        String script = "find " + RootCommandRunner.quote(path) + " -mindepth 1 -maxdepth 1 2>/dev/null";
        List<String> paths = new ArrayList<>();
        for (String line : lines(exec(script).getOutput())) {
            if (line.length() > 0) {
                paths.add(line);
            }
        }
        if (paths.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> directories = testFlags(paths, "-d");
        List<Entry> entries = new ArrayList<>(paths.size());
        for (String child : paths) {
            entries.add(new Entry(nameOf(child), child, directories.contains(child)));
        }
        Collections.sort(entries, ENTRY_COMPARATOR);
        return entries;
    }

    /** 递归收集文件（最多 {@code limit} 条），用于 glob 的候选集。 */
    public List<String> collectFiles(String root, int limit) throws RootFsException {
        String script = "find " + RootCommandRunner.quote(root) + " -type f 2>/dev/null";
        List<String> files = new ArrayList<>();
        for (String line : lines(exec(script).getOutput())) {
            if (line.length() == 0) {
                continue;
            }
            files.add(line);
            if (files.size() >= limit) {
                break;
            }
        }
        Collections.sort(files);
        return files;
    }

    /** 递归收集 [DIR] 形式的目录树条目，格式 {@code [DIR]  relative/} / {@code [FILE] relative}。 */
    public List<String> collectTree(String root, int limit) throws RootFsException {
        String script = "find " + RootCommandRunner.quote(root) + " -mindepth 1 2>/dev/null";
        List<String> all = new ArrayList<>();
        for (String line : lines(exec(script).getOutput())) {
            if (line.length() > 0) {
                all.add(line);
            }
        }
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> directories = testFlags(all, "-d");
        List<String> rendered = new ArrayList<>(all.size());
        String prefix = root.endsWith("/") ? root : root + "/";
        for (String path : all) {
            String relative = path.startsWith(prefix) ? path.substring(prefix.length()) : path;
            if (relative.length() == 0) {
                continue;
            }
            rendered.add((directories.contains(path) ? "[DIR]  " + relative + "/" : "[FILE] " + relative));
            if (rendered.size() >= limit) {
                break;
            }
        }
        return rendered;
    }

    // ── 内部辅助 ──

    private RootCommandRunner.Result exec(String script) throws RootFsException {
        return exec(script, null);
    }

    private RootCommandRunner.Result exec(String script, byte[] stdin) throws RootFsException {
        try {
            return runner.run(script, stdin, timeoutMs);
        } catch (RootShellExecutor.RootTimeoutException e) {
            throw new RootFsException("root command timed out after " + timeoutMs + "ms");
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RootFsException("root command failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    /** 用一条 {@code if [ -d p ]; ...} 脚本批量判定标志位，返回命中该标志的路径。 */
    private List<String> testFlags(List<String> paths, String flag) throws RootFsException {
        StringBuilder script = new StringBuilder();
        for (String path : paths) {
            String quoted = RootCommandRunner.quote(path);
            script.append("if [ ").append(flag).append(' ').append(quoted).append(" ]; then printf '%s\\n' ")
                    .append(quoted).append("; fi; ");
        }
        List<String> hits = new ArrayList<>();
        for (String line : lines(exec(script.toString()).getOutput())) {
            hits.add(line);
        }
        return hits;
    }

    private static List<String> lines(String output) {
        List<String> result = new ArrayList<>();
        if (output == null || output.length() == 0) {
            return result;
        }
        for (String line : output.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.length() > 0) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static String firstLine(RootCommandRunner.Result result) {
        String line = result.firstLine();
        return line.length() == 0 ? "(no output)" : line;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    static String parentOf(String path) {
        int index = path.lastIndexOf('/');
        if (index <= 0) {
            return "";
        }
        return path.substring(0, index);
    }

    static String nameOf(String path) {
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }

    private static int currentUid() {
        try {
            return android.os.Process.myUid();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static final Comparator<Entry> ENTRY_COMPARATOR = (left, right) -> {
        if (left.isDirectory() != right.isDirectory()) {
            return left.isDirectory() ? -1 : 1;
        }
        return left.getName().compareToIgnoreCase(right.getName());
    };

    /** 一次 stat 的结果。 */
    public static final class Meta {
        private final boolean exists;
        private final boolean directory;
        private final long size;

        Meta(boolean exists, boolean directory, long size) {
            this.exists = exists;
            this.directory = directory;
            this.size = size;
        }

        public boolean exists() {
            return exists;
        }

        public boolean isDirectory() {
            return directory;
        }

        public boolean isFile() {
            return exists && !directory;
        }

        public long size() {
            return size;
        }
    }

    /** 目录条目。 */
    public static final class Entry {
        private final String name;
        private final String path;
        private final boolean directory;

        Entry(String name, String path, boolean directory) {
            this.name = name;
            this.path = path;
            this.directory = directory;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public boolean isDirectory() {
            return directory;
        }
    }

    /** 写入结果。 */
    public static final class WriteResult {
        private final boolean existed;
        private final int bytes;

        WriteResult(boolean existed, int bytes) {
            this.existed = existed;
            this.bytes = bytes;
        }

        public boolean existed() {
            return existed;
        }

        public int bytes() {
            return bytes;
        }
    }

    /** root 文件操作失败（含 root 不可用、超时、命令非 0 退出）。 */
    public static final class RootFsException extends Exception {
        public RootFsException(String message) {
            super(message);
        }
    }

    /** 便捷入口：把字符串按 UTF-8 编码。 */
    public static byte[] utf8(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }

    /** 便捷入口：{@link File} 无关的路径拼接。 */
    public static String join(String parent, String name) {
        if (parent == null || parent.length() == 0) {
            return name;
        }
        return parent.endsWith("/") ? parent + name : parent + "/" + name;
    }

    /**
     * root 模式下的展示路径：与 {@code FileToolPathPolicy.displayPath} 语义一致，
     * 但在工作区路径为空时不抛异常（root 模式允许直接操作绝对路径）。
     */
    public static String displayPath(String homePath, String path) {
        String value = path == null ? "" : path.trim().replace('\\', '/');
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        String root = homePath == null ? "" : homePath.trim().replace('\\', '/');
        while (root.length() > 1 && root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        if (root.length() == 0 || !value.startsWith("/")) {
            return value;
        }
        if (value.equals(root)) {
            return ".";
        }
        String prefix = root + "/";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }
}
