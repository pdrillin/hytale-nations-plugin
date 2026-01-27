package com.universe.nations.nation;


import java.util.UUID;

public class NationInfo {
    private final UUID id;
    private final String name;
    private final String nameLower;
    private final UUID ownerUuid;
    private int level;
    private final long createdAt;

    public NationInfo(UUID id, String name, UUID ownerUuid, int level, long createdAt) {
        this.id = id;
        this.name = name;
        this.nameLower = name.toLowerCase();
        this.ownerUuid = ownerUuid;
        this.level = level;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getNameLower() { return nameLower; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public long getCreatedAt() { return createdAt; }
}
