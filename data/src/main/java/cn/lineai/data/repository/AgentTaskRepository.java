package cn.lineai.data.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import cn.lineai.data.db.LineCodeDatabase;
import cn.lineai.data.db.LineCodeSchema;
import cn.lineai.model.AgentTaskEventRecord;
import cn.lineai.model.AgentTaskRecord;
import cn.lineai.model.AgentTaskState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AgentTaskRepository extends BaseRepository {
    private static final String TASKS = LineCodeSchema.TABLE_AGENT_TASKS;
    private static final String EVENTS = LineCodeSchema.TABLE_AGENT_TASK_EVENTS;

    public AgentTaskRepository(LineCodeDatabase database) {
        super(database);
    }

    public synchronized AgentTaskRecord enqueue(
            String conversationId,
            String projectId,
            String workspacePath,
            String userMessageId,
            String prompt,
            String rawInputJson,
            String modelConfigId,
            int maxToolCalls
    ) {
        String taskId = nextId("task");
        long now = System.currentTimeMillis();
        AgentTaskRecord task = new AgentTaskRecord(
                taskId,
                conversationId,
                projectId,
                workspacePath,
                userMessageId,
                prompt,
                rawInputJson,
                modelConfigId,
                AgentTaskState.QUEUED,
                0,
                0,
                maxToolCalls,
                "",
                "",
                0,
                now,
                now,
                0,
                0,
                0
        );
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            insertTask(db, task);
            appendEvent(db, task, "enqueued", "{}");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        database.backupAsync();
        return task;
    }

    public synchronized AgentTaskRecord get(String taskId) {
        if (taskId == null || taskId.trim().length() == 0) {
            return null;
        }
        return queryTask("id = ?", new String[] {taskId}, "updated_at DESC");
    }

    public synchronized AgentTaskRecord getByGenerationId(int generationId) {
        if (generationId <= 0) {
            return null;
        }
        return queryTask("generation_id = ?", new String[] {String.valueOf(generationId)}, "updated_at DESC");
    }

    public synchronized AgentTaskRecord getNextQueued(String conversationId) {
        if (conversationId == null || conversationId.trim().length() == 0) {
            return null;
        }
        return queryTask(
                "conversation_id = ? AND state = ?",
                new String[] {conversationId, AgentTaskState.QUEUED.storageValue()},
                "created_at ASC"
        );
    }

    public synchronized AgentTaskRecord getRecoverable(String conversationId) {
        if (conversationId == null || conversationId.trim().length() == 0) {
            return null;
        }
        return queryTask(
                "conversation_id = ? AND state = ?",
                new String[] {conversationId, AgentTaskState.RECOVERABLE.storageValue()},
                "updated_at ASC"
        );
    }

    public synchronized List<AgentTaskRecord> listForConversation(String conversationId) {
        if (conversationId == null || conversationId.trim().length() == 0) {
            return Collections.emptyList();
        }
        return queryTasks("conversation_id = ?", new String[] {conversationId}, "created_at ASC");
    }

    public synchronized List<AgentTaskEventRecord> listEvents(String taskId, int limit) {
        if (taskId == null || taskId.trim().length() == 0) {
            return Collections.emptyList();
        }
        ArrayList<AgentTaskEventRecord> result = new ArrayList<>();
        String safeLimit = limit <= 0 ? "200" : String.valueOf(Math.min(limit, 1000));
        Cursor cursor = database.getReadableDatabase().query(
                EVENTS,
                null,
                "task_id = ?",
                new String[] {taskId},
                null,
                null,
                "sequence ASC",
                safeLimit
        );
        try {
            while (cursor.moveToNext()) {
                result.add(readEvent(cursor));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public synchronized AgentTaskRecord start(String taskId, int generationId, long deadlineAt) {
        AgentTaskRecord current = get(taskId);
        if (current == null || current.getState().isTerminal()) {
            return current;
        }
        if (current.getState().isInFlight() && current.getGenerationId() != generationId) {
            return current;
        }
        long now = System.currentTimeMillis();
        int resumeCount = current.getResumeCount()
                + (current.getState() == AgentTaskState.QUEUED ? 0 : 1);
        long startedAt = current.getStartedAt() > 0 ? current.getStartedAt() : now;
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("state", AgentTaskState.RUNNING.storageValue());
            values.put("generation_id", Math.max(0, generationId));
            values.put("resume_count", resumeCount);
            values.put("started_at", startedAt);
            values.put("updated_at", now);
            values.put("deadline_at", deadlineAt);
            values.putNull("finished_at");
            db.update(TASKS, values, "id = ?", new String[] {taskId});
            AgentTaskRecord next = get(taskId);
            appendEvent(db, next, "started", payload(
                    "generation_id", generationId,
                    "resume_count", resumeCount,
                    "deadline_at", deadlineAt
            ));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        database.backupAsync();
        return get(taskId);
    }

    public synchronized AgentTaskRecord checkpoint(
            String taskId,
            int generationId,
            String phase,
            int toolCallCount,
            String payloadJson
    ) {
        AgentTaskRecord current = get(taskId);
        if (current == null || current.getState().isTerminal()) {
            return current;
        }
        long now = System.currentTimeMillis();
        int nextToolCallCount = Math.max(current.getToolCallCount(), Math.max(0, toolCallCount));
        String nextPayload = payloadJson == null ? "" : payloadJson;
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("generation_id", Math.max(0, generationId));
            values.put("tool_call_count", nextToolCallCount);
            values.put("checkpoint_json", nextPayload);
            values.put("updated_at", now);
            db.update(TASKS, values, "id = ?", new String[] {taskId});
            AgentTaskRecord next = get(taskId);
            appendEvent(db, next, "checkpoint:" + normalizePhase(phase), nextPayload);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return get(taskId);
    }

    public synchronized AgentTaskRecord finish(
            String taskId,
            AgentTaskState nextState,
            String error,
            String checkpointJson
    ) {
        AgentTaskRecord current = get(taskId);
        if (current == null || current.getState().isTerminal()) {
            return current;
        }
        AgentTaskState state = nextState == null ? AgentTaskState.FAILED : nextState;
        long now = System.currentTimeMillis();
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("state", state.storageValue());
            values.put("updated_at", now);
            values.put("last_error", error == null ? "" : error);
            if (checkpointJson != null) {
                values.put("checkpoint_json", checkpointJson);
            }
            if (state.isTerminal()) {
                values.put("finished_at", now);
                values.put("deadline_at", 0L);
            }
            db.update(TASKS, values, "id = ?", new String[] {taskId});
            AgentTaskRecord next = get(taskId);
            appendEvent(db, next, eventType(state), payload(
                    "error", error == null ? "" : error,
                    "checkpoint", checkpointJson == null ? "" : checkpointJson
            ));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        database.backupAsync();
        return get(taskId);
    }

    public synchronized AgentTaskRecord cancel(String taskId, String reason) {
        AgentTaskRecord current = get(taskId);
        if (current == null || current.getState().isTerminal()) {
            return current;
        }
        return finish(taskId, AgentTaskState.CANCELLED, reason, current.getCheckpointJson());
    }

    public synchronized AgentTaskRecord pause(String taskId, String reason) {
        AgentTaskRecord current = get(taskId);
        if (current == null || current.getState().isTerminal()) {
            return current;
        }
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("state", AgentTaskState.PAUSED.storageValue());
            values.put("updated_at", System.currentTimeMillis());
            values.put("last_error", reason == null ? "" : reason);
            values.put("deadline_at", 0L);
            values.put("generation_id", 0);
            db.update(TASKS, values, "id = ?", new String[] {taskId});
            AgentTaskRecord next = get(taskId);
            appendEvent(db, next, "paused", payload("reason", reason == null ? "" : reason));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        database.backupAsync();
        return get(taskId);
    }

    public synchronized int cancelForConversation(String conversationId, String reason) {
        if (conversationId == null || conversationId.trim().length() == 0) {
            return 0;
        }
        List<AgentTaskRecord> tasks = queryTasks(
                "conversation_id = ? AND state NOT IN (?, ?, ?)",
                new String[] {
                        conversationId,
                        AgentTaskState.SUCCEEDED.storageValue(),
                        AgentTaskState.FAILED.storageValue(),
                        AgentTaskState.CANCELLED.storageValue()
                },
                "created_at ASC"
        );
        int count = 0;
        for (AgentTaskRecord task : tasks) {
            if (cancel(task.getId(), reason) != null) {
                count++;
            }
        }
        return count;
    }

    public synchronized int recoverInFlightTasks() {
        List<AgentTaskRecord> tasks = queryTasks(
                "state IN (?, ?)",
                new String[] {
                        AgentTaskState.RUNNING.storageValue(),
                        AgentTaskState.WAITING_FOR_APPROVAL.storageValue()
                },
                "updated_at ASC"
        );
        if (tasks.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            for (AgentTaskRecord task : tasks) {
                ContentValues values = new ContentValues();
                values.put("state", AgentTaskState.RECOVERABLE.storageValue());
                values.put("generation_id", 0);
                values.put("updated_at", System.currentTimeMillis());
                values.put("last_error", "上一进程未正常结束，可从检查点恢复。");
                db.update(TASKS, values, "id = ?", new String[] {task.getId()});
                AgentTaskRecord next = get(task.getId());
                appendEvent(db, next, "recovered", payload(
                        "previous_state", task.getState().storageValue(),
                        "checkpoint", task.getCheckpointJson()
                ));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        database.backupAsync();
        return tasks.size();
    }

    private AgentTaskRecord queryTask(String selection, String[] args, String orderBy) {
        Cursor cursor = database.getReadableDatabase().query(
                TASKS,
                null,
                selection,
                args,
                null,
                null,
                orderBy,
                "1"
        );
        try {
            return cursor.moveToFirst() ? readTask(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    private List<AgentTaskRecord> queryTasks(String selection, String[] args, String orderBy) {
        ArrayList<AgentTaskRecord> result = new ArrayList<>();
        Cursor cursor = database.getReadableDatabase().query(
                TASKS,
                null,
                selection,
                args,
                null,
                null,
                orderBy
        );
        try {
            while (cursor.moveToNext()) {
                result.add(readTask(cursor));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    private AgentTaskRecord readTask(Cursor cursor) {
        return new AgentTaskRecord(
                value(cursor, "id"),
                value(cursor, "conversation_id"),
                value(cursor, "project_id"),
                value(cursor, "workspace_path"),
                value(cursor, "user_message_id"),
                value(cursor, "prompt"),
                value(cursor, "raw_input_json"),
                value(cursor, "model_config_id"),
                AgentTaskState.fromStorage(value(cursor, "state")),
                intValue(cursor, "generation_id"),
                intValue(cursor, "tool_call_count"),
                intValue(cursor, "max_tool_calls"),
                value(cursor, "checkpoint_json"),
                value(cursor, "last_error"),
                intValue(cursor, "resume_count"),
                longValue(cursor, "created_at"),
                longValue(cursor, "updated_at"),
                longValue(cursor, "started_at"),
                longValue(cursor, "finished_at"),
                longValue(cursor, "deadline_at")
        );
    }

    private AgentTaskEventRecord readEvent(Cursor cursor) {
        return new AgentTaskEventRecord(
                longValue(cursor, "id"),
                value(cursor, "task_id"),
                longValue(cursor, "sequence"),
                value(cursor, "event_type"),
                AgentTaskState.fromStorage(value(cursor, "state")),
                value(cursor, "payload_json"),
                longValue(cursor, "created_at")
        );
    }

    private void insertTask(SQLiteDatabase db, AgentTaskRecord task) {
        ContentValues values = new ContentValues();
        values.put("id", task.getId());
        values.put("conversation_id", task.getConversationId());
        putNullable(values, "project_id", task.getProjectId());
        values.put("workspace_path", task.getWorkspacePath());
        values.put("user_message_id", task.getUserMessageId());
        values.put("prompt", task.getPrompt());
        values.put("raw_input_json", task.getRawInputJson());
        values.put("model_config_id", task.getModelConfigId());
        values.put("state", task.getState().storageValue());
        values.put("generation_id", task.getGenerationId());
        values.put("tool_call_count", task.getToolCallCount());
        values.put("max_tool_calls", task.getMaxToolCalls());
        values.put("checkpoint_json", task.getCheckpointJson());
        values.put("last_error", task.getLastError());
        values.put("resume_count", task.getResumeCount());
        values.put("created_at", task.getCreatedAt());
        values.put("updated_at", task.getUpdatedAt());
        putNullableLong(values, "started_at", task.getStartedAt());
        putNullableLong(values, "finished_at", task.getFinishedAt());
        putNullableLong(values, "deadline_at", task.getDeadlineAt());
        db.insertWithOnConflict(TASKS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void appendEvent(SQLiteDatabase db, AgentTaskRecord task, String eventType, String payloadJson) {
        if (task == null) {
            return;
        }
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(MAX(sequence), 0) + 1 FROM " + EVENTS + " WHERE task_id = ?",
                new String[] {task.getId()}
        );
        long sequence;
        try {
            sequence = cursor.moveToFirst() ? cursor.getLong(0) : 1L;
        } finally {
            cursor.close();
        }
        ContentValues values = new ContentValues();
        values.put("task_id", task.getId());
        values.put("sequence", sequence);
        values.put("event_type", eventType == null ? "" : eventType);
        values.put("state", task.getState().storageValue());
        values.put("payload_json", payloadJson == null ? "" : payloadJson);
        values.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict(EVENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private String eventType(AgentTaskState state) {
        if (state == AgentTaskState.SUCCEEDED) return "succeeded";
        if (state == AgentTaskState.CANCELLED) return "cancelled";
        if (state == AgentTaskState.PAUSED) return "paused";
        return "failed";
    }

    private String normalizePhase(String phase) {
        String value = phase == null ? "unknown" : phase.trim().toLowerCase(java.util.Locale.ROOT);
        return value.length() == 0 ? "unknown" : value.replaceAll("[^a-z0-9_-]", "_");
    }

    private String payload(String key, Object value) {
        try {
            return new org.json.JSONObject().put(key, value == null ? "" : value).toString();
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String payload(String key1, Object value1, String key2, Object value2) {
        try {
            return new org.json.JSONObject()
                    .put(key1, value1 == null ? "" : value1)
                    .put(key2, value2 == null ? "" : value2)
                    .toString();
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String payload(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        try {
            return new org.json.JSONObject()
                    .put(key1, value1 == null ? "" : value1)
                    .put(key2, value2 == null ? "" : value2)
                    .put(key3, value3 == null ? "" : value3)
                    .toString();
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private void putNullable(ContentValues values, String key, String value) {
        if (value == null || value.length() == 0) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    private void putNullableLong(ContentValues values, String key, long value) {
        if (value <= 0) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }
}
