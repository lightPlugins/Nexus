package io.nexstudios.nexus.bukkit;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariDataSource;
import io.nexstudios.nexus.bukkit.command.OpenMenus;
import io.nexstudios.nexus.bukkit.command.ReloadCommand;
import io.nexstudios.nexus.bukkit.command.SwitchLanguage;
import io.nexstudios.nexus.bukkit.database.AbstractDatabase;
import io.nexstudios.nexus.bukkit.database.impl.MariaDatabase;
import io.nexstudios.nexus.bukkit.database.impl.MySQLDatabase;
import io.nexstudios.nexus.bukkit.database.impl.SQLiteDatabase;
import io.nexstudios.nexus.bukkit.database.model.ConnectionProperties;
import io.nexstudios.nexus.bukkit.database.model.DatabaseCredentials;
import io.nexstudios.nexus.bukkit.handler.MessageSender;
import io.nexstudios.nexus.bukkit.hooks.PapiHook;
import io.nexstudios.nexus.bukkit.hooks.VaultHook;
import io.nexstudios.nexus.bukkit.inventory.event.NexusMenuEvent;
import io.nexstudios.nexus.bukkit.inventory.models.InventoryData;
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
import io.nexstudios.nexus.bukkit.utils.NexusLogger;
import io.nexstudios.nexus.bukkit.files.NexusFile;
import io.nexstudios.nexus.bukkit.files.NexusFileReader;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class Nexus extends JavaPlugin {

    // Nexus fields
    @Getter
    private static Nexus instance;
    public static NexusLogger nexusLogger;
    public NexusFile settingsFile;
    public NexusFileReader languageFiles;
    public NexusFileReader inventoryFiles;
    public NexusLanguage nexusLanguage;

    // API Services
    public MessageSender messageSender;

    // Command Manager
    public PaperCommandManager commandManager;

    // Third party hooks
    public PapiHook papiHook;
    public VaultHook vaultHook;

    // Database related fields
    private AbstractDatabase abstractDatabase;
    public HikariDataSource hikariDataSource;

    // test inventories
    private final Map<String, InventoryData> nexusInventoryData = new HashMap<>();

    @Override
    public void onLoad() {
        // Plugin startup logic
        instance = this;
        nexusLogger = new NexusLogger("<reset>[<yellow>Nexus<reset>]", true, 99, "<yellow>");
        nexusLogger.info("Nexus is loading...");
        onReload();
        nexusLogger.info("Check for compatibility with other plugins...");
        checkForHooks();
        nexusLogger.info("Initializing database connection...");
        initDatabase();
    }

    @Override
    public void onEnable() {
        nexusLogger.info("Register Nexus commands...");
        commandManager = new PaperCommandManager(this);
        registerCommands();
        nexusLogger.info("Register Nexus events...");
        registerEvents();
        nexusLogger.info("Nexus is enabled");
    }

    public NexusLogger getNexusLogger() {
        return nexusLogger;
    }

    @Override
    public void onDisable() {
        nexusLogger.info("Nexus has been disabled!");
    }

    public void onReload() {
        loadNexusFiles();
        messageSender = new MessageSender(nexusLanguage);
        loadInventories();
    }

    private void commandCompletions() {
        // Register command completions for various types
        commandManager.getCommandCompletions().registerCompletion("languages", c -> {
            List<String> completions = nexusLanguage.getAvailableLanguages().keySet().stream().toList();
            return ImmutableList.copyOf(completions);
        });
        commandManager.getCommandCompletions().registerCompletion("inventories", c -> {
            List<String> completions = nexusInventoryData.keySet().stream().toList();
            return ImmutableList.copyOf(completions);
        });
    }

    private void loadNexusFiles() {
        settingsFile = new NexusFile(this, "settings.yml", nexusLogger, true);
        // preload the default english language file.
        new NexusFile(this, "languages/english.yml", nexusLogger, true);
        nexusLogger.setDebugEnabled(settingsFile.getBoolean("logging.debug.enable", true));
        nexusLogger.setDebugLevel(settingsFile.getInt("logging.debug.level", 3));
        // initialize the NexusFileReader for languages.
        languageFiles = new NexusFileReader("languages", this);
        // Load all language files as FileConfigurations.
        nexusLanguage = new NexusLanguage(languageFiles, nexusLogger);
        inventoryFiles = new NexusFileReader("inventories", this);
        nexusLogger.info("All Nexus files have been (re)loaded successfully.");
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
                        "Valid database types are: SQLite, MySQL, MariaDB.",
                        "Disabling all nexus related plugins."));
                onDisable();
                return;
            }

            switch (databaseType.toLowerCase()) {
                case "sqlite":
                    this.abstractDatabase = new SQLiteDatabase(this, connectionProperties);
                    this.getNexusLogger().info("Using SQLite (local) database.");
                    break;
                case "mysql":
                    this.abstractDatabase = new MySQLDatabase(this, credentials, connectionProperties);
                    this.getNexusLogger().info("Using MySQL (remote) database.");
                    break;
                case "mariadb":
                    this.abstractDatabase = new MariaDatabase(this, credentials, connectionProperties);
                    this.getNexusLogger().info("Using MariaDB (remote) database.");
                    break;
                default:
                    this.getNexusLogger().error(List.of(
                            "Database type not specified in config. Disabling plugin.",
                            "Please specify the database type in the config file.",
                            "Valid database types are: SQLite, MySQL, MariaDB.",
                            "Disabling all nexus related plugins."));
                    return;
            }

            this.abstractDatabase.connect();

        } catch (Exception e) {
            getNexusLogger().error(List.of(
                    "Could not maintain Database Connection.",
                    "Please check your database connection & settings in the config file.",
                    "Disabling nexus related plugins."));
            throw new RuntimeException("Could not maintain Database Connection.", e);
        }
    }

    private void checkForHooks() {
        // Check if PlaceholderAPI is installed and register the hook
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papiHook = new PapiHook();
            nexusLogger.info("<yellow>PlaceholderAPI<reset> hook registered successfully.");
        }
        // Check if Vault is installed and register the hook
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            vaultHook = new VaultHook(this, nexusLogger);
            if (vaultHook.getEconomy() != null) {
                nexusLogger.info("<yellow>Vault Economy<reset> hook registered successfully.");
            } else {
                nexusLogger.warning("Vault Economy hook could not be registered. Economy provider is null.");
            }
        } else {
            nexusLogger.warning("Vault is not installed or enabled. Vault hook is not be available.");
        }
    }

    public FileConfiguration getInventoryFileByName(String name) {
        File file = inventoryFiles.getFiles().stream()
                .filter(f -> f.getName().equalsIgnoreCase(name + ".yml"))
                .findFirst()
                .orElse(null);
        if (file == null) {
            nexusLogger.debug(List.of("Inventory file not found: " + name), 2);
            return null;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void registerCommands() {
        // Register commands using the command manager
        commandCompletions();
        commandManager.registerCommand(new ReloadCommand());
        commandManager.registerCommand(new SwitchLanguage());
        commandManager.registerCommand(new OpenMenus());
        int size = commandManager.getRegisteredRootCommands().size();
        nexusLogger.info("Successfully registered " + size  + " command(s).");
    }
    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new NexusMenuEvent(), this);
    }

    public void loadInventories() {

        getNexusLogger().info("Loading inventories ...");
        this.nexusInventoryData.clear();

        // create InventoryData from inventoryfiles
        List<File> files = inventoryFiles.getFiles();
        if (files.isEmpty()) {
            // If no inventory files are found, log a warning and skip loading inventories
            getNexusLogger().warning("No inventory files found. Skipping inventory loading.");
            return;
        }

        files.forEach(file -> {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = file.getName().replace(".yml", "");
                InventoryData inventoryData = new InventoryData(config, nexusLanguage, id);
                this.nexusInventoryData.put(id, inventoryData);
                getNexusLogger().info("Loaded inventory: " + id);
            } catch (Exception e) {
                getNexusLogger().error(List.of(
                        "Failed to load inventory from file: " + file.getName(),
                        "Error: " + e.getMessage()
                ));
                e.printStackTrace();
            }
        });

        getNexusLogger().info("Successfully loaded " + this.nexusInventoryData.size() + " inventories.");
    }
}
