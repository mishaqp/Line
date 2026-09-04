package cn.lineai.tool.builtin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RootCommandRunner} 的测试替身：按“脚本包含某片段”返回预置输出，
 * 并记录全部调用，便于断言 root 命令的拼装方式。
 */
final class FakeRootRunner implements RootCommandRunner {

    static final class Call {
        final String script;
        final String stdin;

        Call(String script, byte[] stdin) {
            this.script = script;
            this.stdin = stdin == null ? null : new String(stdin, java.nio.charset.StandardCharsets.US_ASCII);
        }
    }

    private final List<Call> calls = new ArrayList<>();
    private final Map<String, Result> responses = new LinkedHashMap<>();
    private Result fallback = new Result("", 0);
    private Exception failure;

    FakeRootRunner on(String scriptFragment, String output, int exitCode) {
        responses.put(scriptFragment, new Result(output, exitCode));
        return this;
    }

    FakeRootRunner fallback(String output, int exitCode) {
        this.fallback = new Result(output, exitCode);
        return this;
    }

    FakeRootRunner failWith(Exception error) {
        this.failure = error;
        return this;
    }

    List<Call> calls() {
        return calls;
    }

    String lastScript() {
        return calls.isEmpty() ? "" : calls.get(calls.size() - 1).script;
    }

    @Override
    public Result run(String script, byte[] stdin, long timeoutMs) throws Exception {
        calls.add(new Call(script, stdin));
        if (failure != null) {
            throw failure;
        }
        for (Map.Entry<String, Result> entry : responses.entrySet()) {
            if (script != null && script.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return fallback;
    }
}
