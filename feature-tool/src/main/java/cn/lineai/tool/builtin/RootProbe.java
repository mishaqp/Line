package cn.lineai.tool.builtin;

import java.io.IOException;

/**
 * Classifies a {@code su} health check before the first mutating tool call.
 *
 * <p>MISSING — no {@code su} binary; TIMEOUT — su waited for a prompt / hung;
 * DENIED — ran but not uid 0; READY — {@code id -u} is 0.</p>
 */
public final class RootProbe {
    public enum Status {
        UNKNOWN,
        READY,
        DENIED,
        TIMEOUT,
        MISSING;

        public String shortLabel() {
            switch (this) {
                case READY:
                    return "su uid=0";
                case TIMEOUT:
                    return "su hung";
                case MISSING:
                    return "no su";
                case DENIED:
                    return "su denied";
                default:
                    return "su ?";
            }
        }
    }

    private RootProbe() {
    }

    public static Status fromResult(RootCommandRunner.Result result) {
        if (result == null) {
            return Status.DENIED;
        }
        if (result.isSuccess()) {
            String line = result.firstLine();
            if ("0".equals(line) || line.contains("uid=0")) {
                return Status.READY;
            }
        }
        return Status.DENIED;
    }

    public static Status fromFailure(Exception error) {
        if (error instanceof RootShellExecutor.RootTimeoutException) {
            return Status.TIMEOUT;
        }
        if (error instanceof IOException) {
            return Status.MISSING;
        }
        String message = error == null || error.getMessage() == null ? "" : error.getMessage().toLowerCase();
        if (message.contains("error=2")
                || message.contains("no such file")
                || message.contains("cannot run")
                || message.contains("not found")) {
            return Status.MISSING;
        }
        return Status.DENIED;
    }

    public static String describe(Status status) {
        if (status == null) {
            status = Status.UNKNOWN;
        }
        switch (status) {
            case READY:
                return "Grant root permission is not required; su is ready.";
            case TIMEOUT:
                return "su is interactive or hung. Grant root in KernelSU/Magisk and retry. stdin is closed so this is not a password prompt hang in Line.";
            case MISSING:
                return "su binary not found. Root execution needs KernelSU, Magisk or equivalent.";
            case DENIED:
                return "Grant root permission in KernelSU/Magisk (su -c id -u must print 0).";
            default:
                return "Root availability is unknown.";
        }
    }
}
