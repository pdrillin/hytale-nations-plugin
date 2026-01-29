package com.universe.nations.nation;

public enum NationRole {
    OWNER,
    OFFICER,
    MEMBER;

    public boolean canInvite() { return this == OWNER || this == OFFICER; }
    public boolean canKick(NationRole target) { return this == OWNER || (this == OFFICER && target == MEMBER); }
    public boolean canPromote() { return this == OWNER; }
    public boolean canEditNation() { return this == OWNER; }
}
