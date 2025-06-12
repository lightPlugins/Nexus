package io.nexstudios.nexus.bukkit;

import com.zaxxer.hikari.HikariDataSource;
import io.nexstudios.nexus.bukkit.database.SQLDatabase;
import io.nexstudios.nexus.bukkit.database.impl.MariaDatabase;
import io.nexstudios.nexus.bukkit.database.impl.MySQLDatabase;
import io.nexstudios.nexus.bukkit.database.impl.SQLiteDatabase;
import io.nexstudios.nexus.bukkit.database.model.ConnectionProperties;
import io.nexstudios.nexus.bukkit.database.model.DatabaseCredentials;
import io.nexstudios.nexus.common.logging.NexusLogger;
import io.nexstudios.nexus.common.logging.files.NexusFile;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@Getter
public final class Nexus extends JavaPlugin {

    @Getter
    private static Nexus instance;
    public static NexusLogger nexusLogger;
    public NexusFile settingsFile;

    private SQLDatabase sqlDatabase;
    public HikariDataSource hikariDataSource;

    @Override
    public void onLoad() {
        // Plugin startup logic
        instance = this;
        nexusLogger = new NexusLogger("<reset>[<yellow>Nexus<reset>]", true, 99, "<yellow>");
        nexusLogger.info("Nexus plugin is loading...");
        onReload();
    }

    @Override
    public void onEnable() {

        nexusLogger.info("Nexus plugin enabled");
    }

    public NexusLogger getNexusLogger() {
        return nexusLogger;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        nexusLogger.info("Nexus plugin has been disabled!");
    }

    public void onReload() {
        nexusLogger.info("Reloading Nexus plugin...");
        loadNexusFiles();
        nexusLogger.info("Nexus plugin reloaded successfully.");
    }

    private void loadNexusFiles() {
        settingsFile = new NexusFile(this, "settings.yml", nexusLogger, true);
        nexusLogger.setDebugEnabled(settingsFile.getBoolean("logging.debug.enable", true));
        nexusLogger.setDebugLevel(settingsFile.getInt("logging.debug.level", 3));
        nexusLogger.info("All Nexus files have been loaded successfully.");
    }

    private void initDatabase() {
        try {
            String databaseType = settingsFile.getConfig().getString("storage.type");
            ConnectionProperties connectionProperties = ConnectionProperties.fromConfig(settingsFile.getConfig());
            DatabaseCredentials credentials = DatabaseCredentials.fromConfig(settingsFile.getConfig());

            if(databaseType == null) {
                this.getNexusLogger().error(List.of(
                        "Database type not specified in config. Disabling plugin.",
                        "Please specify the database type in the config file.",
                        "Valid database types are: SQLite, MySQL.",
                        "Disabling all core related plugins."));
                onDisable();
                return;
            }

            switch (databaseType.toLowerCase()) {
                case "sqlite":
                    this.sqlDatabase = new SQLiteDatabase(this, connectionProperties);
                    this.getNexusLogger().info("Using SQLite (local) database.");
                    break;
                case "mysql":
                    this.sqlDatabase = new MySQLDatabase(this, credentials, connectionProperties);
                    this.getNexusLogger().info("Using MySQL (remote) database.");
                    break;
                case "mariadb":
                    this.sqlDatabase = new MariaDatabase(this, credentials, connectionProperties);
                    this.getNexusLogger().info("Using MariaDB (remote) database.");
                    break;
                default:
                    this.getNexusLogger().error(List.of(
                            "Database type not specified in config. Disabling plugin.",
                            "Please specify the database type in the config file.",
                            "Valid database types are: SQLite, MySQL.",
                            "Disabling all core related plugins."));
                    return;
            }

            this.sqlDatabase.connect();

        } catch (Exception e) {
            getNexusLogger().error(List.of(
                    "Could not maintain Database Connection. Disabling third party plugins.",
                    "Please check your database connection & settings in the config file.",
                    "Disabling all core related plugins."));
            throw new RuntimeException("Could not maintain Database Connection.", e);
        }
    }
}
