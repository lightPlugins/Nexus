package io.nexstudios.nexus.bukkit.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.bukkit.database.PooledDatabase;
import io.nexstudios.nexus.bukkit.database.model.ConnectionProperties;
import io.nexstudios.nexus.bukkit.database.model.DatabaseCredentials;
import io.nexstudios.nexus.bukkit.database.model.DatabaseTypes;

public class MariaDatabase extends PooledDatabase {

    private final DatabaseCredentials credentials;
    private final ConnectionProperties connectionProperties;
    private final String poolName = "nexus-mariadb-";

    public MariaDatabase(Nexus parent, DatabaseCredentials credentials, ConnectionProperties connectionProperties) {
        super(parent);
        this.connectionProperties = connectionProperties;
        this.credentials = credentials;
    }

    @Override
    public void connect() {

        HikariConfig hikari = new HikariConfig();

        hikari.setPoolName(poolName + POOL_COUNTER.getAndIncrement());

        this.applyCredentials(hikari, credentials, connectionProperties);
        this.applyConnectionProperties(hikari, connectionProperties);
        this.addDefaultDataSourceProperties(hikari);
        this.hikari = new HikariDataSource(hikari);
    }

    private void applyCredentials(HikariConfig hikari, DatabaseCredentials credentials, ConnectionProperties connectionProperties) {
        hikari.setJdbcUrl("jdbc:mariadb://" + credentials.host() + ":" + credentials.port() + "/" + credentials.databaseName() + "?characterEncoding=" + connectionProperties.characterEncoding());
        hikari.setUsername(credentials.userName());
        hikari.setPassword(credentials.password());
    }

    private void applyConnectionProperties(HikariConfig hikari, ConnectionProperties connectionProperties) {
        ExpertParams.applyConnectionProperties(hikari, connectionProperties);
    }

    private void addDefaultDataSourceProperties(HikariConfig hikari) {
        ExpertParams.addDefaultDataSourceProperties(hikari);
    }

    @Override
    public DatabaseTypes getDatabaseType() {
        return DatabaseTypes.MARIADB;
    }
}