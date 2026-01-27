package com.universe.nations.nation;

import com.hypixel.hytale.logger.HytaleLogger;
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
        String name = rawName.trim();
        String lower = name.toLowerCase();
        String desc = rawDesc.trim();

        if (name.length() < 3 || name.length() > 20) {
            throw new IllegalArgumentException("Le nom doit faire entre 3 et 20 caractères.");
        }
        if (!name.matches("[a-zA-Z0-9 _-]+")) {
            throw new IllegalArgumentException("Caractères invalides (autorisé: lettres, chiffres, espace, _ et -).");
        }
        if (memberToNation.containsKey(ownerUuid)) {
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

        if (!canModifyNation(actorUuid, nation)) {
            throw new IllegalArgumentException("Tu n'es pas le chef de cette nation.");
        }

        String name = rawName == null ? "" : rawName.trim();
        String description = rawDescription == null ? "" : rawDescription.trim().replace("\n", "");

        // Validation nom
        if (name.length() < 3 || name.length() > 20) {
            throw new IllegalArgumentException("Le nom doit faire entre 3 et 20 caractères.");
        }
        if (!name.matches("[a-zA-Z0-9 _-]+")) {
            throw new IllegalArgumentException("Caractères invalides (autorisé: lettres, chiffres, espace, _ et -).");
        }

        // Validation description (optionnel mais pratique)
        if (description.length() > 120) {
            throw new IllegalArgumentException("La description ne doit pas dépasser 120 caractères.");
        }

        String newLower = name.toLowerCase();
        UUID existing = nationIdByNameLower.get(newLower);
        if (existing != null && !existing.equals(nationId)) {
            throw new IllegalArgumentException("Ce nom de nation est déjà pris.");
        }

        // Mise à jour index name_lower si changement
        String oldLower = nation.getNameLower();
        if (!oldLower.equals(newLower)) {
            nationIdByNameLower.remove(oldLower);
            nationIdByNameLower.put(newLower, nationId);
        }

        // Mise à jour objet
        nation.setName(name);              // nécessite NationInfo.setName(...)
        nation.setDescription(description);// nécessite NationInfo.setDescription(...)

        // Persist
        db.saveNation(nation);
    }
}
