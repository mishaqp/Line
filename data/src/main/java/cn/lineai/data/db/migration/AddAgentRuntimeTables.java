package cn.lineai.data.db.migration;

import android.database.sqlite.SQLiteDatabase;
import cn.lineai.data.db.LineCodeSchema;

public final class AddAgentRuntimeTables extends DatabaseMigration {
    @Override
    public int getTargetVersion() {
        return 5;
    }

    @Override
    public void apply(SQLiteDatabase db) {
        db.execSQL(LineCodeSchema.SQL_CREATE_AGENT_TASKS);
        db.execSQL(LineCodeSchema.SQL_CREATE_AGENT_TASK_EVENTS);
        db.execSQL(LineCodeSchema.SQL_CREATE_INDEX_AGENT_TASKS);
        db.execSQL(LineCodeSchema.SQL_CREATE_INDEX_AGENT_TASK_EVENTS);
    }
}
