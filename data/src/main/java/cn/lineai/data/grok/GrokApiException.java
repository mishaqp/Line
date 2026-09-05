package cn.lineai.data.grok;

public final class GrokApiException extends Exception {
    private final int statusCode;

    public GrokApiException(int statusCode, String message) {
        super(message == null ? "Grok request failed" : message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isUnauthorized() {
        return statusCode == 401 || statusCode == 403;
    }
}
