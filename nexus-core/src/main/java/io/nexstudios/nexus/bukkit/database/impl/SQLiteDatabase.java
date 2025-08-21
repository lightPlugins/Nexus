package io.nexstudios.nexus.bukkit.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.database.PooledDatabase;
import io.nexstudios.nexus.bukkit.database.model.ConnectionProperties;
import io.nexstudios.nexus.bukkit.database.model.DatabaseTypes;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SQLiteDatabase extends PooledDatabase {

    private static final String FILE_NAME = "nexus.db";

    private final String filePath;
    private final ConnectionProperties connectionProperties;

    public SQLiteDatabase(NexusPlugin plugin, ConnectionProperties connectionProperties) {
        super(plugin);
        this.connectionProperties = connectionProperties;
        this.filePath = this.plugin.getDataFolder().getPath() + File.separator + FILE_NAME;
    }

    @Override
    public DatabaseTypes getDatabaseType() {
        return DatabaseTypes.SQLITE;
    }

    @Override
    public void connect() {

        this.createDBFile();

        final HikariConfig hikari = new HikariConfig();

        hikari.setPoolName("nexus-sqlite-" + POOL_COUNTER.getAndIncrement());

        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setJdbcUrl("jdbc:sqlite:" + this.filePath);

        hikari.setConnectionTimeout(connectionProperties.connectionTimeout());
        hikari.setIdleTimeout(connectionProperties.idleTimeout());
        hikari.setKeepaliveTime(connectionProperties.keepAliveTime());
        hikari.setMaxLifetime(connectionProperties.maxLifetime());
        hikari.setMinimumIdle(connectionProperties.minimumIdle());
        hikari.setMaximumPoolSize(1);
        hikari.setLeakDetectionThreshold(connectionProperties.leakDetectionThreshold());
        if (connectionProperties.testQuery() != null && !connectionProperties.testQuery().isBlank()) {
            hikari.setConnectionTestQuery(connectionProperties.testQuery());
        }

        HikariDataSource newDs = new HikariDataSource(hikari);
        // Atomar tauschen, Executor anpassen, alten Pool schließen
        swapInNewDataSource(newDs);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createDBFile() {
        File dbFile = new File(this.filePath);
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }
        } catch (IOException e) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Failed to create SQLite database file",
                    "File path: " + this.filePath,
                    "Error: " + e.getMessage()
            ));
            throw new RuntimeException("Unable to create " + FILE_NAME, e);
        }
    }
}