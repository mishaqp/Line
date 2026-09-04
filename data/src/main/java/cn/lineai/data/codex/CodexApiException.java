package cn.lineai.data.codex;

/**
 * Safe error for Codex account endpoints. It intentionally carries only the
 * HTTP status and a short message, never a response body or credential.
 */
public final class CodexApiException extends Exception {
    private final int statusCode;

    public CodexApiException(int statusCode, String message) {
        super(message == null ? "Codex request failed" : message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isUnauthorized() {
        return statusCode == 401;
    }
}
