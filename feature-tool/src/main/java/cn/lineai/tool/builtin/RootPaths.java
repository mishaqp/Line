package cn.lineai.tool.builtin;

/**
 * Paths that {@code su} can actually see on Magisk / KernelSU.
 *
 * <p>The app workspace is often {@code /storage/emulated/0}. In the isolated
 * mount namespace used by {@code su -c}, that FUSE view is missing or not
 * enterable, while {@code /data} and {@code /data/media/0} stay mounted.
 * Shell therefore starts in {@code /data}. File tools rewrite emulated
 * prefixes to {@code /data/media/0} so list/read/write still hit user files.</p>
 */
public final class RootPaths {
    public static final String SHELL_HOME = "/data";
    public static final String MEDIA_HOME = "/data/media/0";

    private static final String[] EMULATED_PREFIXES = {
            "/storage/emulated/0",
            "/storage/self/primary",
            "/mnt/user/0/primary",
            "/sdcard"
    };

    private RootPaths() {
    }

    /** Working directory for {@code su -c}. Empty or emulated storage → {@code /data}. */
    public static String shellCwd(String requested) {
        String path = normalize(requested);
        if (path.length() == 0 || isEmulatedUserStorage(path)) {
            return SHELL_HOME;
        }
        return toRootVisible(path);
    }

    /** Rewrite emulated/FUSE user storage to the real ext4 tree. Other paths unchanged. */
    public static String toRootVisible(String path) {
        String value = normalize(path);
        if (value.length() == 0) {
            return value;
        }
        for (int i = 0; i < EMULATED_PREFIXES.length; i++) {
            String prefix = EMULATED_PREFIXES[i];
            if (value.equals(prefix)) {
                return MEDIA_HOME;
            }
            if (value.startsWith(prefix + "/")) {
                return MEDIA_HOME + value.substring(prefix.length());
            }
        }
        return value;
    }

    public static boolean isEmulatedUserStorage(String path) {
        String value = normalize(path);
        if (value.length() == 0) {
            return false;
        }
        for (int i = 0; i < EMULATED_PREFIXES.length; i++) {
            String prefix = EMULATED_PREFIXES[i];
            if (value.equals(prefix) || value.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    static String normalize(String path) {
        if (path == null) {
            return "";
        }
        String value = path.trim().replace('\\', '/');
        if (value.startsWith("file://")) {
            value = value.substring("file://".length());
        }
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
