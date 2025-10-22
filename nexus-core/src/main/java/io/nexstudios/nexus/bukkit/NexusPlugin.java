package io.nexstudios.nexus.bukkit;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariDataSource;
import io.nexstudios.nexus.bukkit.actions.ActionFactory;
import io.nexstudios.nexus.bukkit.command.InvOpenCommand;
import io.nexstudios.nexus.bukkit.command.ReloadCommand;
import io.nexstudios.nexus.bukkit.command.StatCommand;
import io.nexstudios.nexus.bukkit.command.SwitchLanguage;
import io.nexstudios.nexus.bukkit.database.AbstractDatabase;
import io.nexstudios.nexus.bukkit.database.PooledDatabase;
import io.nexstudios.nexus.bukkit.database.api.DefaultNexusDatabaseService;
import io.nexstudios.nexus.bukkit.database.api.NexusDatabaseBukkitRegistrar;
import io.nexstudios.nexus.bukkit.database.api.NexusDatabaseService;
import io.nexstudios.nexus.bukkit.database.impl.MariaDatabase;
import io.nexstudios.nexus.bukkit.database.impl.MySQLDatabase;
import io.nexstudios.nexus.bukkit.database.impl.SQLiteDatabase;
import io.nexstudios.nexus.bukkit.database.model.ConnectionProperties;
import io.nexstudios.nexus.bukkit.database.model.DatabaseCredentials;
import io.nexstudios.nexus.bukkit.effects.EffectBinding;
import io.nexstudios.nexus.bukkit.effects.EffectFactory;
import io.nexstudios.nexus.bukkit.effects.NexusEffectsApi;
import io.nexstudios.nexus.bukkit.effects.runtime.EffectBindingRegistry;
import io.nexstudios.nexus.bukkit.effects.trigger.EntityDamageTriggerListener;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;
import io.nexstudios.nexus.bukkit.handler.MessageSender;
import io.nexstudios.nexus.bukkit.hooks.*;
import io.nexstudios.nexus.bukkit.hooks.auraskills.AuraSkillsHook;
import io.nexstudios.nexus.bukkit.hooks.mythicmobs.MythicMobsHook;
import io.nexstudios.nexus.bukkit.hooks.worldguard.WorldGuardHook;
import io.nexstudios.nexus.bukkit.inv.api.InvService;
import io.nexstudios.nexus.bukkit.inv.event.NexInventoryClickListener;
import io.nexstudios.nexus.bukkit.inv.renderer.DefaultNexItemRenderer;
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
import io.nexstudios.nexus.bukkit.placeholder.NexusPlaceholderBootstrap;
import io.nexstudios.nexus.bukkit.placeholder.NexusPlaceholderRegistry;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import io.nexstudios.nexus.bukkit.player.events.NoMoreFeed;
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
import java.util.Locale;
import java.util.Map;

@Getter
public final class NexusPlugin extends JavaPlugin {

    // Nexus fields
    @Getter
    private static NexusPlugin instance;
    public static NexusLogger nexusLogger;
    public NexusFile settingsFile;
    public NexusFileReader languageFiles;
    public NexusFileReader inventoryFiles;
    public NexusFileReader dropTableFiles;
    public NexusLanguage nexusLanguage;
    public EffectFactory effectFactory;
    public EffectBindingRegistry bindingRegistry;
    private InvService invService;

    // API Services
    public MessageSender messageSender;
    public ActionFactory actionFactory;

    // Command Manager
    public PaperCommandManager commandManager;

    // Third party hooks
    public PapiHook papiHook;
    public VaultHook vaultHook;
    public EcoSkillsHook ecoSkillsHook;
    public EcoItemsHook ecoItemsHook;
    public MythicMobsHook mythicMobsHook;
    public MMOItemsHook mmoItemsHook;
    public AuraSkillsHook auraSkillsHook;

    // Database related fields
    private AbstractDatabase abstractDatabase;
    public HikariDataSource hikariDataSource;
    private NexusDatabaseService nexusDatabaseService;

    @Override
    public void onLoad() {
        // Plugin startup logic
        instance = this;
        NexServices.init();
        nexusLogger = new NexusLogger("<reset>[<yellow>Nexus<reset>]", true, 99, "<yellow>");
        nexusLogger.info("Nexus is loading...");
        onReload();
        nexusLogger.info("Check for compatibility with other plugins...");
        nexusLogger.info("Initializing database connection...");
        initDatabase();
    }

    @Override
    public void onEnable() {
        nexusLogger.info("Register Nexus commands...");
        checkForHooks();
        nexusLogger.info("Register internal and third party placeholders...");
        NexusPlaceholderBootstrap.registerNexusPlaceholders(this);
        int internalPlaceholders = NexusPlaceholderRegistry.countKeys("nexus");
        nexusLogger.info("Registered <yellow>" + internalPlaceholders + "<reset> internal placeholders.");
        commandManager = new PaperCommandManager(this);
        actionFactory = new ActionFactory();
        effectFactory = new EffectFactory(PlayerVariableResolver.ofStore());
        bindingRegistry = new EffectBindingRegistry();
        registerCommands();
        nexusLogger.info("Register Nexus events...");
        registerEvents();
        nexusLogger.info("Initiate Nexus effect system ...");
        nexusLogger.info("Register built-in trigger and filter types");
        registerBuiltInTriggerAndFilterTypes();
        nexusLogger.warning("Register Nexus effect bindings from testing config. This should be removed in production!");
        NexusEffectsApi.registerBindingsFromSection(this, settingsFile.getConfig());
        logEffectSystemStats();
        registerDatabaseService();

        this.invService = new InvService(this, new DefaultNexItemRenderer(), nexusLanguage);
        invService.registerNamespace(this.getName().toLowerCase(Locale.ROOT), inventoryFiles);

        nexusLogger.info("Nexus is enabled");
    }

    public NexusLogger getNexusLogger() {
        return nexusLogger;
    }

    @Override
    public void onDisable() {
        if (nexusDatabaseService != null) {
            NexusDatabaseBukkitRegistrar.unregister(this, nexusDatabaseService);
            nexusDatabaseService = null;
        }
        // DB sauber schließen
        if (abstractDatabase != null) {
            try {
                abstractDatabase.close();
            } catch (Exception e) {
                nexusLogger.error(List.of(
                        "Error while closing database.",
                        "Error: " + e.getMessage()
                ));
            }
            abstractDatabase = null;
        }
        hikariDataSource = null;
        nexusLogger.info("Nexus has been disabled!");
    }

    public void onReload() {
        loadNexusFiles();
        messageSender = new MessageSender(nexusLanguage);
        //loadInventories();
        //PlayerVariables.set(UUID.randomUUID(), "stat-level", "50");
    }

    private void commandCompletions() {
        // Register command completions for various types
        commandManager.getCommandCompletions().registerCompletion("languages", c -> {
            List<String> completions = nexusLanguage.getAvailableLanguages().keySet().stream().toList();
            return ImmutableList.copyOf(completions);
        });
    }

    private void loadNexusFiles() {
        settingsFile = new NexusFile(this, "settings.yml", nexusLogger, false);
        // preload the default english language file.
        new NexusFile(this, "languages/english.yml", nexusLogger, true);
        nexusLogger.setDebugEnabled(settingsFile.getBoolean("logging.debug.enable", true));
        nexusLogger.setDebugLevel(settingsFile.getInt("logging.debug.level", 3));
        // initialize the NexusFileReader for languages.
        languageFiles = new NexusFileReader("languages", this);
        // Load all language files as FileConfigurations.
        nexusLanguage = new NexusLanguage(languageFiles, nexusLogger);
        inventoryFiles = new NexusFileReader("inventories", this);
        dropTableFiles = new NexusFileReader("droptables", this);
        nexusLogger.info("All Nexus files have been (re)loaded successfully.");
    }

    private void initDatabase() {
        try {
            String databaseType = settingsFile.getConfig().getString("storage.type");
            ConnectionProperties connectionProperties = ConnectionProperties.fromConfig(settingsFile.getConfig());
            DatabaseCredentials credentials = DatabaseCredentials.fromConfig(settingsFile.getConfig());

            if (databaseType == null) {
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

    private void registerDatabaseService() {
        if (this.abstractDatabase instanceof PooledDatabase pooled) {
            try {
                // DataSource referenzieren (optional lokal halten)
                this.hikariDataSource = (HikariDataSource) pooled.getDataSource();
                // Service erzeugen und registrieren
                this.nexusDatabaseService = new DefaultNexusDatabaseService(pooled.getDataSource());
                NexusDatabaseBukkitRegistrar.register(this, this.nexusDatabaseService);
                nexusLogger.info("NexusDatabaseService registered successfully.");
            } catch (Exception e) {
                nexusLogger.error(List.of(
                        "Failed to register NexusDatabaseService.",
                        "Error: " + e.getMessage()
                ));
            }
        } else {
            nexusLogger.warning("AbstractDatabase is not a PooledDatabase. NexusDatabaseService will not be registered.");
        }
    }



    private void checkForHooks() {

        if(getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiHook = new PapiHook();
            nexusLogger.info("<yellow>PlaceholderAPI<reset> hook registered successfully.");
        }
        if(getServer().getPluginManager().getPlugin("EcoSkills") != null) {
            ecoSkillsHook = new EcoSkillsHook();
            nexusLogger.info("<yellow>EcoSkills<reset> hook registered successfully.");
        }
        if(getServer().getPluginManager().getPlugin("EcoItems") != null) {
            ecoItemsHook = new EcoItemsHook();
            nexusLogger.info("<yellow>EcoItems<reset> hook registered successfully.");
        }
        if(getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            mythicMobsHook = new MythicMobsHook(Bukkit.getPluginManager());
            nexusLogger.info("<yellow>MythicMobs<reset> hook registered successfully.");
        }
        if(getServer().getPluginManager().getPlugin("MMOItems") != null) {
            mmoItemsHook = new MMOItemsHook();
            nexusLogger.info("<yellow>MMOItems<reset> hook registered successfully.");
        }

        if(getServer().getPluginManager().getPlugin("AuraSkills") != null) {
            auraSkillsHook = new AuraSkillsHook();
            nexusLogger.info("<yellow>AuraSkills<reset> hook registered successfully.");
        }

        if(getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            Bukkit.getPluginManager().registerEvents(new WorldGuardHook(), this);
            nexusLogger.info("<yellow>WorldGuard<reset> hook registered successfully.");
        }

        // Check if Vault is installed and register the hook mythicMobsHook
        if(getServer().getPluginManager().getPlugin("Vault") != null) {
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
        commandManager.registerCommand(new StatCommand());
        commandManager.registerCommand(new InvOpenCommand());
        int size = commandManager.getRegisteredRootCommands().size();
        nexusLogger.info("Successfully registered " + size  + " command(s).");
    }
    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new EntityDamageTriggerListener(bindingRegistry), this);
        Bukkit.getPluginManager().registerEvents(new NoMoreFeed(), this);
        Bukkit.getPluginManager().registerEvents(new NexInventoryClickListener(), this);

    }

    private void registerBuiltInTriggerAndFilterTypes() {
        // Trigger-Typen
        NexusEffectsApi.registerTriggerType("entity-damage");
        // NexusEffectsApi.registerTriggerType("kill-entity");
        NexusEffectsApi.registerFilterType("match-item-hand");
        NexusEffectsApi.registerFilterType("match-item-inventory");
        NexusEffectsApi.registerFilterType("has-permission");
        NexusEffectsApi.registerFilterType("in-world");
    }


    public void logEffectSystemStats() {
        int effectTypes = effectFactory != null ? effectFactory.getRegisteredEffectTypeCount() : 0;
        List<EffectBinding> bindings = bindingRegistry != null ? bindingRegistry.getBindings() : List.of();

        int bindingCount = bindings.size();
        int triggerCount = 0;
        int filterCount = 0;

        for (EffectBinding b : bindings) {
            if (b.triggers() != null) {
                triggerCount += b.triggers().size();
            }
            Object rawFilters = b.rawConfig() != null ? b.rawConfig().get("filters") : null;
            if (rawFilters instanceof List<?> l) {
                filterCount += l.size();
            }
        }

        nexusLogger.info(String.format(
                "Effect System: %d Effect(s), %d Binding(s), %d Trigger(s), %d Filter(s).",
                effectTypes, bindingCount, triggerCount, filterCount
        ));
    }

}
