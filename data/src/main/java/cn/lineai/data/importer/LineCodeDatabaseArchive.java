package cn.lineai.data.importer;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;
import cn.lineai.data.db.LineCodeDatabase;
import cn.lineai.data.db.LineCodeSchema;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

public final class LineCodeDatabaseArchive {
    private static final String TAG = "LineCodeDatabaseArchive";
    private static final int MESSAGE_TEXT_CHUNK_SIZE = 64 * 1024;
    private static final String[] MESSAGE_EXPORT_COLUMNS = new String[] {
            "id",
            "conversation_id",
            "local_order",
            "role",
            "content",
            "reasoning_content",
            "timestamp",
            "streaming",
            "hidden",
            "exclude_from_context",
            "tool_call_id",
            "tool_name",
            "is_error",
            "raw_json"
    };
    private static final String[] MESSAGE_META_EXPORT_COLUMNS = new String[] {
            "id",
            "conversation_id",
            "local_order",
            "role",
            "timestamp",
            "streaming",
            "hidden",
            "exclude_from_context",
            "tool_call_id",
            "tool_name",
            "is_error"
    };
    private static final String[] MESSAGE_CHUNK_EXPORT_COLUMNS = new String[] {
            "id",
            "message_id",
            "field_name",
            "chunk_order",
            "content"
    };
    private static final String[] TABLES = new String[] {
            LineCodeSchema.TABLE_SETTINGS,
            LineCodeSchema.TABLE_PROJECTS,
            LineCodeSchema.TABLE_MODELS,
            LineCodeSchema.TABLE_CONVERSATIONS,
            LineCodeSchema.TABLE_MESSAGES,
            LineCodeSchema.TABLE_MESSAGE_TEXT_CHUNKS,
            LineCodeSchema.TABLE_MESSAGE_BLOCKS,
            LineCodeSchema.TABLE_TOOL_CALLS,
            LineCodeSchema.TABLE_TOOL_RESULTS,
            LineCodeSchema.TABLE_ATTACHMENTS,
            LineCodeSchema.TABLE_DIFFS,
            LineCodeSchema.TABLE_MEMORIES,
            LineCodeSchema.TABLE_WORKING_MEMORY,
            LineCodeSchema.TABLE_CONVERSATION_INDEX,
            LineCodeSchema.TABLE_SKILLS,
            LineCodeSchema.TABLE_SKILL_USAGE,
            LineCodeSchema.TABLE_EXTENSION_AGENTS,
            LineCodeSchema.TABLE_EXTENSION_MCPS,
            LineCodeSchema.TABLE_AGENT_TASKS,
            LineCodeSchema.TABLE_AGENT_TASK_EVENTS
    };

    private final LineCodeArchiveCodec codec = new LineCodeArchiveCodec();

    public JSONObject exportSnapshot(LineCodeDatabase database) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", "linecode-database");
        root.put("schemaVersion", LineCodeSchema.VERSION);
        JSONObject tables = new JSONObject();
        SQLiteDatabase db = database.getReadableDatabase();
        for (String table : TABLES) {
            if (LineCodeSchema.TABLE_MESSAGES.equals(table)) {
                tables.put(table, exportMessagesTable(db));
            } else if (LineCodeSchema.TABLE_MESSAGE_TEXT_CHUNKS.equals(table)) {
                tables.put(table, exportMessageTextChunksTable(db));
            } else {
                tables.put(table, exportTable(db, table));
            }
        }
        root.put("tables", tables);
        return root;
    }

    /**
     * 完整导出（保留 messages 正文），供本地自动备份使用。
     * 与 {@link #exportSnapshot} 的区别：后者出于隐私考虑将消息正文置空，
     * 而本方法导出 messages / message_text_chunks 的全部内容，确保备份可完整恢复聊天记录。
     */
    public JSONObject exportFullSnapshot(LineCodeDatabase database) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", "linecode-database");
        root.put("schemaVersion", LineCodeSchema.VERSION);
        root.put("full", true);
        JSONObject tables = new JSONObject();
        SQLiteDatabase db = database.getReadableDatabase();
        for (String table : TABLES) {
            tables.put(table, exportFullTable(db, table));
        }
        root.put("tables", tables);
        return root;
    }

    private JSONObject exportFullTable(SQLiteDatabase db, String table) throws Exception {
        if (LineCodeSchema.TABLE_MESSAGE_TEXT_CHUNKS.equals(table)) {
            return exportMessageTextChunksTable(db);
        }
        return exportTable(db, table);
    }

    public boolean hasSnapshot(File payloadDir) {
        return new File(payloadDir, LineCodeArchiveCodec.ENTRY_DATABASE).isFile();
    }

    public JSONObject readSnapshot(File payloadDir) throws Exception {
        return new JSONObject(codec.readUtf8(new File(payloadDir, LineCodeArchiveCodec.ENTRY_DATABASE)));
    }

    public void importSnapshot(LineCodeDatabase database, JSONObject snapshot) throws Exception {
        if (snapshot == null || !"linecode-database".equals(snapshot.optString("format"))) {
            throw new IllegalArgumentException(".linecode 数据库快照无效。");
        }
        int snapshotVersion = snapshot.optInt("schemaVersion", 0);
        if (snapshotVersion > LineCodeSchema.VERSION) {
            throw new IllegalStateException(
                    ".linecode 归档由更高版本生成（schemaVersion=" + snapshotVersion
                            + "），请先升级 App。");
        }
        if (snapshotVersion > 0 && snapshotVersion < LineCodeSchema.VERSION) {
            Log.w(TAG, "Importing older schema snapshot: " + snapshotVersion
                    + " -> " + LineCodeSchema.VERSION);
        }
        JSONObject tables = snapshot.optJSONObject("tables");
        if (tables == null) {
            throw new IllegalArgumentException(".linecode 数据库快照缺少 tables。");
        }
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = TABLES.length - 1; i >= 0; i--) {
                db.delete(TABLES[i], null, null);
            }
            for (String table : TABLES) {
                importTable(db, table, tables.optJSONObject(table));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private JSONObject exportTable(SQLiteDatabase db, String table) throws Exception {
        JSONObject object = new JSONObject();
        JSONArray columns = new JSONArray();
        JSONArray rows = new JSONArray();
        Cursor cursor = db.query(table, null, null, null, null, null, null);
        try {
            String[] names = cursor.getColumnNames();
            for (String name : names) {
                columns.put(name);
            }
            while (cursor.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < names.length; i++) {
                    row.put(names[i], encodeValue(cursor, i));
                }
                rows.put(row);
            }
        } finally {
            cursor.close();
        }
        object.put("columns", columns);
        object.put("rows", rows);
        return object;
    }

    private JSONObject exportMessagesTable(SQLiteDatabase db) throws Exception {
        JSONObject object = new JSONObject();
        JSONArray columns = columnsJson(MESSAGE_EXPORT_COLUMNS);
        JSONArray rows = new JSONArray();
        Cursor cursor = db.query(
                LineCodeSchema.TABLE_MESSAGES,
                MESSAGE_META_EXPORT_COLUMNS,
                null,
                null,
                null,
                null,
                "conversation_id ASC, local_order ASC"
        );
        try {
            while (cursor.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < MESSAGE_EXPORT_COLUMNS.length; i++) {
                    String column = MESSAGE_EXPORT_COLUMNS[i];
                    if ("content".equals(column) || "reasoning_content".equals(column) || "raw_json".equals(column)) {
                        row.put(column, stringCell(""));
                    } else {
                        row.put(column, encodeValue(cursor, cursor.getColumnIndexOrThrow(column)));
                    }
                }
                rows.put(row);
            }
        } finally {
            cursor.close();
        }
        object.put("columns", columns);
        object.put("rows", rows);
        return object;
    }

    private JSONObject exportMessageTextChunksTable(SQLiteDatabase db) throws Exception {
        JSONObject object = new JSONObject();
        JSONArray columns = columnsJson(MESSAGE_CHUNK_EXPORT_COLUMNS);
        JSONArray rows = new JSONArray();
        HashSet<String> chunkedFields = new HashSet<>();
        Cursor cursor = db.query(
                LineCodeSchema.TABLE_MESSAGE_TEXT_CHUNKS,
                MESSAGE_CHUNK_EXPORT_COLUMNS,
                null,
                null,
                null,
                null,
                "message_id ASC, field_name ASC, chunk_order ASC"
        );
        try {
            while (cursor.moveToNext()) {
                String messageId = cursor.getString(cursor.getColumnIndexOrThrow("message_id"));
                String fieldName = cursor.getString(cursor.getColumnIndexOrThrow("field_name"));
                chunkedFields.add(chunkKey(messageId, fieldName));
                JSONObject row = new JSONObject();
                for (int i = 0; i < MESSAGE_CHUNK_EXPORT_COLUMNS.length; i++) {
                    row.put(MESSAGE_CHUNK_EXPORT_COLUMNS[i], encodeValue(cursor, i));
                }
                rows.put(row);
            }
        } finally {
            cursor.close();
        }
        appendLegacyMessageTextChunks(db, rows, chunkedFields);
        object.put("columns", columns);
        object.put("rows", rows);
        return object;
    }

    private void appendLegacyMessageTextChunks(SQLiteDatabase db, JSONArray rows, HashSet<String> chunkedFields) throws Exception {
        Cursor cursor = db.query(
                LineCodeSchema.TABLE_MESSAGES,
                new String[] {"id"},
                null,
                null,
                null,
                null,
                "conversation_id ASC, local_order ASC"
        );
        try {
            while (cursor.moveToNext()) {
                String messageId = cursor.getString(0);
                appendLegacyMessageTextField(db, rows, chunkedFields, messageId, "content");
                appendLegacyMessageTextField(db, rows, chunkedFields, messageId, "reasoning_content");
                appendLegacyMessageTextField(db, rows, chunkedFields, messageId, "raw_json");
            }
        } finally {
            cursor.close();
        }
    }

    private void appendLegacyMessageTextField(
            SQLiteDatabase db,
            JSONArray rows,
            HashSet<String> chunkedFields,
            String messageId,
            String fieldName
    ) throws Exception {
        if (chunkedFields.contains(chunkKey(messageId, fieldName))) {
            return;
        }
        int length = legacyMessageTextLength(db, messageId, fieldName);
        if (length <= 0) {
            return;
        }
        for (int start = 0, order = 0; start < length; start += MESSAGE_TEXT_CHUNK_SIZE, order++) {
            JSONObject row = new JSONObject();
            row.put("id", nullCell());
            row.put("message_id", stringCell(messageId));
            row.put("field_name", stringCell(fieldName));
            row.put("chunk_order", integerCell(order));
            row.put("content", stringCell(readLegacyMessageTextRange(db, messageId, fieldName, start, MESSAGE_TEXT_CHUNK_SIZE)));
            rows.put(row);
        }
    }

    private int legacyMessageTextLength(SQLiteDatabase db, String messageId, String fieldName) {
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(length(" + safeMessageTextField(fieldName) + "), 0) FROM messages WHERE id = ? LIMIT 1",
                new String[] {messageId == null ? "" : messageId});
        try {
            return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private String readLegacyMessageTextRange(SQLiteDatabase db, String messageId, String fieldName, int start, int maxChars) {
        Cursor cursor = db.rawQuery(
                "SELECT substr(" + safeMessageTextField(fieldName) + ", ?, ?) FROM messages WHERE id = ? LIMIT 1",
                new String[] {String.valueOf(start + 1), String.valueOf(maxChars), messageId == null ? "" : messageId});
        try {
            return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getString(0) : "";
        } finally {
            cursor.close();
        }
    }

    private String safeMessageTextField(String fieldName) {
        if ("content".equals(fieldName) || "reasoning_content".equals(fieldName) || "raw_json".equals(fieldName)) {
            return fieldName;
        }
        throw new IllegalArgumentException("非法消息文本字段: " + fieldName);
    }

    private String chunkKey(String messageId, String fieldName) {
        return (messageId == null ? "" : messageId) + "\n" + (fieldName == null ? "" : fieldName);
    }

    private JSONArray columnsJson(String[] names) {
        JSONArray columns = new JSONArray();
        for (String name : names) {
            columns.put(name);
        }
        return columns;
    }

    private JSONObject encodeValue(Cursor cursor, int index) throws Exception {
        JSONObject object = new JSONObject();
        int type = cursor.getType(index);
        if (type == Cursor.FIELD_TYPE_NULL) {
            object.put("type", "null");
        } else if (type == Cursor.FIELD_TYPE_INTEGER) {
            object.put("type", "integer");
            object.put("value", cursor.getLong(index));
        } else if (type == Cursor.FIELD_TYPE_FLOAT) {
            object.put("type", "float");
            object.put("value", cursor.getDouble(index));
        } else if (type == Cursor.FIELD_TYPE_BLOB) {
            object.put("type", "blob");
            object.put("value", Base64.encodeToString(cursor.getBlob(index), Base64.NO_WRAP));
        } else {
            object.put("type", "string");
            object.put("value", cursor.getString(index));
        }
        return object;
    }

    private JSONObject nullCell() throws Exception {
        JSONObject object = new JSONObject();
        object.put("type", "null");
        return object;
    }

    private JSONObject integerCell(long value) throws Exception {
        JSONObject object = new JSONObject();
        object.put("type", "integer");
        object.put("value", value);
        return object;
    }

    private JSONObject stringCell(String value) throws Exception {
        JSONObject object = new JSONObject();
        object.put("type", "string");
        object.put("value", value == null ? "" : value);
        return object;
    }

    private void importTable(SQLiteDatabase db, String table, JSONObject tableJson) throws Exception {
        if (tableJson == null) {
            return;
        }
        JSONArray rows = tableJson.optJSONArray("rows");
        if (rows == null) {
            return;
        }
        HashSet<String> columns = tableColumns(db, table);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            ContentValues values = new ContentValues();
            Iterator<String> keys = row.keys();
            while (keys.hasNext()) {
                String column = keys.next();
                if (!columns.contains(column)) {
                    continue;
                }
                putValue(values, column, row.optJSONObject(column));
            }
            db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private void putValue(ContentValues values, String column, JSONObject cell) {
        if (cell == null || "null".equals(cell.optString("type"))) {
            values.putNull(column);
            return;
        }
        String type = cell.optString("type");
        if ("integer".equals(type)) {
            values.put(column, cell.optLong("value"));
        } else if ("float".equals(type)) {
            values.put(column, cell.optDouble("value"));
        } else if ("blob".equals(type)) {
            values.put(column, Base64.decode(cell.optString("value"), Base64.NO_WRAP));
        } else {
            values.put(column, cell.optString("value"));
        }
    }

    private HashSet<String> tableColumns(SQLiteDatabase db, String table) {
        if (!LineCodeSchema.isValidTable(table)) {
            throw new IllegalArgumentException("非法表名: " + table);
        }
        HashSet<String> columns = new HashSet<>();
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        try {
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
        } finally {
            cursor.close();
        }
        return columns;
    }
}
