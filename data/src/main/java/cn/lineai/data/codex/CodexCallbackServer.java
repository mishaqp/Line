package cn.lineai.data.codex;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loopback callback receiver for the native Codex PKCE flow.
 */
final class CodexCallbackServer {
    interface Callback {
        void onResult(String code, String state, String error);
    }

    private static final String TAG = "CodexCallback";
    private final int port;
    private final Callback callback;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread thread;

    CodexCallbackServer(int port, Callback callback) {
        this.port = port;
        this.callback = callback;
    }

    synchronized boolean start() {
        if (running) {
            return true;
        }
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            thread = new Thread(this::acceptLoop, "linecode-codex-callback");
            thread.start();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Unable to bind OAuth callback port: " + e.getClass().getSimpleName());
            serverSocket = null;
            running = false;
            return false;
        }
    }

    synchronized void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {
            }
            serverSocket = null;
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                java.net.Socket socket = serverSocket.accept();
                handle(socket);
            } catch (Exception e) {
                if (running) {
                    Log.w(TAG, "OAuth callback server error: " + e.getClass().getSimpleName());
                }
            }
        }
    }

    private void handle(java.net.Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }
            String line;
            while ((line = reader.readLine()) != null && line.length() > 0) {
                // Consume headers before writing the response.
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2 || !"GET".equalsIgnoreCase(parts[0])) {
                writeResponse(socket, 400, "Invalid callback request.");
                return;
            }
            URI uri = new URI("http://localhost" + parts[1]);
            Map<String, String> params = queryParams(uri.getRawQuery());
            String code = params.get("code");
            String state = params.get("state");
            String error = params.get("error");
            writeResponse(socket, 200, "Authorization complete. You can close this tab.");
            if (callback != null) {
                callback.onResult(code == null ? "" : code, state, error);
            }
        } catch (Exception e) {
            Log.w(TAG, "Invalid OAuth callback: " + e.getClass().getSimpleName());
            try {
                writeResponse(socket, 400, "Invalid callback.");
            } catch (Exception ignored) {
            }
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            stop();
        }
    }

    private Map<String, String> queryParams(String query) throws Exception {
        HashMap<String, String> result = new HashMap<>();
        if (query == null || query.length() == 0) {
            return result;
        }
        for (String item : query.split("&")) {
            String[] pair = item.split("=", 2);
            String key = URLDecoder.decode(pair[0], "UTF-8");
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], "UTF-8") : "";
            result.put(key, value);
        }
        return result;
    }

    private void writeResponse(java.net.Socket socket, int code, String message) throws Exception {
        byte[] body = ("<html><body><h2>" + message + "</h2></body></html>")
                .getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + code + " OK\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        socket.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write(body);
        socket.getOutputStream().flush();
    }
}
