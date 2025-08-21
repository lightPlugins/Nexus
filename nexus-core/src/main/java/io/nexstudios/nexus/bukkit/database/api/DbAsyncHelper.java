package io.nexstudios.nexus.bukkit.database.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// Java
public final class DbAsyncHelper {
    private final NexusDatabaseService db;
    private final java.util.concurrent.ExecutorService ioExec;

    public DbAsyncHelper(NexusDatabaseService db) {
        this.db = db;
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.ioExec = java.util.concurrent.Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "other-plugin-db-io");
            t.setDaemon(true);
            return t;
        });
    }

    public <T> CompletableFuture<List<T>> queryAsync(
            String sql,
            Function<ResultSet, T> mapper,
            Object... params
    ) {
        return CompletableFuture.supplyAsync(() -> {
            java.util.List<T> result = new java.util.ArrayList<>();
            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // Params setzen
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }

                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(mapper.apply(rs));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("DB query failed: " + sql, e);
            }
            return Collections.unmodifiableList(result);
        }, ioExec);
    }

    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                return ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("DB update failed: " + sql, e);
            }
        }, ioExec);
    }

    public void shutdown() {
        ioExec.shutdown();
    }
}