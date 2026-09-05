package cn.lineai.data.lip;

import cn.lineai.model.LipManifest;
import cn.lineai.model.SkillRecord;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Reads {@code manifest.json} or infers a LIP layout from an extracted directory.
 */
public final class LipManifestParser {

    private LipManifestParser() {
    }

    public static LipManifest parseExtracted(File root) throws Exception {
        return parseExtracted(root, root == null ? "" : root.getName());
    }

    public static LipManifest parseExtracted(File root, String fallbackName) throws Exception {
        if (root == null || !root.isDirectory()) {
            throw new IllegalArgumentException("LIP root is not a directory.");
        }
        File manifestFile = new File(root, "manifest.json");
        if (manifestFile.isFile()) {
            return parseJson(readUtf8(manifestFile), fallbackName);
        }
        return infer(root, fallbackName);
    }

    public static LipManifest parseJson(String raw, String fallbackId) throws Exception {
        JSONObject json = new JSONObject(raw == null || raw.trim().length() == 0 ? "{}" : raw);
        String id = firstNonEmpty(json.optString("id"), sanitizeId(fallbackId));
        String name = firstNonEmpty(json.optString("name"), id);
        String version = firstNonEmpty(json.optString("version"), "1.0");
        String description = json.optString("description", "");
        ArrayList<LipManifest.SkillEntry> skills = new ArrayList<>();
        JSONArray skillArray = json.optJSONArray("skills");
        if (skillArray != null) {
            for (int i = 0; i < skillArray.length(); i++) {
                Object item = skillArray.get(i);
                if (item instanceof JSONObject) {
                    JSONObject row = (JSONObject) item;
                    skills.add(new LipManifest.SkillEntry(
                            normalizePath(row.optString("path")),
                            row.optString("name", ""),
                            normalizeLocation(row.optString("location"))
                    ));
                } else if (item != null) {
                    skills.add(new LipManifest.SkillEntry(
                            normalizePath(String.valueOf(item)), "", ""));
                }
            }
        }
        return new LipManifest(
                id,
                name,
                version,
                description,
                skills,
                stringList(json.optJSONArray("agents")),
                stringList(json.optJSONArray("mcps"))
        );
    }

    static LipManifest infer(File root, String fallbackName) {
        ArrayList<LipManifest.SkillEntry> skills = new ArrayList<>();
        ArrayList<String> agents = new ArrayList<>();
        ArrayList<String> mcps = new ArrayList<>();
        File skillMd = new File(root, "SKILL.md");
        if (skillMd.isFile()) {
            skills.add(new LipManifest.SkillEntry(".", sanitizeId(fallbackName), ""));
        }
        collectSkills(new File(root, "skills"), "skills", skills);
        collectJsonFiles(new File(root, "agents"), "agents", agents);
        collectJsonFiles(new File(root, "mcps"), "mcps", mcps);
        if (skills.isEmpty() && agents.isEmpty() && mcps.isEmpty()) {
            collectSkills(root, "", skills);
        }
        String id = sanitizeId(fallbackName);
        return new LipManifest(id, id, "1.0", "", skills, agents, mcps);
    }

    private static void collectSkills(File dir, String prefix, ArrayList<LipManifest.SkillEntry> out) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        if (new File(dir, "SKILL.md").isFile() && prefix.length() > 0) {
            out.add(new LipManifest.SkillEntry(prefix, dir.getName(), ""));
            return;
        }
        for (File child : children) {
            if (!child.isDirectory()) {
                continue;
            }
            String next = prefix.length() == 0 ? child.getName() : prefix + "/" + child.getName();
            if (new File(child, "SKILL.md").isFile()) {
                out.add(new LipManifest.SkillEntry(next, child.getName(), ""));
            }
        }
    }

    private static void collectJsonFiles(File dir, String prefix, ArrayList<String> out) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && child.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
                out.add(prefix + "/" + child.getName());
            }
        }
    }

    private static ArrayList<String> stringList(JSONArray array) {
        ArrayList<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            Object item = array.opt(i);
            if (item instanceof JSONObject) {
                String path = normalizePath(((JSONObject) item).optString("path"));
                if (path.length() > 0) {
                    values.add(path);
                }
            } else if (item != null) {
                String path = normalizePath(String.valueOf(item));
                if (path.length() > 0) {
                    values.add(path);
                }
            }
        }
        return values;
    }

    public static String sanitizeId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith(".lip") || value.endsWith(".zip")) {
            int dot = value.lastIndexOf('.');
            value = value.substring(0, dot);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                builder.append(c);
            } else if (c == '.' || c == ' ') {
                builder.append('-');
            }
        }
        String id = trimDashes(builder.toString());
        return id.length() == 0 ? "lip-package" : id;
    }

    private static String trimDashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '-') {
            end--;
        }
        return value.substring(start, end);
    }

    private static String normalizePath(String path) {
        String value = path == null ? "" : path.trim().replace('\\', '/');
        while (value.startsWith("./")) {
            value = value.substring(2);
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.contains("..")) {
            throw new IllegalArgumentException("LIP path escapes package root: " + path);
        }
        return value;
    }

    private static String normalizeLocation(String location) {
        String value = location == null ? "" : location.trim().toLowerCase(Locale.ROOT);
        if (SkillRecord.LOCATION_PROJECT.equals(value) || SkillRecord.LOCATION_APP.equals(value)) {
            return value;
        }
        return "";
    }

    private static String firstNonEmpty(String left, String right) {
        if (left != null && left.trim().length() > 0) {
            return left.trim();
        }
        return right == null ? "" : right.trim();
    }

    private static String readUtf8(File file) throws Exception {
        FileInputStream input = new FileInputStream(file);
        try {
            byte[] buffer = new byte[(int) Math.min(file.length(), 256 * 1024L)];
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
