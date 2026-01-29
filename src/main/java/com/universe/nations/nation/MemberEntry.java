package com.universe.nations.nation;

import java.util.UUID;

public record MemberEntry(UUID nationId, UUID memberUuid, NationRole role) {}
