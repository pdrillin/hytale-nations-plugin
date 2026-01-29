package com.universe.nations.nation;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.universe.nations.files.DatabaseManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class NationManager {

    private final HytaleLogger logger;
    private final DatabaseManager db;

    // Cache RAM
    private final Map<UUID, NationInfo> nationsById = new ConcurrentHashMap<>();
    private final Map<String, UUID> nationIdByNameLower = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> memberToNation = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, NationRole>> membersByNationId = new ConcurrentHashMap<>();
    private final Map<UUID, NationInvite> invitesByRecipient = new ConcurrentHashMap<>();


    public NationManager(HytaleLogger logger, DatabaseManager db) {
        this.logger = logger;
        this.db = db;
        loadAll();
    }

    private void loadAll() {
        logger.at(Level.INFO).log("Loading Nations data from DB...");

        nationsById.clear();
        nationIdByNameLower.clear();
        memberToNation.clear();

        nationsById.putAll(db.loadNations());
        for (NationInfo n : nationsById.values()) {
            nationIdByNameLower.put(n.getNameLower(), n.getId());
        }

        memberToNation.putAll(db.loadMemberToNation());

        for (MemberEntry entry : db.loadNationMembers()) {
            memberToNation.put(entry.memberUuid(), entry.nationId());
            membersByNationId
                    .computeIfAbsent(entry.nationId(), k -> new ConcurrentHashMap<>())
                    .put(entry.memberUuid(), entry.role());
        }

        logger.at(Level.INFO).log("Loaded nations=" + nationsById.size() + " members=" + memberToNation.size());
    }

    public NationInfo getNationOf(UUID playerUuid) {
        UUID nationId = memberToNation.get(playerUuid);
        return nationId == null ? null : nationsById.get(nationId);
    }

    public boolean nationNameExists(String name) {
        return nationIdByNameLower.containsKey(name.toLowerCase());
    }

    public NationInfo createNation(UUID ownerUuid, String rawName, String rawDesc) {
        String name = validateAndNormalizeName(rawName);
        String lower = name.toLowerCase();
        String desc = normalizeDescription(rawDesc);

        if (isInNation(ownerUuid)) {
            throw new IllegalArgumentException("Tu es déjà dans une nation.");
        }
        if (nationIdByNameLower.containsKey(lower)) {
            throw new IllegalArgumentException("Ce nom de nation est déjà pris.");
        }

        NationInfo nation = new NationInfo(
                UUID.randomUUID(),
                name,
                ownerUuid,
                NationLevel.HAMLET.id,
                desc, //description
                System.currentTimeMillis(),
                System.currentTimeMillis());

        // Update RAM
        nationsById.put(nation.getId(), nation);
        nationIdByNameLower.put(lower, nation.getId());
        memberToNation.put(ownerUuid, nation.getId());

        // Persist DB
        db.saveNation(nation);
        db.addMember(nation.getId(), ownerUuid, "OWNER");

        return nation;
    }

    public NationInfo getNationById(UUID nationId) {
        return nationsById.get(nationId);
    }

    public boolean canModifyNation(UUID actorUuid, NationInfo nation) {
        return nation != null && nation.getOwnerUuid().equals(actorUuid);
    }

    /**
     * Modifie nom + description d'une nation (pour UI).
     * Vérifie: owner, validité, unicité du nom (case-insensitive).
     */
    public void updateNationInfo(UUID actorUuid, UUID nationId, String rawName, String rawDescription) {
        NationInfo nation = nationsById.get(nationId);
        if (nation == null) throw new IllegalArgumentException("Nation introuvable.");
        if (!isOwner(actorUuid, nation)) throw new IllegalArgumentException("Tu n'es pas le chef de cette nation.");

        String name = validateAndNormalizeName(rawName);
        String newLower = name.toLowerCase();
        String description = normalizeDescription(rawDescription);

        UUID existing = nationIdByNameLower.get(newLower);
        if (existing != null && !existing.equals(nationId)) {
            throw new IllegalArgumentException("Ce nom de nation est déjà pris.");
        }

        String oldLower = nation.getNameLower();
        if (!oldLower.equals(newLower)) {
            nationIdByNameLower.remove(oldLower);
            nationIdByNameLower.put(newLower, nationId);
        }

        nation.setName(name);
        nation.setDescription(description);

        db.saveNation(nation);
    }

    public boolean isInNation(UUID playerUuid) { return memberToNation.containsKey(playerUuid); }

    public boolean isOwner(UUID playerUuid, NationInfo nation) { return nation != null && nation.getOwnerUuid().equals(playerUuid); }

    private String validateAndNormalizeName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();

        if (name.length() < 3 || name.length() > 20) {
            throw new IllegalArgumentException("Le nom doit faire entre 3 et 20 caractères.");
        }
        if (!name.matches("[a-zA-Z0-9 _-]+")) {
            throw new IllegalArgumentException("Caractères invalides (autorisé: lettres, chiffres, espace, _ et -).");
        }
        return name;
    }

    private String normalizeDescription(String raw) {
        String d = raw == null ? "" : raw.trim().replace("\n", "");
        if (d.length() > 120) throw new IllegalArgumentException("La description ne doit pas dépasser 120 caractères.");
        return d;
    }

    public NationRole getRole(UUID nationId, UUID memberUuid) {
        var map = membersByNationId.get(nationId);
        if (map == null) return null;
        return map.get(memberUuid);
    }

    public void invite(UUID actorUuid, String targetUsername) {
        NationInfo nation = getNationOf(actorUuid);
        if (nation == null) throw new IllegalArgumentException("Tu n'as pas de nation.");

        NationRole actorRole = getRole(nation.getId(), actorUuid);
        if (actorRole == null || !actorRole.canInvite()) throw new IllegalArgumentException("Tu n'as pas le droit d'inviter.");

        PlayerRef target = Universe.get().getPlayers().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(targetUsername))
                .findFirst().orElse(null);

        if (target == null) throw new IllegalArgumentException("Joueur introuvable ou hors ligne.");
        if (memberToNation.containsKey(target.getUuid())) throw new IllegalArgumentException("Ce joueur est déjà dans une nation.");

        NationInvite invite = new NationInvite(target.getUuid(), nation.getId(), actorUuid, System.currentTimeMillis());
        invitesByRecipient.put(target.getUuid(), invite);

        target.sendMessage(Message.raw("Tu as été invité dans la nation " + nation.getName() + ". Fais /nation accept"));
    }

    public void acceptInvite(UUID recipientUuid) {
        NationInvite inv = invitesByRecipient.get(recipientUuid);
        if (inv == null) throw new IllegalArgumentException("Aucune invitation.");

        if (memberToNation.containsKey(recipientUuid)) throw new IllegalArgumentException("Tu es déjà dans une nation.");

        NationInfo nation = nationsById.get(inv.nationId());
        if (nation == null) throw new IllegalArgumentException("Nation introuvable.");

        // add member
        memberToNation.put(recipientUuid, inv.nationId());

        membersByNationId.computeIfAbsent(inv.nationId(), k -> new ConcurrentHashMap<>())
                .put(recipientUuid, NationRole.MEMBER);

        db.addMember(inv.nationId(), recipientUuid, NationRole.MEMBER.name());
        invitesByRecipient.remove(recipientUuid);
    }
}
