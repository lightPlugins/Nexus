package io.nexstudios.nexus.bukkit.levels;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.function.Consumer;

record LevelDao(String tableName) {

    public Optional<LevelProgress> load(Connection c, UUID playerId, LevelKey key) throws Exception {
        String sql = "SELECT level, xp FROM " + tableName + " WHERE player_uuid=? AND namespace=? AND level_key=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, key.getNamespace());
            ps.setString(3, key.getKey());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int level = rs.getInt(1);
                    double xp = rs.getDouble(2);
                    return Optional.of(new LevelProgress(playerId, key, level, xp));
                }
            }
        }
        return Optional.empty();
    }

    public void upsertBatch(Connection c, List<LevelProgress> batch) throws Exception {
        String update = "UPDATE " + tableName + " SET level=?, xp=?, updated_at=CURRENT_TIMESTAMP " +
                "WHERE player_uuid=? AND namespace=? AND level_key=?";
        String insert = "INSERT INTO " + tableName + " (player_uuid, namespace, level_key, level, xp, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement up = c.prepareStatement(update);
             PreparedStatement in = c.prepareStatement(insert)) {

            for (LevelProgress p : batch) {
                up.clearParameters();
                up.setInt(1, p.getLevel());
                up.setDouble(2, p.getXp());
                up.setString(3, p.getPlayerId().toString());
                up.setString(4, p.getKey().getNamespace());
                up.setString(5, p.getKey().getKey());
                int updated = up.executeUpdate();
                if (updated == 0) {
                    in.clearParameters();
                    in.setString(1, p.getPlayerId().toString());
                    in.setString(2, p.getKey().getNamespace());
                    in.setString(3, p.getKey().getKey());
                    in.setInt(4, p.getLevel());
                    in.setDouble(5, p.getXp());
                    in.addBatch();
                }
            }
            in.executeBatch();
        }
    }

    public void streamAll(Connection c, Consumer<LevelProgress> consumer) throws Exception {
        String sql = "SELECT player_uuid, namespace, level_key, level, xp FROM " + tableName;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            // je nach Treiber kann man Fetch Size setzen:
            try {
                ps.setFetchSize(5000);
            } catch (Throwable ignored) {
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID pid = UUID.fromString(rs.getString(1));
                    String ns = rs.getString(2);
                    String key = rs.getString(3);
                    int level = rs.getInt(4);
                    double xp = rs.getDouble(5);
                    consumer.accept(new LevelProgress(pid, new LevelKey(ns, key), level, xp));
                }
            }
        }
    }


    public void ensureSchema(Connection c) throws Exception {
        String ddl = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "player_uuid VARCHAR(36) NOT NULL," +
                "namespace VARCHAR(64) NOT NULL," +
                "level_key VARCHAR(64) NOT NULL," +
                "level INT NOT NULL," +           // Level jetzt INT
                "xp DOUBLE NOT NULL," +
                "updated_at TIMESTAMP NULL," +
                "PRIMARY KEY (player_uuid, namespace, level_key)" +
                ")";
        try (PreparedStatement ps = c.prepareStatement(ddl)) {
            ps.execute();
        }
    }
}