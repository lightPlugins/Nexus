package io.nexstudios.nexus.bukkit.database.api;

import javax.sql.DataSource;

public class DefaultNexusDatabaseService implements NexusDatabaseService {
    private final DataSource dataSource;

    public DefaultNexusDatabaseService(DataSource dataSource) {
        if (dataSource == null) throw new IllegalArgumentException("dataSource must not be null");
        this.dataSource = dataSource;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }
}