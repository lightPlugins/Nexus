package io.nexstudios.nexus.bukkit.database;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.database.model.DatabaseTypes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public abstract class AbstractDatabase {

    protected final NexusPlugin plugin;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "nexus-db-monitor");
        t.setDaemon(true);
        return t;
    });

    // Bounded ThreadPool mit Backpressure; Größe wird dynamisch an Hikari angepasst
    private volatile ThreadPoolExecutor asyncSqlExecutor = new ThreadPoolExecutor(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(512),
            r -> {
                Thread t = new Thread(r, "nexus-db-async");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    AbstractDatabase(NexusPlugin plugin) {
        this.plugin = plugin;
        startMonitoring();
    }

    public abstract DatabaseTypes getDatabaseType();
    public abstract void connect();
    public abstract void close();
    public abstract Connection getConnection();

    /**
     * Für Implementierungen: Nach Aufbau oder Änderung des Pools aufrufen,
     * damit der Async-Executor passend zur Poolgröße skaliert.
     */
    protected void adjustAsyncExecutorForPool(int maxPoolSize) {
        int cores = Runtime.getRuntime().availableProcessors();
        int target = Math.max(2, Math.min(cores * 2, Math.max(cores, maxPoolSize * 2)));
        int queueCap = Math.max(256, maxPoolSize * 128);

        ThreadPoolExecutor current = this.asyncSqlExecutor;
        // Nur neu bauen, wenn es sich lohnt
        if (current.getCorePoolSize() == target && current.getMaximumPoolSize() == target && current.getQueue().remainingCapacity() + current.getQueue().size() == queueCap) {
            return;
        }

        ThreadPoolExecutor replacement = new ThreadPoolExecutor(
                target, target,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCap),
                r -> {
                    Thread t = new Thread(r, "nexus-db-async");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        // alten Executor geordnet herunterfahren
        ThreadPoolExecutor old = this.asyncSqlExecutor;
        this.asyncSqlExecutor = replacement;
        old.shutdown();
    }

    /**
     * Sollte von Unterklassen in close() aufgerufen werden,
     * um Monitoring und Async-Executor sauber zu stoppen.
     */
    protected void shutdownInfrastructure() {
        scheduler.shutdownNow();
        ThreadPoolExecutor exec = this.asyncSqlExecutor;
        exec.shutdown();
    }

    public CompletableFuture<Integer> executeSqlFuture(String sql, Object... replacements) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (Connection c = getConnection(); PreparedStatement statement = prepareStatement(c, sql, replacements)) {
                int affectedLines = statement.executeUpdate();
                future.complete(affectedLines);
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        }, asyncSqlExecutor);
        return future;
    }

    public boolean checkConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && connection.isValid((int) Duration.ofSeconds(2).toSeconds());
        } catch (SQLException e) {
            return false;
        }
    }

    public CompletableFuture<List<Object>> querySqlFuture(String sql, String column, Object... replacements) {
        CompletableFuture<List<Object>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (Connection c = getConnection(); PreparedStatement statement = prepareStatement(c, sql, replacements); ResultSet resultSet = statement.executeQuery()) {
                List<Object> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(resultSet.getObject(column));
                }
                future.complete(results);
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        }, asyncSqlExecutor);
        return future;
    }

    public CompletableFuture<HashMap<Object, Object>> querySqlFuture(String sql, String keyColumn, String valueColumn, Object... replacements) {
        CompletableFuture<HashMap<Object, Object>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (Connection c = getConnection(); PreparedStatement statement = prepareStatement(c, sql, replacements); ResultSet resultSet = statement.executeQuery()) {
                HashMap<Object, Object> results = new HashMap<>();
                while (resultSet.next()) {
                    results.put(resultSet.getObject(keyColumn), resultSet.getObject(valueColumn));
                }
                future.complete(results);
            } catch (SQLException e) {
                future.completeExceptionally(new RuntimeException("[Nexus] Could not execute SQL query", e));
            }
        }, asyncSqlExecutor);
        return future;
    }

    private PreparedStatement prepareStatement(Connection connection, String sql, Object... replacements) {
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            this.replaceQueryParameters(statement, replacements);
            return statement;
        } catch (SQLException e) {
            throw new RuntimeException("[Nexus] Could not prepare/read SQL statement: " + sql, e);
        }
    }

    private void replaceQueryParameters(PreparedStatement statement, Object[] replacements) {
        if (replacements != null) {
            for (int i = 0; i < replacements.length; i++) {
                int position = i + 1;
                Object value = replacements[i];
                try {
                    statement.setObject(position, value);
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to set query parameter at position " + position +
                            " to " + value + " for query: " + statement, e);
                }
            }
        }
    }

    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            Date date = new Date();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
            String currentTime = simpleDateFormat.format(date);

            try (Connection connection = getConnection()) {
                boolean valid = connection != null && connection.isValid((int) Duration.ofSeconds(2).toSeconds());
                if (!valid) {
                    plugin.getNexusLogger().error(List.of(
                            "Database connection is not valid at " + currentTime + ".",
                            "Attempting to reconnect..."
                    ));
                    connect();
                    try (Connection after = getConnection()) {
                        if (after != null && after.isValid((int) Duration.ofSeconds(2).toSeconds())) {
                            plugin.getNexusLogger().info(List.of("Database connection has been re-established."));
                        } else {
                            plugin.getNexusLogger().error("Database connection could not be re-established.");
                        }
                    }
                }
            } catch (SQLException | RuntimeException e) {
                plugin.getNexusLogger().error(List.of(
                        "Error while checking database connection: " + e.getMessage(),
                        "Attempting to reconnect..."
                ));
                try {
                    connect();
                    if (checkConnection()) {
                        plugin.getNexusLogger().info("Database connection has been re-established.");
                    } else {
                        plugin.getNexusLogger().error("Database connection could not be re-established.");
                    }
                } catch (Throwable t) {
                    plugin.getNexusLogger().error("Reconnect attempt failed: " + t.getMessage());
                }
            }
        }, 5, 5, TimeUnit.MINUTES);
    }
}