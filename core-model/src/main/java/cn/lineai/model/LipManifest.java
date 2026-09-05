package cn.lineai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed LIP package before install. Paths are relative to the extracted root. */
public final class LipManifest {
    public static final int SCHEMA = 1;

    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final List<SkillEntry> skills;
    private final List<String> agentPaths;
    private final List<String> mcpPaths;

    public LipManifest(
            String id,
            String name,
            String version,
            String description,
            List<SkillEntry> skills,
            List<String> agentPaths,
            List<String> mcpPaths
    ) {
        this.id = Strings.nullToEmpty(id);
        this.name = Strings.nullToEmpty(name);
        this.version = Strings.nullToEmpty(version);
        this.description = Strings.nullToEmpty(description);
        this.skills = skills == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(skills));
        this.agentPaths = copy(agentPaths);
        this.mcpPaths = copy(mcpPaths);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public List<SkillEntry> getSkills() {
        return skills;
    }

    public List<String> getAgentPaths() {
        return agentPaths;
    }

    public List<String> getMcpPaths() {
        return mcpPaths;
    }

    public boolean isEmpty() {
        return skills.isEmpty() && agentPaths.isEmpty() && mcpPaths.isEmpty();
    }

    public static final class SkillEntry {
        private final String path;
        private final String name;
        private final String location;

        public SkillEntry(String path, String name, String location) {
            this.path = Strings.nullToEmpty(path);
            this.name = Strings.nullToEmpty(name);
            this.location = Strings.nullToEmpty(location);
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public String getLocation() {
            return location;
        }
    }

    private static List<String> copy(List<String> source) {
        ArrayList<String> values = new ArrayList<>();
        if (source != null) {
            for (String item : source) {
                if (item != null && item.trim().length() > 0) {
                    values.add(item.trim().replace('\\', '/'));
                }
            }
        }
        return Collections.unmodifiableList(values);
    }
}
