package com.universe.nations.nation;

import java.util.UUID;

public record NationInvite(UUID recipient, UUID nationId, UUID sender, long createdAt) {}

