package io.nexstudios.nexus.bukkit.database.impl;

import com.zaxxer.hikari.HikariConfig;
import io.nexstudios.nexus.bukkit.database.model.ConnectionProperties;

public class ExpertParams {

    public static void applyConnectionProperties(HikariConfig hikari, ConnectionProperties connectionProperties) {
        hikari.setConnectionTimeout(connectionProperties.connectionTimeout());
        hikari.setIdleTimeout(connectionProperties.idleTimeout());
        hikari.setKeepaliveTime(connectionProperties.keepAliveTime());
        hikari.setMaxLifetime(connectionProperties.maxLifetime());
        hikari.setMinimumIdle(connectionProperties.minimumIdle());
        hikari.setMaximumPoolSize(connectionProperties.maximumPoolSize());
        hikari.setLeakDetectionThreshold(connectionProperties.leakDetectionThreshold());
        hikari.setConnectionTestQuery(connectionProperties.testQuery());
    }

    public static void addDefaultDataSourceProperties(HikariConfig hikari) {
        hikari.addDataSourceProperty("cachePrepStmts", true);
        hikari.addDataSourceProperty("prepStmtCacheSize", 250);
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        hikari.addDataSourceProperty("useServerPrepStmts", true);
        hikari.addDataSourceProperty("useLocalSessionState", true);
        hikari.addDataSourceProperty("rewriteBatchedStatements", true);
        hikari.addDataSourceProperty("cacheResultSetMetadata", true);
        hikari.addDataSourceProperty("cacheServerConfiguration", true);
        hikari.addDataSourceProperty("elideSetAutoCommits", true);
        hikari.addDataSourceProperty("maintainTimeStats", false);
    }

}
