package cn.lineai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Installed LIP package: a zip bundle that unpacked into skills / agents / MCPs. */
public final class LipPackageRecord {
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final boolean enabled;
    private final List<String> skillIds;
    private final List<String> agentIds;
    private final List<String> mcpIds;
    private final long installedAt;

    public LipPackageRecord(
            String id,
            String name,
            String version,
            String description,
            boolean enabled,
            List<String> skillIds,
            List<String> agentIds,
            List<String> mcpIds,
            long installedAt
    ) {
        this.id = Strings.nullToEmpty(id);
        this.name = Strings.nullToEmpty(name);
        this.version = Strings.nullToEmpty(version);
        this.description = Strings.nullToEmpty(description);
        this.enabled = enabled;
        this.skillIds = copy(skillIds);
        this.agentIds = copy(agentIds);
        this.mcpIds = copy(mcpIds);
        this.installedAt = installedAt;
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

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getSkillIds() {
        return skillIds;
    }

    public List<String> getAgentIds() {
        return agentIds;
    }

    public List<String> getMcpIds() {
        return mcpIds;
    }

    public long getInstalledAt() {
        return installedAt;
    }

    public int componentCount() {
        return skillIds.size() + agentIds.size() + mcpIds.size();
    }

    public LipPackageRecord withEnabled(boolean value) {
        return new LipPackageRecord(
                id, name, version, description, value, skillIds, agentIds, mcpIds, installedAt);
    }

    private static List<String> copy(List<String> source) {
        ArrayList<String> values = new ArrayList<>();
        if (source != null) {
            for (String item : source) {
                if (item != null && item.trim().length() > 0) {
                    values.add(item.trim());
                }
            }
        }
        return Collections.unmodifiableList(values);
    }
}
