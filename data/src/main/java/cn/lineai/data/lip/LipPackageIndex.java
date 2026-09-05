package cn.lineai.data.lip;

import cn.lineai.model.LipPackageRecord;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** File-backed list of installed LIP packages. Lives under {@code .linecode/lip/index.json}. */
public final class LipPackageIndex {
    private final File indexFile;

    public LipPackageIndex(File linecodeRoot) {
        File root = linecodeRoot == null ? new File(".linecode") : linecodeRoot;
        File dir = new File(root, "lip");
        this.indexFile = new File(dir, "index.json");
    }

    public synchronized List<LipPackageRecord> list() {
        if (!indexFile.isFile()) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = new JSONArray(readUtf8(indexFile));
            ArrayList<LipPackageRecord> records = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                records.add(fromJson(array.getJSONObject(i)));
            }
            return records;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    public synchronized LipPackageRecord find(String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        for (LipPackageRecord record : list()) {
            if (id.equals(record.getId())) {
                return record;
            }
        }
        return null;
    }

    public synchronized void upsert(LipPackageRecord record) {
        if (record == null || record.getId().length() == 0) {
            return;
        }
        ArrayList<LipPackageRecord> records = new ArrayList<>(list());
        for (int i = records.size() - 1; i >= 0; i--) {
            if (record.getId().equals(records.get(i).getId())) {
                records.remove(i);
            }
        }
        records.add(0, record);
        write(records);
    }

    public synchronized void remove(String id) {
        if (id == null || id.length() == 0) {
            return;
        }
        ArrayList<LipPackageRecord> records = new ArrayList<>();
        for (LipPackageRecord record : list()) {
            if (!id.equals(record.getId())) {
                records.add(record);
            }
        }
        write(records);
    }

    private void write(List<LipPackageRecord> records) {
        File parent = indexFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        JSONArray array = new JSONArray();
        for (LipPackageRecord record : records) {
            array.put(toJson(record));
        }
        try {
            byte[] bytes = array.toString(2).getBytes(StandardCharsets.UTF_8);
            FileOutputStream output = new FileOutputStream(indexFile, false);
            try {
                output.write(bytes);
            } finally {
                output.close();
            }
        } catch (Exception ignored) {
        }
    }

    private static JSONObject toJson(LipPackageRecord record) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", record.getId());
            json.put("name", record.getName());
            json.put("version", record.getVersion());
            json.put("description", record.getDescription());
            json.put("enabled", record.isEnabled());
            json.put("installedAt", record.getInstalledAt());
            json.put("skillIds", new JSONArray(record.getSkillIds()));
            json.put("agentIds", new JSONArray(record.getAgentIds()));
            json.put("mcpIds", new JSONArray(record.getMcpIds()));
        } catch (Exception ignored) {
        }
        return json;
    }

    private static LipPackageRecord fromJson(JSONObject json) {
        return new LipPackageRecord(
                json.optString("id"),
                json.optString("name"),
                json.optString("version"),
                json.optString("description"),
                json.optBoolean("enabled", true),
                stringList(json.optJSONArray("skillIds")),
                stringList(json.optJSONArray("agentIds")),
                stringList(json.optJSONArray("mcpIds")),
                json.optLong("installedAt", 0L)
        );
    }

    private static ArrayList<String> stringList(JSONArray array) {
        ArrayList<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "");
            if (value.length() > 0) {
                values.add(value);
            }
        }
        return values;
    }

    private static String readUtf8(File file) throws Exception {
        FileInputStream input = new FileInputStream(file);
        try {
            byte[] buffer = new byte[(int) Math.min(file.length(), 512 * 1024L)];
            int offset = 0;
            while (offset < buffer.length) {
                int read = input.read(buffer, offset, buffer.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            return new String(buffer, 0, offset, StandardCharsets.UTF_8);
        } finally {
            input.close();
        }
    }
}
