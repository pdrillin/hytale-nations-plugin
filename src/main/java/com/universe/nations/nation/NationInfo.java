package com.universe.nations.nation;


import java.util.UUID;

public class NationInfo {
    private final UUID id;
    private String name;
    private String nameLower;
    private final UUID ownerUuid;
    private int level;
    private String description;
    private final long createdAt;
    private long updatedAt;

    public NationInfo(UUID id, String name, UUID ownerUuid, int level, String description, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.nameLower = name.toLowerCase();
        this.ownerUuid = ownerUuid;
        this.level = level;
        this.description = description == null ? "" : description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public NationInfo(UUID id, String name, UUID ownerUuid, int level, long createdAt) {
        this(id, name, ownerUuid, level, "", createdAt, createdAt);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getNameLower() { return nameLower; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public void setName(String name) {
        this.name = name;
        this.nameLower = name.toLowerCase();
    }

    private void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
}
