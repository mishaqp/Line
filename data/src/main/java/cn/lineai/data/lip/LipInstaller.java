package cn.lineai.data.lip;

import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.model.ExtensionAgentConfig;
import cn.lineai.model.ExtensionMcpConfig;
import cn.lineai.model.LipManifest;
import cn.lineai.model.LipPackageRecord;
import cn.lineai.model.McpRequestHeader;
import cn.lineai.model.McpToolSummary;
import cn.lineai.model.SkillRecord;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public final class LipInstaller {
    private final ExtensionStore extensionStore;
    private final LipPackageIndex index;
    private final File tempRoot;

    public LipInstaller(ExtensionStore extensionStore, LipPackageIndex index, File tempRoot) {
        this.extensionStore = extensionStore;
        this.index = index;
        this.tempRoot = tempRoot;
    }

    public List<LipPackageRecord> list() {
        return index.list();
    }

    public LipPackageRecord installFile(String homePath, String location, File source) throws Exception {
        if (source == null || !source.exists()) {
            throw new IllegalArgumentException("LIP file not found.");
        }
        File extracted = extract(source);
        try {
            return installExtracted(homePath, location, extracted, source.getName());
        } finally {
            deleteRecursive(extracted);
        }
    }

    public LipPackageRecord installExtracted(
            String homePath,
            String location,
            File extracted,
            String fallbackName
    ) throws Exception {
        LipManifest manifest = LipManifestParser.parseExtracted(extracted);
        if (manifest.isEmpty()) {
            throw new IllegalArgumentException("LIP package has no skills, agents or MCP configs.");
        }
        String packageId = manifest.getId().length() == 0
                ? LipManifestParser.sanitizeId(fallbackName)
                : manifest.getId();
        LipPackageRecord existing = index.find(packageId);
        if (existing != null) {
            deletePackage(existing, false);
        }
        String defaultLocation = location == null || location.length() == 0
                ? SkillRecord.LOCATION_APP
                : location;
        ArrayList<String> skillIds = new ArrayList<>();
        for (LipManifest.SkillEntry entry : manifest.getSkills()) {
            File skillSource = resolve(extracted, entry.getPath());
            if (!skillSource.exists()) {
                throw new IllegalArgumentException("Skill missing in LIP: " + entry.getPath());
            }
            String skillLocation = entry.getLocation().length() == 0 ? defaultLocation : entry.getLocation();
            SkillRecord installed = extensionStore.installSkill(
                    homePath, skillLocation, skillSource.getAbsolutePath(), entry.getName());
            if (installed != null && installed.getId().length() > 0) {
                skillIds.add(installed.getId());
            }
        }
        ArrayList<String> agentIds = new ArrayList<>();
        for (String path : manifest.getAgentPaths()) {
            ExtensionAgentConfig saved = extensionStore.saveAgentExtension(
                    readAgent(resolve(extracted, path)));
            if (saved != null && saved.getId().length() > 0) {
                agentIds.add(saved.getId());
            }
        }
        ArrayList<String> mcpIds = new ArrayList<>();
        for (String path : manifest.getMcpPaths()) {
            ExtensionMcpConfig saved = extensionStore.saveMcpExtension(
                    readMcp(resolve(extracted, path)));
            if (saved != null && saved.getId().length() > 0) {
                mcpIds.add(saved.getId());
            }
        }
        LipPackageRecord record = new LipPackageRecord(
                packageId,
                manifest.getName().length() == 0 ? packageId : manifest.getName(),
                manifest.getVersion(),
                manifest.getDescription(),
                true,
                skillIds,
                agentIds,
                mcpIds,
                System.currentTimeMillis()
        );
        index.upsert(record);
        return record;
    }

    public void setEnabled(String id, boolean enabled) {
        LipPackageRecord record = index.find(id);
        if (record == null) {
            return;
        }
        for (String skillId : record.getSkillIds()) {
            extensionStore.setSkillEnabled(skillId, enabled);
        }
        for (String agentId : record.getAgentIds()) {
            extensionStore.setAgentEnabled(agentId, enabled);
        }
        for (String mcpId : record.getMcpIds()) {
            extensionStore.setMcpEnabled(mcpId, enabled);
        }
        index.upsert(record.withEnabled(enabled));
    }

    public void delete(String id) {
        LipPackageRecord record = index.find(id);
        if (record == null) {
            return;
        }
        deletePackage(record, true);
    }

    private void deletePackage(LipPackageRecord record, boolean removeIndex) {
        for (String skillId : record.getSkillIds()) {
            extensionStore.deleteSkill(skillId);
        }
        for (String agentId : record.getAgentIds()) {
            extensionStore.deleteAgent(agentId);
        }
        for (String mcpId : record.getMcpIds()) {
            extensionStore.deleteMcp(mcpId);
        }
        if (removeIndex) {
            index.remove(record.getId());
        }
    }

    private File extract(File source) throws Exception {
        if (source.isDirectory()) {
            return source;
        }
        String lower = source.getName().toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".zip") && !lower.endsWith(".lip") && !"skill.md".equals(lower)) {
            throw new IllegalArgumentException("LIP must be a .lip / .zip package or a directory.");
        }
        File target = uniqueChild(tempRoot == null ? source.getParentFile() : tempRoot,
                LipManifestParser.sanitizeId(source.getName()) + "-extract");
        if ("skill.md".equals(lower)) {
            target.mkdirs();
            copyFile(source, new File(target, "SKILL.md"));
            return target;
        }
        unzip(source, target);
        File nested = unwrapSingleDirectory(target);
        return nested;
    }

    private static File unwrapSingleDirectory(File target) {
        File manifest = new File(target, "manifest.json");
        if (manifest.isFile() || new File(target, "SKILL.md").isFile()
                || new File(target, "skills").isDirectory()) {
            return target;
        }
        File[] children = target.listFiles();
        if (children != null && children.length == 1 && children[0].isDirectory()) {
            return children[0];
        }
        return target;
    }

    public static void unzip(File source, File target) throws Exception {
        target.mkdirs();
        File canonicalTarget = target.getCanonicalFile();
        ZipInputStream input = new ZipInputStream(new BufferedInputStream(new FileInputStream(source)));
        try {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                File out = new File(target, entry.getName()).getCanonicalFile();
                if (!out.getPath().equals(canonicalTarget.getPath())
                        && !out.getPath().startsWith(canonicalTarget.getPath() + File.separator)) {
                    throw new IllegalArgumentException("ZIP entry escapes package: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(out, false));
                    try {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                        }
                    } finally {
                        output.close();
                    }
                }
                input.closeEntry();
            }
        } finally {
            input.close();
        }
    }

    private static File resolve(File root, String relative) throws Exception {
        File canonicalRoot = root.getCanonicalFile();
        File out = new File(root, relative == null ? "" : relative).getCanonicalFile();
        if (!out.getPath().equals(canonicalRoot.getPath())
                && !out.getPath().startsWith(canonicalRoot.getPath() + File.separator)) {
            throw new IllegalArgumentException("LIP path escapes package: " + relative);
        }
        return out;
    }

    private static ExtensionAgentConfig readAgent(File file) throws Exception {
        JSONObject json = new JSONObject(readUtf8(file));
        long now = System.currentTimeMillis();
        return new ExtensionAgentConfig(
                json.optString("id", ""),
                json.optBoolean("enabled", true),
                json.optString("name", file.getName()),
                json.optString("slug", ""),
                json.optString("prompt", ""),
                json.optString("trigger", ""),
                stringList(json.optJSONArray("toolNames")),
                stringList(json.optJSONArray("mcpIds")),
                now,
                now
        );
    }

    private static ExtensionMcpConfig readMcp(File file) throws Exception {
        JSONObject json = new JSONObject(readUtf8(file));
        ArrayList<McpRequestHeader> headers = new ArrayList<>();
        JSONArray headerArray = json.optJSONArray("headers");
        if (headerArray != null) {
            for (int i = 0; i < headerArray.length(); i++) {
                JSONObject row = headerArray.optJSONObject(i);
                if (row != null) {
                    headers.add(new McpRequestHeader(row.optString("name"), row.optString("value")));
                }
            }
        }
        ArrayList<McpToolSummary> tools = new ArrayList<>();
        JSONArray toolArray = json.optJSONArray("tools");
        if (toolArray != null) {
            for (int i = 0; i < toolArray.length(); i++) {
                JSONObject row = toolArray.optJSONObject(i);
                if (row != null) {
                    tools.add(new McpToolSummary(
                            row.optString("name"),
                            row.optBoolean("enabled", true),
                            row.optString("description"),
                            row.optString("inputSchema", "")
                    ));
                }
            }
        }
        long now = System.currentTimeMillis();
        return new ExtensionMcpConfig(
                json.optString("id", ""),
                json.optBoolean("enabled", true),
                json.optString("name", file.getName()),
                json.optString("url", ""),
                headers,
                tools,
                now,
                now
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

    private static File uniqueChild(File parent, String name) {
        File base = parent == null ? new File(".") : parent;
        base.mkdirs();
        File candidate = new File(base, name);
        int index = 2;
        while (candidate.exists()) {
            candidate = new File(base, name + "-" + index);
            index++;
        }
        return candidate;
    }

    private static void copyFile(File source, File target) throws Exception {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileInputStream input = new FileInputStream(source);
        try {
            FileOutputStream output = new FileOutputStream(target, false);
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    private static String readUtf8(File file) throws Exception {
        FileInputStream input = new FileInputStream(file);
        try {
            byte[] buffer = new byte[(int) Math.min(Math.max(file.length(), 1L), 256 * 1024L)];
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
