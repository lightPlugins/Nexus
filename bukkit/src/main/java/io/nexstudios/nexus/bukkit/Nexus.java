package io.nexstudios.nexus.bukkit;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.zaxxer.hikari.HikariDataSource;
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
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
import io.nexstudios.nexus.common.logging.NexusLogger;
import io.nexstudios.nexus.common.files.NexusFile;
import io.nexstudios.nexus.common.files.NexusFileReader;
import io.papermc.paper.command.brigadier.Commands;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public final class Nexus extends JavaPlugin {

    // Nexus fields
    @Getter
    private static Nexus instance;
    public static NexusLogger nexusLogger;
    public NexusFile settingsFile;
    public NexusFileReader languageFiles;
    public NexusLanguage nexusLanguage;

    // API Services
    public MessageSender messageSender;

    // Command Manager
    public PaperCommandManager commandManager;

    // Third party hooks
    public PapiHook papiHook;

    // Database related fields
    private AbstractDatabase abstractDatabase;
    public HikariDataSource hikariDataSource;

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
    }

    private void commandCompletions() {
        // Register command completions for various types
        commandManager.getCommandCompletions().registerCompletion("languages", c -> {
            List<String> completions = nexusLanguage.getAvailableLanguages().keySet().stream().toList();
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
    }

    private void registerCommands() {
        // Register commands using the command manager
        commandCompletions();
        commandManager.registerCommand(new ReloadCommand());
        commandManager.registerCommand(new SwitchLanguage());
        int size = commandManager.getRegisteredRootCommands().size();
        nexusLogger.info("Successfully registered " + size  + " command(s).");
    }
}
