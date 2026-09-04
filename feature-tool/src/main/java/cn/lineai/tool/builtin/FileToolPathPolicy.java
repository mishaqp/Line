package cn.lineai.tool.builtin;

import cn.lineai.tool.ToolContext;
import java.io.File;
import java.io.IOException;

public final class FileToolPathPolicy {
    private FileToolPathPolicy() {
    }

    public static File resolve(String homePath, String inputPath) throws IOException {
        return resolve(homePath, java.util.Collections.emptyList(), inputPath, false);
    }

    public static File resolve(ToolContext context, String inputPath) throws IOException {
        if (context == null) {
            throw new IOException("Tool context is empty");
        }
        return resolve(context.getHomePath(), context.getExtraWriteRoots(), inputPath, context.isBypassPathProtection());
    }

    /**
     * Root 执行目标下的路径解析：允许访问整个文件系统，因此不做工作区边界校验。
     * 只用于 {@code su} 侧的路径拼装/展示——应用进程本身并不一定有权限读写这些路径。
     */
    public static File resolveForRoot(ToolContext context, String inputPath) throws IOException {
        if (context == null) {
            throw new IOException("Tool context is empty");
        }
        String homePath = context.getHomePath();
        if (homePath == null || homePath.trim().length() == 0) {
            throw new IOException("Workspace path is empty");
        }
        String rawPath = inputPath == null ? "" : inputPath.trim();
        File root = new File(homePath).getCanonicalFile();
        File target = rawPath.length() == 0
                ? root
                : new File(rawPath).isAbsolute() ? new File(rawPath) : new File(root, rawPath);
        return target.getCanonicalFile();
    }

    private static File resolve(String homePath, java.util.List<String> extraRoots, String inputPath, boolean bypassProtection) throws IOException {
        if (homePath == null || homePath.trim().length() == 0) {
            throw new IOException("Workspace path is empty");
        }
        String rawPath = inputPath == null ? "" : inputPath.trim();
        File root = new File(homePath).getCanonicalFile();
        File target = rawPath.length() == 0
                ? root
                : new File(rawPath).isAbsolute() ? new File(rawPath) : new File(root, rawPath);
        File canonical = target.getCanonicalFile();
        if (bypassProtection) {
            return canonical;
        }
        if (!isInside(root, canonical)) {
            File allowedRoot = matchingExtraRoot(extraRoots, canonical);
            if (allowedRoot == null) {
                throw new IOException("Path is outside the current workspace and authorized Skills directory: " + rawPath);
            }
        }
        return canonical;
    }

    public static String displayPath(String homePath, File file) throws IOException {
        File root = new File(homePath).getCanonicalFile();
        File target = file.getCanonicalFile();
        String rootPath = root.getPath();
        String targetPath = target.getPath();
        if (targetPath.equals(rootPath)) {
            return ".";
        }
        if (targetPath.startsWith(rootPath + File.separator)) {
            return targetPath.substring(rootPath.length() + 1);
        }
        return targetPath;
    }

    public static boolean isInside(File root, File target) {
        String rootPath = root.getPath();
        String targetPath = target.getPath();
        return targetPath.equals(rootPath) || targetPath.startsWith(rootPath + File.separator);
    }

    private static File matchingExtraRoot(java.util.List<String> extraRoots, File target) throws IOException {
        if (extraRoots == null || extraRoots.isEmpty()) {
            return null;
        }
        for (String rootPath : extraRoots) {
            if (rootPath == null || rootPath.trim().length() == 0) {
                continue;
            }
            File root = new File(rootPath.trim()).getCanonicalFile();
            if (isInside(root, target)) {
                return root;
            }
        }
        return null;
    }
}
