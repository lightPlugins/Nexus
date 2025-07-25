package io.nexstudios.nexus.bukkit.database;

import com.zaxxer.hikari.HikariDataSource;
import io.nexstudios.nexus.bukkit.NexusPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PooledDatabase extends AbstractDatabase {

    protected static final AtomicInteger POOL_COUNTER = new AtomicInteger(0);
    protected HikariDataSource hikari;

    public PooledDatabase(NexusPlugin plugin) {
        super(plugin);
    }

    @Override
    public void close() {
        NexusPlugin.nexusLogger.error("Attempting to close HikariCP connection pool...");
        if (this.hikari != null) {
            this.hikari.close();
        }
    }

    @Override
    public Connection getConnection() {
        try {
            return this.hikari.getConnection();
        } catch (SQLException e) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Failed to get connection from HikariCP",
                    "Error: " + e.getMessage()
            ));
            e.printStackTrace();
            return null;
        }
    }
}
