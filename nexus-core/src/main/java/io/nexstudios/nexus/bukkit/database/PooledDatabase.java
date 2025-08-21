package io.nexstudios.nexus.bukkit.database;

import com.zaxxer.hikari.HikariDataSource;
import io.nexstudios.nexus.bukkit.NexusPlugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PooledDatabase extends AbstractDatabase {

    protected static final AtomicInteger POOL_COUNTER = new AtomicInteger(0);
    protected volatile HikariDataSource hikari;
    private final Object poolLock = new Object();

    public PooledDatabase(NexusPlugin plugin) {
        super(plugin);
    }

    /**
     * Ermöglicht anderen Plugins den Zugriff auf den Pool, ohne Connection zu sharen.
     */
    public DataSource getDataSource() {
        HikariDataSource ds = this.hikari;
        if (ds == null) {
            throw new IllegalStateException("HikariCP pool is not initialized. Did you call connect()?");
        }
        return ds;
    }

    @Override
    public void close() {
        plugin.getNexusLogger().info("Closing HikariCP connection pool...");
        synchronized (poolLock) {
            HikariDataSource ds = this.hikari;
            this.hikari = null;
            if (ds != null) {
                try {
                    ds.close();
                } catch (Exception e) {
                    NexusPlugin.nexusLogger.error(List.of(
                            "Error while closing HikariCP pool",
                            "Error: " + e.getMessage()
                    ));
                }
            }
        }
        // Infrastruktur (Executor/Scheduler) der Basisklasse stoppen
        shutdownInfrastructure();
    }

    @Override
    public Connection getConnection() {
        HikariDataSource ds = this.hikari;
        if (ds == null) {
            throw new IllegalStateException("HikariCP pool is not initialized. Did you call connect()?");
        }
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Failed to get connection from HikariCP",
                    "Error: " + e.getMessage()
            ));
            throw new RuntimeException("Unable to obtain DB connection from pool", e);
        }
    }

    /**
     * Tauscht den Pool atomar aus, passt den Async-Executor an und schließt ggf. den alten Pool.
     */
    protected void swapInNewDataSource(HikariDataSource newDs) {
        synchronized (poolLock) {
            HikariDataSource old = this.hikari;
            this.hikari = newDs;
            // Executor-Größe an neue Poolgröße koppeln
            adjustAsyncExecutorForPool(newDs.getMaximumPoolSize());
            if (old != null) {
                try {
                    old.close();
                } catch (Exception e) {
                    NexusPlugin.nexusLogger.error(List.of(
                            "Error while closing previous HikariCP pool",
                            "Error: " + e.getMessage()
                    ));
                }
            }
        }
    }
}