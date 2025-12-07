package io.nexstudios.nexus.bukkit;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariDataSource;
import io.nexstudios.nexus.bukkit.actions.ActionFactory;
import io.nexstudios.nexus.bukkit.command.*;
import io.nexstudios.nexus.bukkit.conditions.ConditionFactory;
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
import io.nexstudios.nexus.bukkit.fakebreak.FakeBreakAllBlocksListener;
import io.nexstudios.nexus.bukkit.handler.MessageSender;
import io.nexstudios.nexus.bukkit.hologram.NexHoloService;
import io.nexstudios.nexus.bukkit.hooks.*;
import io.nexstudios.nexus.bukkit.hooks.auraskills.AuraSkillsHook;
import io.nexstudios.nexus.bukkit.hooks.auroracollections.AuroraCollectionsHook;
import io.nexstudios.nexus.bukkit.hooks.itemsadder.ItemsAdderHook;
import io.nexstudios.nexus.bukkit.hooks.luckperms.LuckPermsHook;
import io.nexstudios.nexus.bukkit.hooks.mythicmobs.MythicMobsHook;
import io.nexstudios.nexus.bukkit.hooks.worldguard.WorldGuardHook;
import io.nexstudios.nexus.bukkit.indicator.DamageIndicator;
import io.nexstudios.nexus.bukkit.inv.api.InvService;
import io.nexstudios.nexus.bukkit.inv.event.NexInventoryClickListener;
import io.nexstudios.nexus.bukkit.inv.renderer.DefaultNexItemRenderer;
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
import io.nexstudios.nexus.bukkit.levels.events.LevelFlushListener;
import io.nexstudios.nexus.bukkit.levels.handler.LevelCheckOnJoinListener;
import io.nexstudios.nexus.bukkit.levels.handler.LevelPreloadJoinListener;
import io.nexstudios.nexus.bukkit.levels.impl.DefaultLevelService;
import io.nexstudios.nexus.bukkit.levels.LevelService;
import io.nexstudios.nexus.bukkit.levels.NexLevel;
import io.nexstudios.nexus.bukkit.levels.impl.LevelActionListener;
import io.nexstudios.nexus.bukkit.placeholder.NexusPlaceholderBootstrap;
import io.nexstudios.nexus.bukkit.placeholder.NexusPlaceholderRegistry;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import io.nexstudios.nexus.bukkit.player.events.NoMoreFeed;
import io.nexstudios.nexus.bukkit.redis.JedisNexusRedisService;
import io.nexstudios.nexus.bukkit.redis.NexusRedisBukkitRegistrar;
import io.nexstudios.nexus.bukkit.redis.NexusRedisService;
import io.nexstudios.nexus.bukkit.utils.BlockUtil;
import io.nexstudios.nexus.bukkit.utils.NexusLogger;
import io.nexstudios.nexus.bukkit.files.NexusFile;
import io.nexstudios.nexus.bukkit.files.NexusFileReader;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Locale;

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
    private DamageIndicator damageIndicator;
    private InvService invService;
    @Getter
    public BlockUtil blockUtil;

    // API Services
    public MessageSender messageSender;
    public ActionFactory actionFactory;
    public ConditionFactory conditionFactory;
    public LevelService levelService;
    public NexHoloService nexHoloService;

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
    public AuroraCollectionsHook auroraCollectionsHook;
    public WorldGuardHook worldGuardHook;
    public ItemsAdderHook itemsAdderHook;
    public LuckPermsHook luckPermsHook;

    // Database related fields
    private AbstractDatabase abstractDatabase;
    public HikariDataSource hikariDataSource;
    private NexusDatabaseService nexusDatabaseService;
    private NexusRedisService redisService;

    @Override
    public void onLoad() {
        instance = this;
        NexServices.init(this);
        nexusLogger = new NexusLogger("<reset>[<yellow>Nexus<reset>]", true, 99, "<yellow>");
        nexusLogger.info("Nexus is loading...");
        onReload();
        checkCrossServerConfig();
        nexusLogger.info("Check for compatibility with other plugins ...");
        if(getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook();
            nexusLogger.info("<yellow>WorldGuard<reset> flag(s) registered successfully.");
        }

        nexusLogger.info("Initializing database connection ...");
        initRedis();
        initDatabase();
        nexusLogger.info("Loading phase completed, waiting for enable phase...");
    }

    @Override
    public void onEnable() {
        logAsciiLogo();
        nexusLogger.info("Checking for third party hooks ...");
        checkForHooks();
        nexusLogger.info("Register internal and third party placeholders...");
        NexusPlaceholderBootstrap.registerNexusPlaceholders(this);
        int internalPlaceholders = NexusPlaceholderRegistry.countKeys("nexus");
        nexusLogger.info("Registered <yellow>" + internalPlaceholders + "<reset> internal nexus placeholders.");
        commandManager = new PaperCommandManager(this);
        actionFactory = new ActionFactory();
        conditionFactory = new ConditionFactory();
        effectFactory = new EffectFactory(PlayerVariableResolver.ofStore());
        damageIndicator = new DamageIndicator(this, new EcoSkillsHook());
        bindingRegistry = new EffectBindingRegistry();
        blockUtil = new BlockUtil(this);
        nexusLogger.info("Register Nexus commands ...");
        registerCommands();
        nexusLogger.info("Register Nexus events ...");
        registerEvents();
        nexusLogger.info("Initiate Nexus effect system ...");
        nexusLogger.info("Register built-in trigger and filter types");
        registerBuiltInTriggerAndFilterTypes();
        //nexusLogger.warning("Register Nexus effect bindings from testing config. This should be removed in production!");
        //NexusEffectsApi.registerBindingsFromSection(this, settingsFile.getConfig());
        logEffectSystemStats();
        registerDatabaseService();

        this.invService = new InvService(this, new DefaultNexItemRenderer(), nexusLanguage);
        invService.registerNamespace(this.getName().toLowerCase(Locale.ROOT), inventoryFiles);
        nexusLogger.info("Initiate Nexus level system ...");
        registerLevelService();

        if (levelService != null) {
            try {
                nexusLogger.info("NexLevel preload started (sync).");
                levelService.preloadAllSync();
            } catch (Throwable t) {
                nexusLogger.warning("Could not start NexLevel preload: " + t.getMessage());
            }
        }

        nexHoloService = new NexHoloService(this);
        nexusLogger.info("Nexus successfully enabled");
    }

    public void registerLevelService() {
        try {
            var dbService = this.nexusDatabaseService;
            if (dbService != null) {
                NexLevel.FlushConfig cfg = new NexLevel.FlushConfig();
                cfg.flushIntervalMillis = 300_000L; // 5 Minuten
                cfg.batchSize = 500;
                cfg.tableName = "nex_player_levels";
                NexLevel.init(this, dbService, cfg);

                this.levelService = new DefaultLevelService(NexLevel.getInstance());
                nexusLogger.info("LevelService initialized successfully.");
            } else {
                nexusLogger.error(List.of(
                        "NexusDatabaseService not available. LevelService not initialized."
                ));
            }
        } catch (Exception ex) {
            nexusLogger.error(List.of("Failed to initialize LevelService.", "Error: " + ex.getMessage()));
        }

    }

    public NexusLogger getNexusLogger() {
        return nexusLogger;
    }

    @Override
    public void onDisable() {

        nexusLogger.info("Write last backups in database ...");
        try {
            nexusLogger.info("Saving NexLevel data ...");
            NexLevel.shutdown();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // Shutdown Redis service
        if (redisService != null) {
            nexusLogger.info("Shutting down Redis connection ...");
            try {
                NexusRedisBukkitRegistrar.unregister(this, redisService);
                redisService.shutdown();
            } catch (Throwable t) {
                nexusLogger.error(List.of(
                        "Error while shutting down NexusRedisService.",
                        "Error: " + t.getMessage()
                ));
            }
            redisService = null;
        }

        nexusLogger.info("Shutting down Database connection ...");
        if (nexusDatabaseService != null) {
            NexusDatabaseBukkitRegistrar.unregister(this, nexusDatabaseService);
            nexusDatabaseService = null;
        }

        if (damageIndicator != null) {
            damageIndicator.shutdown();
        }

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
        if (damageIndicator != null) {
            damageIndicator.reloadFromConfig();
        }
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
                this.hikariDataSource = (HikariDataSource) pooled.getDataSource();
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
            worldGuardHook.registerEvents();
            nexusLogger.info("<yellow>WorldGuard<reset> hook registered successfully.");
        }

        if(getServer().getPluginManager().getPlugin("AuroraCollections") != null) {
            auroraCollectionsHook = new AuroraCollectionsHook();
            nexusLogger.info("<yellow>AuroraCollections<reset> hook registered successfully.");
        }

        if(getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            itemsAdderHook = new ItemsAdderHook(this);
            nexusLogger.info("<yellow>ItemsAdder<reset> hook registered successfully.");
        }

        if(getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultHook = new VaultHook(this, nexusLogger);
        }

        if(getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsHook = new LuckPermsHook();
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
        int size = commandManager.getRegisteredRootCommands().size();
        nexusLogger.info("Successfully registered " + size  + " command(s).");
    }
    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new EntityDamageTriggerListener(bindingRegistry), this);
        Bukkit.getPluginManager().registerEvents(new NoMoreFeed(), this);
        Bukkit.getPluginManager().registerEvents(new NexInventoryClickListener(), this);
        Bukkit.getPluginManager().registerEvents(new LevelFlushListener(), this);
        Bukkit.getPluginManager().registerEvents(blockUtil, this);
        Bukkit.getPluginManager().registerEvents(new FakeBreakAllBlocksListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LevelCheckOnJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new LevelActionListener(), this);
        Bukkit.getPluginManager().registerEvents(new LevelPreloadJoinListener(this), this);

    }

    private void registerBuiltInTriggerAndFilterTypes() {
        // Trigger-Types
        NexusEffectsApi.registerTriggerType("entity-damage");
        //NexusEffectsApi.registerTriggerType("kill-entity");
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

    private void initRedis() {
        try {
            var cfg = settingsFile.getConfig();
            if(cfg.getBoolean("redis.enable", false)) {
                String host = cfg.getString("redis.host", "localhost");
                int port = cfg.getInt("redis.port", 6379);
                String password = cfg.getString("redis.password", "");
                int database = cfg.getInt("storage.redis.database", 0);

                this.redisService = new JedisNexusRedisService(this, host, port, password, database);
                ((JedisNexusRedisService) this.redisService).start();

                if (!redisService.isConnected()) {
                    if(isCrossServerEnabled()) {
                        nexusLogger.warning(List.of(
                                "Cross server communication is enabled, but Redis is required for cross server communication.",
                                "Please ensure Redis is properly configured and running.",
                                "If Redis is not required, disable cross server in your configuration."
                        ));
                        return;
                    }
                    nexusLogger.error(List.of(
                            "Redis connection could not be established.",
                            "Please check your redis.* configuration.",
                            "Error: Could not connect to Redis at " + host + ":" + port + "."
                    ));
                    return;
                }

                NexusRedisBukkitRegistrar.register(this, redisService);
                nexusLogger.info("NexusRedisService registered successfully and connected to Redis at " + host + ":" + port);
            } else {
                nexusLogger.info("Redis is currently not enabled.");
                nexusLogger.info("If you want <yellow>cross server communication<reset>, enable Redis in your configuration.");
            }
        } catch (Throwable t) {
            nexusLogger.error(List.of(
                    "Failed to initialize NexusRedisService.",
                    "Error: " + t.getMessage()
            ));
        }
    }

    public boolean isCrossServerEnabled() {
        return settingsFile.getBoolean("cross-server.enable", false);
    }

    public String getCrossServerName() {
        return settingsFile.getString("cross-server.server-name", "default");
    }

    private void checkCrossServerConfig() {
        if(isCrossServerEnabled()) {
            if(getCrossServerName().equalsIgnoreCase("default")) {
                nexusLogger.error(List.of(
                        "Cross server is enabled, but server name 'default' is specified in the configuration.",
                        "Please specify a server name in the configuration wich is also set in your Velocity config",
                        "Disabling cross server functionality."
                ));
                settingsFile.getConfig().set("cross-server.enable", false);
                settingsFile.saveConfig();
            }
        }
    }

    private void logAsciiLogo() {
        String version = getPluginMeta().getVersion();
        String paperVersion = Bukkit.getServer().getVersion();
        nexusLogger.info("""
                     
                     <gradient:yellow:white>▄▄▄    ▄▄▄  ▄▄▄▄▄▄▄ ▄▄▄   ▄▄▄ ▄▄▄  ▄▄▄  ▄▄▄▄▄▄▄</gradient>\s
                     <gradient:yellow:white>████▄  ███ ███▀▀▀▀▀ ████▄████ ███  ███ █████▀▀▀</gradient>\s
                     <gradient:yellow:white>███▀██▄███ ███▄▄     ▀█████▀  ███  ███  ▀████▄ </gradient>\s
                     <gradient:yellow:white>███  ▀████ ███      ▄███████▄ ███▄▄███    ▀████</gradient>\s
                     <gradient:yellow:white>███    ███ ▀███████ ███▀ ▀███ ▀██████▀ ███████▀</gradient>\s
            
               <gray>The most powerful core plugin that drives the entire <yellow>Nex</yellow><gray> series.</gray>
               <gray>Version: <yellow>%s</yellow></gray><reset>
               <gray>Author: <yellow>light (NexStudios)
               <gray>Paper Version: %s</gray><reset>
            """.formatted(version, paperVersion));
    }
}
