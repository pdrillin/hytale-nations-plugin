package com.universe.nations.nation;

public enum NationLevel {
    HAMLET(0),
    VILLAGE(1),
    CITY(2),
    METROPOLIS(3),
    DEPARTMENT(4),
    REGION(5),
    NATION(6);

    public final int id;

    NationLevel(int id) {
        this.id = id;
    }

    public static NationLevel fromId(int id) {
        for (var v : values()) if (v.id == id) return v;
        return HAMLET;
    }
}

