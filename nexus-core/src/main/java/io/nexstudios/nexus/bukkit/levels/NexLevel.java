package io.nexstudios.nexus.bukkit.levels;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.database.api.NexusDatabaseService;
import io.nexstudios.nexus.bukkit.levels.handler.NexLevelDownEvent;
import io.nexstudios.nexus.bukkit.levels.handler.NexLevelGainXPEvent;
import io.nexstudios.nexus.bukkit.levels.handler.NexLevelUpEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Core level engine of the Nexus plugin.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Manage all level types (skills) via {@link LevelKey} and {@link LevelDefinition}</li>
 *     <li>Track per-player progress ({@link LevelProgress}) in an in-memory cache</li>
 *     <li>Persist progress (total XP + last applied level) asynchronously to the database
 *         using {@link LevelDao} and a background flush scheduler</li>
 *     <li>Recalculate player level and XP-in-level from total XP when level curves change</li>
 *     <li>Fire Bukkit events for XP and level changes:
 *         <ul>
 *             <li>{@link NexLevelGainXPEvent} – whenever XP changes</li>
 *             <li>{@link NexLevelUpEvent} – when the player gains one or more levels</li>
 *             <li>{@link NexLevelDownEvent} – when the player loses one or more levels</li>
 *         </ul>
 *     </li>
 *     <li>Support retroactive rewards when new levels are added to an existing curve:
 *         <ul>
 *             <li>Online players are updated immediately via {@link #registerLevel(String, String, java.util.List)}</li>
 *             <li>Offline players are updated lazily the next time they join,
 *                 based on their stored total XP and {@code lastAppliedLevel}</li>
 *         </ul>
 *     </li>
 * </ul>
 * <p>
 * Persistence model:
 * <ul>
 *     <li>Only {@code totalXp} and {@code lastAppliedLevel} are stored in the database</li>
 *     <li>On load, {@code totalXp} is mapped to (level, xpInLevel) using the current
 *         {@link LevelDefinition}</li>
 *     <li>{@code lastAppliedLevel} tracks up to which level rewards/actions have already been
 *         applied, so that new or changed level curves can safely trigger missing rewards
 *         without double execution</li>
 * </ul>
 * <p>
 * Threading:
 * <ul>
 *     <li>DB access and periodic flushes run on a dedicated single-threaded scheduler</li>
 *     <li>Bukkit events are always fired on the primary server thread</li>
 *     <li>All public API methods are designed to be safe to call from the main thread</li>
 * </ul>
 */
public final class NexLevel {

    public static final class FlushConfig {
        public long flushIntervalMillis = 5 * 60_000L;
        public int batchSize = 500;
        public String tableName = "nex_player_levels";
    }

    @Getter
    private static NexLevel instance;
    private final JavaPlugin plugin;
    private final NexusDatabaseService db;
    private final FlushConfig cfg;
    private final LevelRegistry registry = new LevelRegistry();
    private final ConcurrentMap<UUID, ConcurrentMap<LevelKey, LevelProgress>> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LevelProgress> dirtyIndex = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<LevelProgress> dirtyQueue = new ConcurrentLinkedQueue<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NexLevel-Flush");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> flushTask;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private final LevelDao dao;

    private NexLevel(JavaPlugin plugin, NexusDatabaseService db, FlushConfig cfg) {
        this.plugin = plugin;
        this.db = db;
        this.cfg = cfg;
        this.dao = new LevelDao(cfg.tableName);
    }

    /**
     * Initializes the global NexLevel instance and bootstraps the level system.
     * <p>
     * This method must be called once during plugin startup (typically from
     * {@code NexusPlugin.registerLevelService()}) before any other NexLevel API
     * methods are used.
     *
     * @param plugin the owning {@link JavaPlugin}
     * @param db     the shared {@link NexusDatabaseService} used for persistence
     * @param cfg    flush and schema configuration
     * @return the initialized singleton instance
     */
    public static synchronized NexLevel init(JavaPlugin plugin, NexusDatabaseService db, FlushConfig cfg) {
        if (instance != null) return instance;
        instance = new NexLevel(plugin, db, cfg);
        instance.bootstrap();
        return instance;
    }

    /**
     * Shuts down the global NexLevel instance.
     * <p>
     * Performs a final blocking flush of all dirty level data on the internal
     * scheduler thread, stops periodic flushes and releases references.
     * <p>
     * Should be called from {@code onDisable()} of the owning plugin.
     */
    public static synchronized void shutdown() {
        if (instance == null) return;
        instance.teardown();
        instance = null;
    }

    private void bootstrap() {
        try {
            db.withConnection(c -> {
                try {
                    dao.ensureSchema(c);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error("[NexLevel] Schema-Init fehlgeschlagen: " + e.getMessage());
        }

        flushTask = scheduler.scheduleAtFixedRate(this::flushOnceSafe, cfg.flushIntervalMillis, cfg.flushIntervalMillis, TimeUnit.MILLISECONDS);
    }

    private void teardown() {
        shuttingDown.set(true);
        if (flushTask != null) {
            flushTask.cancel(false);
        }

        try {
            NexusPlugin.nexusLogger.info("[NexLevel] Performing shutdown flush (full blocking flush on NexLevel scheduler) ...");

            scheduler.execute(this::flushAllBlocking);
            scheduler.shutdown();

            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                NexusPlugin.nexusLogger.warning("[NexLevel] Scheduler did not terminate within 10 seconds during shutdown. Some level data might not be flushed.");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            NexusPlugin.nexusLogger.error("[NexLevel] Shutdown interrupted while waiting for scheduler termination: " + e.getMessage());
        } catch (Throwable t) {
            NexusPlugin.nexusLogger.error("[NexLevel] Shutdown flush failed: " + t.getMessage());
            t.printStackTrace();
        }
    }



    /**
     * Registers or updates a level definition for the given (namespace, key).
     * <p>
     * The {@code neededExp} list defines the XP requirement per level
     * (not cumulative): index 0 = XP for level 1, index 1 = XP for level 2, etc.
     * <p>
     * When an existing definition is replaced:
     * <ul>
     *     <li>All cached player entries for this key are recalculated based on their total XP</li>
     *     <li>All currently online players are preloaded for this level type</li>
     *     <li>If an old definition existed, missing level-up actions for already
     *         over-capped players are fired retroactively</li>
     * </ul>
     *
     * @param namespace logical namespace of the level type (e.g. plugin name)
     * @param key       logical key of the level type (e.g. "slayer")
     * @param neededExp XP requirement per level (not cumulative), index 0 => level 1
     */
    public void registerLevel(String namespace, String key, List<Double> neededExp) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(key);
        Objects.requireNonNull(neededExp);
        LevelKey lk = new LevelKey(namespace, key);

        LevelDefinition oldDef = registry.get(lk).orElse(null);
        int oldMaxLevel = oldDef != null ? oldDef.maxLevel() : 0;

        LevelDefinition newDef = new LevelDefinition(neededExp);
        registry.register(lk, newDef);
        recalcCacheForKey(lk, newDef);
        preloadForAllPlayers(namespace, key);

        if (oldMaxLevel > 0) {
            retroactivelyFireLevelUps(lk, oldMaxLevel);
        }
    }

    private void retroactivelyFireLevelUps(LevelKey lk, int oldMaxLevel) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID pid = online.getUniqueId();

            LevelProgress p = getProgress(pid, lk.getNamespace(), lk.getKey());

            int currentLevel = p.getLevel();
            if (currentLevel <= oldMaxLevel) {
                continue;
            }

            if (Bukkit.isPrimaryThread()) {
                Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(pid, lk, oldMaxLevel, currentLevel));
            } else {
                Bukkit.getScheduler().runTask(plugin,
                        () -> Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(pid, lk, oldMaxLevel, currentLevel)));
            }
            
            if (p.getLastAppliedLevel() < currentLevel) {
                p.setLastAppliedLevel(currentLevel);
                markDirty(p);
            }

            flushPlayer(pid);
        }
    }

    private void recalcCacheForKey(LevelKey key, LevelDefinition def) {
        for (Map.Entry<UUID, ConcurrentMap<LevelKey, LevelProgress>> entry : cache.entrySet()) {
            UUID pid = entry.getKey();
            ConcurrentMap<LevelKey, LevelProgress> perPlayer = entry.getValue();
            if (perPlayer == null) continue;

            LevelProgress old = perPlayer.get(key);
            if (old == null) continue;

            double totalXp = old.getTotalXp();
            LevelProgress recalced = recalcFromTotal(pid, key, totalXp, def);

            recalced.setLastAppliedLevel(old.getLastAppliedLevel());

            perPlayer.put(key, recalced);
            markDirty(recalced);
        }
    }

    /**
     * Returns the XP requirement for the given level of the specified level type.
     * <p>
     * The passed level index is clamped to the valid range {@code [1, maxLevel]}.
     *
     * @param namespace namespace of the level type
     * @param key       key of the level type
     * @param level     target level (1-based)
     * @return the XP requirement for the given level, or {@code 0.0} if the
     *         definition is empty
     * @throws IllegalStateException if the level type is not registered
     */
    public double getRequiredXp(String namespace, String key, int level) {
        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);

        int max = def.maxLevel();
        if (max <= 0) {
            return 0.0d;
        }

        if (level < 1) {
            level = 1;
        } else if (level > max) {
            level = max;
        }

        return def.requirementFor(level);
    }

    /**
     * Preloads level progress for all registered level types for the given player.
     * <p>
     * This triggers lazy loading of {@code totalXp} (and {@code lastAppliedLevel})
     * from the database into the in-memory cache. Loading itself is performed
     * asynchronously per key.
     *
     * @param playerId the player's UUID
     */
    public void preloadAllForPlayer(UUID playerId) {
        Set<LevelKey> keys = registry.getAllKeys();
        if (keys.isEmpty()) {
            return;
        }
        for (LevelKey key : keys) {
            getProgress(playerId, key.getNamespace(), key.getKey());
        }
    }

    /**
     * Preloads the given level type for all currently online players.
     * <p>
     * For each online player, {@link #getProgress(UUID, String, String)} is called,
     * which will schedule an asynchronous DB load if no cached entry exists.
     *
     * @param namespace namespace of the level type
     * @param key       key of the level type
     */
    public void preloadForAllPlayers(String namespace, String key) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID pid = online.getUniqueId();
            getProgress(pid, namespace, key);
        }
    }

    /**
     * Returns a snapshot of all registered level keys.
     * <p>
     * Useful for diagnostics or iteration over all known level types.
     *
     * @return a set of all registered {@link LevelKey}s
     */
    public Set<LevelKey> getAllRegisteredLevelKeys() {
        return registry.getAllKeys();
    }

    /**
     * Returns the current {@link LevelProgress} for a player and level type.
     * <p>
     * Behavior:
     * <ul>
     *     <li>If a cached entry exists, it is returned immediately.</li>
     *     <li>Otherwise, a default progress (level 0, totalXp 0) is inserted into
     *         the cache and an asynchronous DB load is scheduled.</li>
     *     <li>Once the async load completes, the cached progress is recalculated
     *         from {@code totalXp} using the current {@link LevelDefinition}.</li>
     * </ul>
     *
     * @param playerId  the player's UUID
     * @param namespace namespace of the level type
     * @param key       key of the level type
     * @return the current (possibly default) {@link LevelProgress} from the cache
     * @throws IllegalStateException if the level type is not registered
     */
    public LevelProgress getProgress(UUID playerId, String namespace, String key) {
        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);

        ConcurrentMap<LevelKey, LevelProgress> map =
                cache.computeIfAbsent(playerId, __ -> new ConcurrentHashMap<>());
        LevelProgress existing = map.get(lk);
        if (existing != null) return existing;

        LevelProgress fallback = new LevelProgress(playerId, lk, 0, 0.0d, 0.0d, 0);
        LevelProgress prev = map.putIfAbsent(lk, fallback);
        if (prev != null) return prev;

        CompletableFuture.runAsync(() -> {
            try {
                db.withConnection(c -> {
                    try {
                        dao.load(c, playerId, lk).ifPresent(dbProg -> {
                            LevelProgress recalced = recalcFromTotal(
                                    playerId, lk, dbProg.getTotalXp(), def
                            );
                            recalced.setLastAppliedLevel(dbProg.getLastAppliedLevel());

                            map.compute(lk, (k2, cur) -> {
                                if (cur == null) return recalced;
                                if (cur.getLevel() == 0 && cur.getTotalXp() == 0.0d) {
                                    return recalced;
                                }
                                return cur;
                            });

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                LevelProgress current = cache
                                        .getOrDefault(playerId, new ConcurrentHashMap<>())
                                        .get(lk);
                                if (current == null) {
                                    return;
                                }

                                LevelProgress tmp = recalcFromTotal(
                                        playerId, lk, current.getTotalXp(), def
                                );
                                int theoreticalLevel = tmp.getLevel();
                                int lastApplied = current.getLastAppliedLevel();

                                if (theoreticalLevel <= lastApplied) {
                                    return;
                                }

                                Bukkit.getPluginManager().callEvent(
                                        new NexLevelUpEvent(playerId, lk, lastApplied, theoreticalLevel)
                                );

                                current.setLastAppliedLevel(theoreticalLevel);
                                markDirty(current);
                            });
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                NexusPlugin.nexusLogger.error("[NexLevel] Lazy-Load fehlgeschlagen: " + e.getMessage());
            }
        });
        return fallback;
    }

    /**
     * Adds XP to a player's level progress for the given level type.
     * <p>
     * This method:
     * <ul>
     *     <li>Updates total XP and recalculates level + XP-in-level</li>
     *     <li>Fires {@link NexLevelGainXPEvent}</li>
     *     <li>Fires {@link NexLevelUpEvent} if the level increased by one or more</li>
     *     <li>Updates {@code lastAppliedLevel} if the level increased</li>
     *     <li>Triggers an asynchronous flush for this player when the level changed</li>
     * </ul>
     *
     * @param playerId  the player's UUID
     * @param namespace namespace of the level type
     * @param key       key of the level type
     * @param deltaXp   XP to add (negative values are delegated to {@link #removeXp})
     * @return the updated {@link LevelProgress}
     */
    public LevelProgress addXp(UUID playerId, String namespace, String key, double deltaXp) {
        if (deltaXp == 0.0d) return getProgress(playerId, namespace, key);
        if (deltaXp < 0.0d) return removeXp(playerId, namespace, key, -deltaXp);

        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);

        LevelProgress current = getProgress(playerId, namespace, key);
        final double oldXp = current.getXp();
        final int oldLevel = current.getLevel();
        final double oldTotalXp = current.getTotalXp();

        double newTotal = oldTotalXp + deltaXp;
        if (newTotal < 0.0d) newTotal = 0.0d;

        LevelProgress after = recalcFromTotal(playerId, lk, newTotal, def);
        setProgressInternal(after);

        final int newLevelFinal = after.getLevel();
        final double newXpFinal = after.getXp();

        if (newLevelFinal > current.getLastAppliedLevel()) {
            after.setLastAppliedLevel(newLevelFinal);
            markDirty(after);
        }

        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(new NexLevelGainXPEvent(playerId, lk, deltaXp, oldXp, newXpFinal));
            if (newLevelFinal > oldLevel) {
                Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(playerId, lk, oldLevel, newLevelFinal));
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getPluginManager().callEvent(new NexLevelGainXPEvent(playerId, lk, deltaXp, oldXp, newXpFinal));
                if (newLevelFinal > oldLevel) {
                    Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(playerId, lk, oldLevel, newLevelFinal));
                }
            });
        }

        if (newLevelFinal != oldLevel) {
            flushPlayer(playerId);
        }

        return after;
    }

    /**
     * Removes XP from a player's level progress for the given level type.
     * <p>
     * This method recalculates level + XP-in-level from the new total XP and
     * fires {@link NexLevelDownEvent} if the level decreased by one or more.
     *
     * @param playerId  the player's UUID
     * @param namespace namespace of the level type
     * @param key       key of the level type
     * @param deltaXp   XP to remove (must be &gt; 0, values &le; 0 are ignored)
     * @return the updated {@link LevelProgress}
     */
    public LevelProgress removeXp(UUID playerId, String namespace, String key, double deltaXp) {
        if (deltaXp <= 0.0d) return getProgress(playerId, namespace, key);

        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);

        LevelProgress current = getProgress(playerId, namespace, key);
        final int oldLevel = current.getLevel();
        final double oldTotal = current.getTotalXp();

        double newTotal = oldTotal - deltaXp;
        if (newTotal < 0.0d) newTotal = 0.0d;

        LevelProgress after = recalcFromTotal(playerId, lk, newTotal, def);
        setProgressInternal(after);

        final int newLevelFinal = after.getLevel();

        if (Bukkit.isPrimaryThread()) {
            if (newLevelFinal < oldLevel) {
                Bukkit.getPluginManager().callEvent(new NexLevelDownEvent(playerId, lk, oldLevel, newLevelFinal));
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (newLevelFinal < oldLevel) {
                    Bukkit.getPluginManager().callEvent(new NexLevelDownEvent(playerId, lk, oldLevel, newLevelFinal));
                }
            });
        }

        return after;
    }

    /**
     * Changes a player's level for the given type by a relative delta.
     * <p>
     * Positive deltas increase the level, negative deltas decrease it.
     * The result is clamped to {@code [0, maxLevel]}.
     * <p>
     * On level increase, {@link NexLevelUpEvent} is fired and
     * {@code lastAppliedLevel} is updated accordingly.
     *
     * @param playerId    the player's UUID
     * @param namespace   namespace of the level type
     * @param key         key of the level type
     * @param deltaLevels relative level change (can be negative)
     * @return the updated {@link LevelProgress}
     */
    public LevelProgress addLevel(UUID playerId, String namespace, String key, int deltaLevels) {
        if (deltaLevels == 0) return getProgress(playerId, namespace, key);
        if (deltaLevels < 0) return setLevel(playerId, namespace, key, getProgress(playerId, namespace, key).getLevel() + deltaLevels);

        LevelKey lk = new LevelKey(namespace, key);
        ensureRegistered(lk);
        LevelProgress before = getProgress(playerId, namespace, key);

        final int oldLevel = before.getLevel();
        int target = before.getLevel() + deltaLevels;
        LevelProgress after = setLevel(playerId, namespace, key, target);
        final int newLevelFinal = after.getLevel();

        if (newLevelFinal > before.getLastAppliedLevel()) {
            after.setLastAppliedLevel(newLevelFinal);
            markDirty(after);
        }

        if (Bukkit.isPrimaryThread()) {
            if (newLevelFinal > oldLevel) {
                Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(playerId, lk, oldLevel, newLevelFinal));
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (newLevelFinal > oldLevel) {
                    Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(playerId, lk, oldLevel, newLevelFinal));
                }
            });
        }

        if (newLevelFinal != oldLevel) {
            flushPlayer(playerId);
        }

        return after;
    }

    /**
     * Sets a player's level for the given type to an absolute value.
     * <p>
     * The level is clamped to {@code [0, maxLevel]} and total XP is adjusted
     * to match the exact cumulative XP threshold for that level.
     * <p>
     * Fires {@link NexLevelUpEvent} or {@link NexLevelDownEvent} if the level
     * changed, updates {@code lastAppliedLevel} when the level increased,
     * and triggers an asynchronous flush for this player.
     *
     * @param playerId  the player's UUID
     * @param namespace namespace of the level type
     * @param key       key of the level type
     * @param newLevel  target absolute level
     * @return the updated {@link LevelProgress}
     */
    public LevelProgress setLevel(UUID playerId, String namespace, String key, int newLevel) {
        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);
        LevelProgress current = getProgress(playerId, namespace, key);

        final int oldLevel = current.getLevel();

        int lv = Math.max(0, Math.min(newLevel, def.maxLevel()));
        double newTotal = (lv == 0) ? 0.0d : totalXpForLevel(def, lv);

        LevelProgress after = recalcFromTotal(playerId, lk, newTotal, def);
        setProgressInternal(after);

        final int newLvFinal = after.getLevel();

        if (newLvFinal > current.getLastAppliedLevel()) {
            after.setLastAppliedLevel(newLvFinal);
            markDirty(after);
        }

        if (newLvFinal != oldLevel) {
            if (Bukkit.isPrimaryThread()) {
                if (newLvFinal > oldLevel) {
                    Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(playerId, lk, oldLevel, newLvFinal));
                } else {
                    Bukkit.getPluginManager().callEvent(new NexLevelDownEvent(playerId, lk, oldLevel, newLvFinal));
                }
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (newLvFinal > oldLevel) {
                        Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(playerId, lk, oldLevel, newLvFinal));
                    } else {
                        Bukkit.getPluginManager().callEvent(new NexLevelDownEvent(playerId, lk, oldLevel, newLvFinal));
                    }
                });
            }

            flushPlayer(playerId);
        }
        return after;
    }

    /**
     * Sets a player's total XP for the given level type and recalculates level
     * and XP-in-level.
     * <p>
     * The XP value is clamped to {@code >= 0}. If the recalculated level changes,
     * the appropriate {@link NexLevelUpEvent} or {@link NexLevelDownEvent}
     * is fired, {@code lastAppliedLevel} is updated when increasing, and an
     * asynchronous flush for this player is triggered.
     *
     * @param playerId  the player's UUID
     * @param namespace namespace of the level type
     * @param key       key of the level type
     * @param newXp     new total XP (will be clamped to {@code >= 0})
     * @return the updated {@link LevelProgress}
     */
    public LevelProgress setXp(UUID playerId, String namespace, String key, double newXp) {
        if (newXp < 0.0d) newXp = 0.0d;

        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);
        LevelProgress current = getProgress(playerId, namespace, key);
        final int oldLevel = current.getLevel();

        double newTotal = newXp;

        LevelProgress after = recalcFromTotal(playerId, lk, newTotal, def);
        setProgressInternal(after);

        final int newLevelFinal = after.getLevel();

        // lastAppliedLevel nur nach oben anpassen
        if (newLevelFinal > current.getLastAppliedLevel()) {
            after.setLastAppliedLevel(newLevelFinal);
            markDirty(after);
        }

        if (newLevelFinal != oldLevel) {
            if (Bukkit.isPrimaryThread()) {
                if (newLevelFinal > oldLevel) {
                    Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(playerId, lk, oldLevel, newLevelFinal));
                } else {
                    Bukkit.getPluginManager().callEvent(new NexLevelDownEvent(playerId, lk, oldLevel, newLevelFinal));
                }
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (newLevelFinal > oldLevel) {
                        Bukkit.getPluginManager().callEvent(new NexLevelUpEvent(playerId, lk, oldLevel, newLevelFinal));
                    } else {
                        Bukkit.getPluginManager().callEvent(new NexLevelDownEvent(playerId, lk, oldLevel, newLevelFinal));
                    }
                });
            }

            flushPlayer(playerId);
        }
        return after;
    }

    /**
     * Resets all level data for the given player across all level types.
     * <p>
     * This clears the in-memory cache and dirty entries for the player and
     * deletes all corresponding rows from the database.
     *
     * @param playerId the player's UUID
     * @return a future completing with {@code true} if any data was removed
     */
    public CompletableFuture<Boolean> resetPlayer(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> resetPlayerSync(playerId));
    }

    /**
     * Resets level data for a single level type for the given player.
     * <p>
     * This clears the cached {@link LevelProgress} for that key, removes dirty
     * entries and deletes the corresponding row from the database.
     *
     * @param playerId  the player's UUID
     * @param namespace namespace of the level type
     * @param key       key of the level type
     * @return a future completing with {@code true} if any data was removed
     */
    public CompletableFuture<Boolean> resetPlayerForType(UUID playerId, String namespace, String key) {
        return CompletableFuture.supplyAsync(() -> resetPlayerForTypeSync(playerId, namespace, key));
    }

    /**
     * Resets all level data for all players and all level types.
     * <p>
     * This clears the entire in-memory cache and dirty queues and deletes all
     * rows from the underlying level table.
     *
     * @return a future completing with {@code true} if any data was removed
     */
    public CompletableFuture<Boolean> resetAllPlayers() {
        return CompletableFuture.supplyAsync(this::resetAllPlayersSync);
    }

    /**
     * Resets level data for all players for a single level type.
     * <p>
     * This removes cached {@link LevelProgress} entries for that key, clears
     * related dirty entries and deletes the corresponding rows from the database.
     *
     * @param namespace namespace of the level type
     * @param key       key of the level type
     * @return a future completing with {@code true} if any data was removed
     */
    public CompletableFuture<Boolean> resetAllPlayersForType(String namespace, String key) {
        return CompletableFuture.supplyAsync(() -> resetAllPlayersForTypeSync(namespace, key));
    }

    private boolean resetPlayerSync(UUID playerId) {
        boolean hadCache = cache.containsKey(playerId);
        boolean hadDirty = dirtyIndex.keySet().stream().anyMatch(k -> k.startsWith(playerId.toString() + "|"));

        cache.remove(playerId);
        dirtyQueue.removeIf(p -> p.getPlayerId().equals(playerId));
        dirtyIndex.keySet().removeIf(k -> k.startsWith(playerId.toString() + "|"));

        int deletedRows = 0;
        try {
            final int[] count = {0};
            db.withConnection(c -> {
                try {
                    count[0] = dao.deleteByPlayer(c, playerId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            deletedRows = count[0];
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error("[NexLevel] resetPlayer DB delete failed: " + e.getMessage());
        }

        return hadCache || hadDirty || deletedRows > 0;
    }

    private boolean resetPlayerForTypeSync(UUID playerId, String namespace, String key) {
        LevelKey lk = new LevelKey(namespace, key);

        ConcurrentMap<LevelKey, LevelProgress> m = cache.get(playerId);
        boolean hadCache = (m != null && m.containsKey(lk));
        boolean hadDirty = dirtyIndex.containsKey(playerId.toString() + "|" + lk);

        if (m != null) {
            m.remove(lk);
            if (m.isEmpty()) {
                cache.remove(playerId, m);
            }
        }

        dirtyQueue.removeIf(p -> p.getPlayerId().equals(playerId) && p.getKey().equals(lk));
        dirtyIndex.remove(playerId + "|" + lk);

        int deletedRows = 0;
        try {
            final int[] count = {0};
            db.withConnection(c -> {
                try {
                    count[0] = dao.deleteByPlayerAndKey(c, playerId, lk);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            deletedRows = count[0];
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error("[NexLevel] resetPlayerForType DB delete failed: " + e.getMessage());
        }

        return hadCache || hadDirty || deletedRows > 0;
    }

    private boolean resetAllPlayersForTypeSync(String namespace, String key) {
        LevelKey lk = new LevelKey(namespace, key);

        boolean registered = registry.isRegistered(lk);
        boolean hadCache = cache.values().stream().anyMatch(m -> m != null && m.containsKey(lk));
        boolean hadDirty = dirtyQueue.stream().anyMatch(p -> p.getKey().equals(lk))
                || dirtyIndex.keySet().stream().anyMatch(k -> k.endsWith("|" + lk.toString()));

        for (Map.Entry<UUID, ConcurrentMap<LevelKey, LevelProgress>> entry : cache.entrySet()) {
            ConcurrentMap<LevelKey, LevelProgress> m = entry.getValue();
            if (m != null) {
                m.remove(lk);
                if (m.isEmpty()) {
                    cache.remove(entry.getKey(), m);
                }
            }
        }

        dirtyQueue.removeIf(p -> p.getKey().equals(lk));
        dirtyIndex.keySet().removeIf(k -> k.endsWith("|" + lk.toString()));

        int deletedRows = 0;
        try {
            final int[] count = {0};
            db.withConnection(c -> {
                try {
                    count[0] = dao.deleteByKey(c, lk);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            deletedRows = count[0];
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error("[NexLevel] resetAllPlayersForType DB delete failed: " + e.getMessage());
        }

        return registered || hadCache || hadDirty || deletedRows > 0;
    }

    private boolean resetAllPlayersSync() {
        boolean hadCache = !cache.isEmpty();
        boolean hadDirty = !dirtyQueue.isEmpty() || !dirtyIndex.isEmpty();

        cache.clear();
        dirtyQueue.clear();
        dirtyIndex.clear();

        int deletedRows = 0;
        try {
            final int[] count = {0};
            db.withConnection(c -> {
                try {
                    count[0] = dao.deleteAll(c);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            deletedRows = count[0];
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error("[NexLevel] resetAllPlayers DB delete failed: " + e.getMessage());
        }

        return hadCache || hadDirty || deletedRows > 0;
    }

    /**
     * Flushes all dirty level entries for the given player to the database.
     * <p>
     * This collects the latest dirty entries for the player from the internal
     * queues and schedules an asynchronous batch upsert on the NexLevel
     * scheduler thread.
     *
     * @param playerId the player's UUID
     */
    public void flushPlayer(UUID playerId) {
        List<LevelProgress> list = new ArrayList<>();
        dirtyQueue.removeIf(p -> {
            if (p.getPlayerId().equals(playerId)) {
                String idx = indexKey(p);
                // IMMER den aktuellsten Stand aus dirtyIndex holen,
                // nicht den alten Wert aus der Queue selbst verwenden.
                LevelProgress cur = dirtyIndex.remove(idx);
                if (cur != null) {
                    list.add(cur);
                }
                return true;
            }
            return false;
        });

        if (list.isEmpty()) {
            return;
        }

        scheduler.execute(() -> {
            try {
                flushBatch(list);
            } catch (Throwable ex) {
                NexusPlugin.nexusLogger.error("[NexLevel] FlushPlayer failed: " + ex.getMessage());
                ex.printStackTrace();
                if (!shuttingDown.get()) {
                    list.forEach(this::markDirty);
                } else {
                    NexusPlugin.nexusLogger.error("[NexLevel] Shutdown in progress, will NOT requeue dirty entries for player " + playerId);
                }
            }
        });
    }

    /**
     * Schedules a full flush of all dirty level entries to the database.
     * <p>
     * The flush runs on the NexLevel scheduler thread and processes the dirty
     * queue in batches until it is empty.
     */
    public void flushAll() {
        scheduler.execute(this::flushAllBlocking);
    }

    /**
     * Invalidates the cached progress and dirty state for a single player
     * and level type.
     * <p>
     * After calling this, subsequent calls to {@link #getProgress(UUID, String, String)}
     * will behave as if the player had no cached data for this key and may
     * trigger a fresh async DB load.
     *
     * @param playerId  the player's UUID
     * @param namespace namespace of the level type
     * @param key       key of the level type
     */
    public void invalidate(UUID playerId, String namespace, String key) {
        LevelKey lk = new LevelKey(namespace, key);
        ConcurrentMap<LevelKey, LevelProgress> m = cache.get(playerId);
        if (m != null) m.remove(lk);
        dirtyQueue.removeIf(p -> p.getPlayerId().equals(playerId) && p.getKey().equals(lk));
        dirtyIndex.remove(playerId.toString() + "|" + lk);
    }

    /**
     * Calculates the theoretical level a player could have for the given type
     * based solely on their current total XP and the current level definition.
     * <p>
     * This does not change any stored state or fire events; it is a pure read
     * operation based on cached data and the definition.
     *
     * @param playerId  the player's UUID
     * @param namespace namespace of the level type
     * @param key       key of the level type
     * @return the level implied by the player's current total XP
     * @throws IllegalStateException if the level type is not registered
     */
    public int getPotentialLevel(UUID playerId, String namespace, String key) {
        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);
        LevelProgress current = getProgress(playerId, namespace, key);
        LevelProgress recalced = recalcFromTotal(playerId, lk, current.getTotalXp(), def);

        return recalced.getLevel();
    }

    /**
     * Applies any missing level-up actions for all registered level types
     * for the given player.
     * <p>
     * For each level type:
     * <ul>
     *     <li>Computes the theoretical level from {@code totalXp}</li>
     *     <li>Compares it with {@code lastAppliedLevel}</li>
     *     <li>If {@code theoreticalLevel > lastAppliedLevel}, fires a single
     *         {@link NexLevelUpEvent} from {@code lastAppliedLevel} to
     *         {@code theoreticalLevel}</li>
     *     <li>Updates {@code lastAppliedLevel} and marks the progress as dirty</li>
     * </ul>
     * If at least one type was updated, an asynchronous flush for this player
     * is scheduled.
     *
     * @param playerId the player's UUID
     */
    public void applyMissingLevelUps(UUID playerId) {
        Set<LevelKey> keys = registry.getAllKeys();
        if (keys.isEmpty()) {
            return;
        }

        boolean changedAny = false;

        for (LevelKey key : keys) {
            LevelProgress progress = getProgress(playerId, key.getNamespace(), key.getKey());
            LevelDefinition def = ensureRegistered(key);

            LevelProgress recalced = recalcFromTotal(
                    playerId,
                    key,
                    progress.getTotalXp(),
                    def
            );
            int theoreticalLevel = recalced.getLevel();

            int lastApplied = progress.getLastAppliedLevel();
            if (theoreticalLevel <= lastApplied) {
                continue;
            }

            if (Bukkit.isPrimaryThread()) {
                Bukkit.getPluginManager().callEvent(
                        new NexLevelUpEvent(playerId, key, lastApplied, theoreticalLevel)
                );
            } else {
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.getPluginManager().callEvent(
                                new NexLevelUpEvent(playerId, key, lastApplied, theoreticalLevel)
                        )
                );
            }

            progress.setLastAppliedLevel(theoreticalLevel);
            markDirty(progress);
            changedAny = true;
        }

        if (changedAny) {
            flushPlayer(playerId);
        }
    }

    private LevelDefinition ensureRegistered(LevelKey lk) {
        return registry.get(lk).orElseThrow(() ->
                new IllegalStateException("LevelType not registered: " + lk));
    }

    private double totalXpForLevel(LevelDefinition def, int level) {
        double sum = 0.0d;
        for (int i = 1; i <= level; i++) {
            sum += def.requirementFor(i);
        }
        return sum;
    }

    private LevelProgress recalcFromTotal(UUID playerId, LevelKey key, double totalXp, LevelDefinition def) {
        if (totalXp < 0.0d) {
            totalXp = 0.0d;
        }

        List<Double> perLevelList = def.neededExp();
        int max = perLevelList.size();

        double[] thresholds = new double[max];
        double sum = 0.0d;
        for (int i = 0; i < max; i++) {
            sum += perLevelList.get(i);
            thresholds[i] = sum;
        }

        int level = 0;

        for (int i = 0; i < max; i++) {
            if (totalXp + 1e-9 >= thresholds[i]) {
                level = i + 1;
            } else {
                break;
            }
        }

        double xpInLevel;
        if (level == 0) {
            xpInLevel = totalXp;
        } else {
            double thresholdForLevel = thresholds[level - 1];
            xpInLevel = totalXp - thresholdForLevel;
        }

        return new LevelProgress(playerId, key, level, xpInLevel, totalXp, 0);
    }

    private void setProgressInternal(LevelProgress updated) {
        cache.compute(updated.getPlayerId(), (pid, map) -> {
            if (map == null) map = new ConcurrentHashMap<>();
            map.put(updated.getKey(), updated);
            return map;
        });

        markDirty(updated);
    }

    private String indexKey(LevelProgress p) {
        return p.getPlayerId() + "|" + p.getKey();
    }

    private void markDirty(LevelProgress p) {
        String idx = indexKey(p);
        LevelProgress prev = dirtyIndex.put(idx, p);
        if (prev == null) {
            dirtyQueue.add(p);
        }
    }

    private void flushOnceSafe() {
        try {
            List<LevelProgress> batch = new ArrayList<>(cfg.batchSize);
            for (int i = 0; i < cfg.batchSize; i++) {
                LevelProgress p = dirtyQueue.poll();
                if (p == null) break;
                String k = indexKey(p);
                LevelProgress cur = dirtyIndex.remove(k);
                if (cur != null) {
                    batch.add(cur);
                }
            }
            if (batch.isEmpty()) return;
            flushBatch(batch);
        } catch (Throwable t) {
            NexusPlugin.nexusLogger.error("[NexLevel] Flush failed: " + t.getMessage());
        }
    }

    private void flushAllBlocking() {
        try {
            while (true) {
                List<LevelProgress> batch = new ArrayList<>(cfg.batchSize);
                for (int i = 0; i < cfg.batchSize; i++) {
                    LevelProgress p = dirtyQueue.poll();
                    if (p == null) break;
                    String k = indexKey(p);
                    LevelProgress cur = dirtyIndex.remove(k);
                    if (cur != null) {
                        batch.add(cur);
                    }
                }
                if (batch.isEmpty()) {
                    if (dirtyQueue.isEmpty()) break;
                    else continue;
                }
                flushBatch(batch);
            }
        } catch (Throwable t) {
            NexusPlugin.nexusLogger.error("[NexLevel] Complete Flush failed on shutdown: " + t.getMessage());
        }
    }

    /**
     * Starts an asynchronous preload of all level records from the database
     * into the in-memory cache.
     * <p>
     * Useful during plugin startup to warm the cache without blocking
     * the main server thread.
     */
    public void preloadAllAsync() {
        CompletableFuture.runAsync(this::preloadAllBlockingSafe);
    }

    /**
     * Performs a synchronous preload of all level records from the database
     * into the in-memory cache.
     * <p>
     * This call blocks until the preload has finished and should therefore
     * only be used during controlled startup phases, never from performance
     * critical paths on the main server thread.
     */
    public void preloadAllSync() {
        preloadAllBlockingSafe();
    }


    private void preloadAllBlockingSafe() {
        long start = System.currentTimeMillis();
        int[] count = {0};
        try {
            db.withConnection(c -> {
                try {
                    dao.streamAll(c, lp -> {
                        LevelDefinition def = registry.get(lp.getKey()).orElse(null);
                        LevelProgress toCache = lp;
                        if (def != null) {
                            toCache = recalcFromTotal(lp.getPlayerId(), lp.getKey(), lp.getTotalXp(), def);
                        }
                        cache.computeIfAbsent(toCache.getPlayerId(), __ -> new ConcurrentHashMap<>())
                                .put(toCache.getKey(), toCache);
                        count[0]++;
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            NexusPlugin.nexusLogger.info("NexLevel system preloaded in " + (System.currentTimeMillis() - start) + "ms");
            NexusPlugin.nexusLogger.info("Found <yellow>" + count[0] + "<reset> LevelProgress entries");
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error("NexLevel system preload failed: " + e.getMessage());
        }
    }

    private void flushBatch(List<LevelProgress> batch) {
        try {
            // WICHTIG: keine verschachtelten Pool-Zugriffe mehr (inTransaction + withConnection),
            // sondern genau EINE Connection holen und auf dieser Connection die Transaktion fahren.
            db.withConnection(c -> {
                boolean oldAutoCommit = true;
                try {
                    oldAutoCommit = c.getAutoCommit();
                    c.setAutoCommit(false);

                    dao.upsertBatch(c, batch);

                    c.commit();
                } catch (Exception e) {
                    try {
                        c.rollback();
                    } catch (Exception rollbackEx) {
                        NexusPlugin.nexusLogger.error("[NexLevel] Rollback after flushBatch failed: " + rollbackEx.getMessage());
                        rollbackEx.printStackTrace();
                    }
                    throw new RuntimeException(e);
                } finally {
                    try {
                        c.setAutoCommit(oldAutoCommit);
                    } catch (Exception ignored) {
                    }
                }
            });
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error("[NexLevel] Batch-Flush failed: " + e.getMessage());
            e.printStackTrace();
            if (!shuttingDown.get()) {
                NexusPlugin.nexusLogger.warning("[NexLevel] Requeue dirty entries for next flush attempt.");
                batch.forEach(this::markDirty);
            } else {
                NexusPlugin.nexusLogger.warning("[NexLevel] Shutdown in progress, will NOT requeue dirty entries.");
            }
        }
    }
}