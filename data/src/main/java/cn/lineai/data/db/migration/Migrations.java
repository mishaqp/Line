package cn.lineai.data.db.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Migrations {
    private static final List<DatabaseMigration> REGISTRY = new ArrayList<>();

    static {
        register(new AddToolCallObservabilityColumns());
        register(new AddIpcProvidersTable());
        register(new AddMessageTextChunksTable());
        register(new AddAgentRuntimeTables());
    }

    private Migrations() {
    }

    public static synchronized void register(DatabaseMigration migration) {
        if (migration == null) {
            return;
        }
        REGISTRY.add(migration);
    }

    public static synchronized List<DatabaseMigration> all() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY));
    }
}
