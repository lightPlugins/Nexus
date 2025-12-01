package io.nexstudios.nexus.bukkit.levels;

import io.nexstudios.nexus.bukkit.NexusPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.function.Consumer;

record LevelDao(String tableName) {

    public Optional<LevelProgress> load(Connection c, UUID playerId, LevelKey key) throws Exception {
        String sql = "SELECT xp, last_applied_level FROM " + tableName + " WHERE player_uuid=? AND namespace=? AND level_key=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, key.getNamespace());
            ps.setString(3, key.getKey());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double totalXp = rs.getDouble(1);
                    int lastApplied;
                    try {
                        lastApplied = rs.getInt(2);
                    } catch (Exception ignored) {
                        // Fallback, falls Spalte in sehr alten DBs noch fehlt
                        lastApplied = 0;
                    }
                    // level/xp werden später anhand LevelDefinition berechnet
                    return Optional.of(new LevelProgress(playerId, key, 0, 0.0d, totalXp, lastApplied));
                }
            }
        }
        return Optional.empty();
    }

    public void upsertBatch(Connection c, List<LevelProgress> batch) throws Exception {
        NexusPlugin.nexusLogger.info("[NexLevel] upsertBatch called with " + batch.size() + " entries");

        String update = "UPDATE " + tableName + " SET xp=?, last_applied_level=?, updated_at=CURRENT_TIMESTAMP " +
                "WHERE player_uuid=? AND namespace=? AND level_key=?";
        String insert = "INSERT INTO " + tableName + " (player_uuid, namespace, level_key, xp, last_applied_level, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement up = c.prepareStatement(update);
             PreparedStatement in = c.prepareStatement(insert)) {

            for (LevelProgress p : batch) {
                double totalXp = p.getTotalXp();
                int lastApplied = p.getLastAppliedLevel();

                up.clearParameters();
                up.setDouble(1, totalXp);
                up.setInt(2, lastApplied);
                up.setString(3, p.getPlayerId().toString());
                up.setString(4, p.getKey().getNamespace());
                up.setString(5, p.getKey().getKey());
                int updated = up.executeUpdate();
                if (updated == 0) {
                    in.clearParameters();
                    in.setString(1, p.getPlayerId().toString());
                    in.setString(2, p.getKey().getNamespace());
                    in.setString(3, p.getKey().getKey());
                    in.setDouble(4, totalXp);
                    in.setInt(5, lastApplied);
                    in.addBatch();
                }
            }
            in.executeBatch();
        }

        NexusPlugin.nexusLogger.info("[NexLevel] upsertBatch finished");
    }

    public void streamAll(Connection c, Consumer<LevelProgress> consumer) throws Exception {
        String sql = "SELECT player_uuid, namespace, level_key, xp, last_applied_level FROM " + tableName;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            try {
                ps.setFetchSize(5000);
            } catch (Throwable ignored) {
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID pid = UUID.fromString(rs.getString(1));
                    String ns = rs.getString(2);
                    String key = rs.getString(3);
                    double totalXp = rs.getDouble(4);
                    int lastApplied;
                    try {
                        lastApplied = rs.getInt(5);
                    } catch (Exception ignored) {
                        lastApplied = 0;
                    }
                    consumer.accept(new LevelProgress(pid, new LevelKey(ns, key), 0, 0.0d, totalXp, lastApplied));
                }
            }
        }
    }

    public void ensureSchema(Connection c) throws Exception {
        String ddl = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "player_uuid VARCHAR(36) NOT NULL," +
                "namespace VARCHAR(64) NOT NULL," +
                "level_key VARCHAR(64) NOT NULL," +
                "xp DOUBLE NOT NULL," +
                "last_applied_level INT NOT NULL DEFAULT 0," +
                "updated_at TIMESTAMP NULL," +
                "PRIMARY KEY (player_uuid, namespace, level_key)" +
                ")";
        try (PreparedStatement ps = c.prepareStatement(ddl)) {
            ps.execute();
        }

        // Sicherstellen, dass die Spalte auch in bestehenden Tabellen existiert
        try (PreparedStatement ps = c.prepareStatement(
                "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS last_applied_level INT NOT NULL DEFAULT 0"
        )) {
            ps.execute();
        } catch (Exception ignored) {
            // DBs ohne IF NOT EXISTS können hier einen Fehler werfen, wenn die Spalte schon existiert – dann ignorieren
        }
    }

    // Delete-Methoden bleiben wie sie sind
    public int deleteByPlayer(Connection c, UUID playerId) throws Exception {
        String sql = "DELETE FROM " + tableName + " WHERE player_uuid=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            return ps.executeUpdate();
        }
    }

    public int deleteByPlayerAndKey(Connection c, UUID playerId, LevelKey key) throws Exception {
        String sql = "DELETE FROM " + tableName + " WHERE player_uuid=? AND namespace=? AND level_key=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, key.getNamespace());
            ps.setString(3, key.getKey());
            return ps.executeUpdate();
        }
    }

    public int deleteByKey(Connection c, LevelKey key) throws Exception {
        String sql = "DELETE FROM " + tableName + " WHERE namespace=? AND level_key=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key.getNamespace());
            ps.setString(2, key.getKey());
            return ps.executeUpdate();
        }
    }

    public int deleteAll(Connection c) throws Exception {
        String sql = "DELETE FROM " + tableName;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }
}