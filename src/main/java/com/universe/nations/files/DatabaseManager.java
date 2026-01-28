package com.universe.nations.files;

import com.hypixel.hytale.logger.HytaleLogger;
import com.universe.nations.nation.NationInfo;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final HytaleLogger logger;
    private final Connection connection;

    public DatabaseManager(HytaleLogger logger, String dbPath) {
        this.logger = logger;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Couldn't find JDBC driver for SQLite");
        }

        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
            createTables();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing database: " + e.getMessage(), e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS nations (
                  id TEXT PRIMARY KEY,
                  name TEXT NOT NULL,
                  name_lower TEXT NOT NULL UNIQUE,
                  owner_uuid TEXT NOT NULL,
                  level INTEGER NOT NULL,
                  description TEXT NOT NULL DEFAULT '',
                  created_at INTEGER NOT NULL,
                  updated_at INTEGER NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS nation_members (
                  nation_id TEXT NOT NULL,
                  member_uuid TEXT NOT NULL UNIQUE,
                  role TEXT NOT NULL,
                  PRIMARY KEY (nation_id, member_uuid),
                  FOREIGN KEY (nation_id) REFERENCES nations(id) ON DELETE CASCADE
                )
            """);
        }
    }

    public Map<UUID, NationInfo> loadNations() {
        Map<UUID, NationInfo> out = new HashMap<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM nations")) {

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                NationInfo n = new NationInfo(
                        id,
                        rs.getString("name"),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getInt("level"),
                        rs.getString("description"),
                        rs.getLong("created_at"),
                        rs.getLong("updated_at")
                );
                out.put(id, n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public Map<UUID, UUID> loadMemberToNation() {
        Map<UUID, UUID> out = new HashMap<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT nation_id, member_uuid FROM nation_members")) {
            while (rs.next()) {
                UUID nationId = UUID.fromString(rs.getString("nation_id"));
                UUID member = UUID.fromString(rs.getString("member_uuid"));
                out.put(member, nationId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public void saveNation(NationInfo nation) {
        try (PreparedStatement ps = connection.prepareStatement("""
            REPLACE INTO nations (id, name, name_lower, owner_uuid, level, description, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """)) {
            ps.setString(1, nation.getId().toString());
            ps.setString(2, nation.getName());
            ps.setString(3, nation.getNameLower());
            ps.setString(4, nation.getOwnerUuid().toString());
            ps.setInt(5, nation.getLevel());
            ps.setString(6, nation.getDescription());
            ps.setLong(7, nation.getCreatedAt());
            ps.setLong(7, nation.getUpdatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addMember(UUID nationId, UUID memberUuid, String role) {
        try (PreparedStatement ps = connection.prepareStatement("""
            REPLACE INTO nation_members (nation_id, member_uuid, role)
            VALUES (?, ?, ?)
        """)) {
            ps.setString(1, nationId.toString());
            ps.setString(2, memberUuid.toString());
            ps.setString(3, role);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
