package io.nexstudios.nexus.bukkit.levels;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.database.api.NexusDatabaseService;
import io.nexstudios.nexus.bukkit.levels.handler.NexLevelDownEvent;
import io.nexstudios.nexus.bukkit.levels.handler.NexLevelGainXPEvent;
import io.nexstudios.nexus.bukkit.levels.handler.NexLevelUpEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NexLevel {

    public static final class FlushConfig {
        public long flushIntervalMillis = 5 * 60_000L; // 5 Minuten
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

    public static synchronized NexLevel init(JavaPlugin plugin, NexusDatabaseService db, FlushConfig cfg) {
        if (instance != null) return instance;
        instance = new NexLevel(plugin, db, cfg);
        instance.bootstrap();
        return instance;
    }

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
            plugin.getLogger().severe("[NexLevel] Schema-Init fehlgeschlagen: " + e.getMessage());
            // hart loggen, weiterlaufen (Cache-only) möglich
        }

        flushTask = scheduler.scheduleAtFixedRate(this::flushOnceSafe, cfg.flushIntervalMillis, cfg.flushIntervalMillis, TimeUnit.MILLISECONDS);
    }

    private void teardown() {
        shuttingDown.set(true);
        if (flushTask != null) {
            flushTask.cancel(false);
        }

        try {
            plugin.getLogger().info("[NexLevel] Performing shutdown flush (full blocking flush on NexLevel scheduler) ...");

            // Den vollständigen Flush auf dem eigenen Scheduler-Thread ausführen.
            scheduler.execute(this::flushAllBlocking);

            // Keine neuen Tasks mehr annehmen
            scheduler.shutdown();

            // Optional: Auf Abschluss warten (Timeout, damit Server nicht ewig hängt)
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("[NexLevel] Scheduler did not terminate within 10 seconds during shutdown. Some level data might not be flushed.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().severe("[NexLevel] Shutdown interrupted while waiting for scheduler termination: " + e.getMessage());
        } catch (Throwable t) {
            plugin.getLogger().severe("[NexLevel] Shutdown flush failed: " + t.getMessage());
            t.printStackTrace();
        }
    }



    // Public API

    public void registerLevel(String namespace, String key, List<Double> neededExp) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(key);
        Objects.requireNonNull(neededExp);
        LevelKey lk = new LevelKey(namespace, key);
        registry.register(lk, new LevelDefinition(neededExp));
    }

    public LevelProgress getProgress(UUID playerId, String namespace, String key) {
        LevelKey lk = new LevelKey(namespace, key);
        ensureRegistered(lk);
        ConcurrentMap<LevelKey, LevelProgress> map = cache.computeIfAbsent(playerId, __ -> new ConcurrentHashMap<>());
        LevelProgress existing = map.get(lk);
        if (existing != null) return existing;

        // Sofort Default + async lazy-load
        LevelProgress fallback = new LevelProgress(playerId, lk, 0, 0.0d);
        LevelProgress prev = map.putIfAbsent(lk, fallback);
        if (prev != null) return prev;

        // Lazy-Load
        CompletableFuture.runAsync(() -> {
            try {
                db.withConnection(c -> {
                    try {
                        dao.load(c, playerId, lk).ifPresent(dbProg -> {
                            // Nur überschreiben, wenn noch unverändert oder 0/0
                            map.compute(lk, (k, cur) -> {
                                if (cur == null) return dbProg;
                                if (cur.getLevel() == 0 && cur.getXp() == 0.0d) {
                                    return dbProg;
                                }
                                return cur; // bereits verändert -> nicht überschreiben
                            });
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("[NexLevel] Lazy-Load fehlgeschlagen: " + e.getMessage());
            }
        });
        return fallback;
    }

    public LevelProgress addXp(UUID playerId, String namespace, String key, double deltaXp) {
        if (deltaXp == 0.0d) return getProgress(playerId, namespace, key);
        if (deltaXp < 0.0d) return removeXp(playerId, namespace, key, -deltaXp);

        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);

        LevelProgress current = getProgress(playerId, namespace, key);
        final double oldXp = current.getXp();
        final int oldLevel = current.getLevel();

        double xp = current.getXp() + deltaXp;
        int level = current.getLevel();

        int max = def.maxLevel();
        while (true) {
            if (level >= max) {
                // Cap: am Max-Level bleiben, XP kappen auf Bedarf des Max-Levels
                xp = Math.max(0.0d, Math.min(xp, def.requirementFor(max)));
                break;
            }
            double reqNext = def.requirementFor(level + 1);
            if (xp + 1e-9 >= reqNext) {
                xp -= reqNext;
                level += 1;
            } else {
                break;
            }
        }

        setProgressInternal(current, level, xp);

        // Werte für Events finalisieren
        final int newLevelFinal = level;
        final double newXpFinal = xp;

        // Events synchron auf Main-Thread
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

        return getProgress(playerId, namespace, key);
    }

    public LevelProgress removeXp(UUID playerId, String namespace, String key, double deltaXp) {
        if (deltaXp <= 0.0d) return getProgress(playerId, namespace, key);

        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);

        LevelProgress current = getProgress(playerId, namespace, key);
        final int oldLevel = current.getLevel();

        double xp = current.getXp();
        int level = current.getLevel();

        double toRemove = deltaXp;
        while (toRemove > 0.0d) {
            if (level <= 0) {
                // Am Boden: Level 0, XP 0
                xp = 0.0d;
                break;
            }

            // erst XP im aktuellen Level verbrauchen
            double used = Math.min(xp, toRemove);
            xp -= used;
            toRemove -= used;
            if (toRemove <= 0.0d) break;

            // noch abzuziehen -> Level-Down
            int prevLevel = level - 1;
            if (prevLevel <= 0) {
                level = 0;
                xp = 0.0d;
            } else {
                level = prevLevel;
                xp = def.requirementFor(prevLevel); // volle "Balken" beim Rückschritt
            }
        }

        if (level < 0) level = 0;
        if (xp < 0.0d) xp = 0.0d;

        setProgressInternal(current, level, xp);

        final int newLevelFinal = level;

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

        return getProgress(playerId, namespace, key);
    }

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
        return after;
    }

    public LevelProgress setLevel(UUID playerId, String namespace, String key, int newLevel) {
        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);
        LevelProgress current = getProgress(playerId, namespace, key);

        final int oldLevel = current.getLevel();

        // Clamp auf [0, max]
        int lv = Math.max(0, Math.min(newLevel, def.maxLevel()));
        double xp = current.getXp();

        // XP clamp relativ zum neuen Level
        if (lv >= def.maxLevel()) {
            xp = Math.min(xp, def.requirementFor(def.maxLevel()));
        } else if (lv == 0) {
            xp = 0.0d;
        } else {
            double req = def.requirementFor(lv + 1);
            xp = Math.min(Math.max(xp, 0.0d), req);
        }

        setProgressInternal(current, lv, xp);

        final int newLvFinal = lv;

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
        }
        return getProgress(playerId, namespace, key);
    }

    public LevelProgress setXp(UUID playerId, String namespace, String key, double newXp) {
        if (newXp < 0.0d) newXp = 0.0d;

        LevelKey lk = new LevelKey(namespace, key);
        LevelDefinition def = ensureRegistered(lk);
        LevelProgress current = getProgress(playerId, namespace, key);
        final int oldLevel = current.getLevel();

        double xp = newXp;
        int level = current.getLevel();

        int max = def.maxLevel();
        while (true) {
            if (level >= max) {
                xp = Math.max(0.0d, Math.min(xp, def.requirementFor(max)));
                break;
            }
            double reqNext = def.requirementFor(level + 1);
            if (xp + 1e-9 >= reqNext) {
                xp -= reqNext;
                level += 1;
            } else {
                break;
            }
        }

        setProgressInternal(current, level, xp);

        final int newLevelFinal = level;

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
        }
        return getProgress(playerId, namespace, key);
    }

    public void flushPlayer(UUID playerId) {
        List<LevelProgress> list = new ArrayList<>();
        dirtyQueue.removeIf(p -> {
            if (p.getPlayerId().equals(playerId)) {
                list.add(p);
                dirtyIndex.remove(indexKey(p), p);
                return true;
            }
            return false;
        });

        if (list.isEmpty()) {
            plugin.getLogger().info("[NexLevel] flushPlayer(" + playerId + "): no dirty entries found.");
            return;
        }

        plugin.getLogger().info("[NexLevel] flushPlayer(" + playerId + "): flushing " + list.size() + " entries...");

        // NICHT mehr über CompletableFuture.runAsync auf dem CommonPool gehen,
        // sondern über den eigenen NexLevel-Scheduler, damit der Lebenszyklus
        // kontrollierbar ist und Shutdown darauf warten kann.
        scheduler.execute(() -> {
            try {
                flushBatch(list);
            } catch (Throwable ex) {
                plugin.getLogger().severe("[NexLevel] FlushPlayer fehlgeschlagen: " + ex.getMessage());
                ex.printStackTrace();
                // Im Normalbetrieb wieder in die Dirty-Queue einreihen,
                // damit der nächste Flush-Versuch es erneut probiert.
                if (!shuttingDown.get()) {
                    list.forEach(this::markDirty);
                } else {
                    plugin.getLogger().severe("[NexLevel] Shutdown in progress, will NOT requeue dirty entries for player " + playerId);
                }
            }
        });
    }

    public void flushAll() {
        // Vollständigen Flush der gesamten Dirty-Queue auf dem NexLevel-Scheduler.
        scheduler.execute(this::flushAllBlocking);
    }

    public void invalidate(UUID playerId, String namespace, String key) {
        LevelKey lk = new LevelKey(namespace, key);
        ConcurrentMap<LevelKey, LevelProgress> m = cache.get(playerId);
        if (m != null) m.remove(lk);
        dirtyQueue.removeIf(p -> p.getPlayerId().equals(playerId) && p.getKey().equals(lk));
        dirtyIndex.remove(playerId.toString() + "|" + lk);
    }

    // Intern

    private LevelDefinition ensureRegistered(LevelKey lk) {
        return registry.get(lk).orElseThrow(() ->
                new IllegalStateException("LevelType nicht registriert: " + lk));
    }

    private void setProgressInternal(LevelProgress p, int level, double xp) {
        LevelProgress updated = new LevelProgress(p.getPlayerId(), p.getKey(), level, xp);

        cache.computeIfPresent(p.getPlayerId(), (pid, map) -> {
            map.put(p.getKey(), updated);
            return map;
        });

        markDirty(updated);
    }

    private String indexKey(LevelProgress p) {
        return p.getPlayerId() + "|" + p.getKey();
    }

    private void markDirty(LevelProgress p) {
        String idx = indexKey(p);
        // deduplizieren: Nur die letzte Version je Key in Index, Queue hält Reihenfolge für Batch
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
            plugin.getLogger().severe("[NexLevel] Flush fehlgeschlagen: " + t.getMessage());
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
                    // Sicherheitscheck: falls durch Race noch was rein kam, erneut prüfen
                    if (dirtyQueue.isEmpty()) break;
                    else continue;
                }
                flushBatch(batch);
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("[NexLevel] Vollständiger Flush beim Shutdown fehlgeschlagen: " + t.getMessage());
        }
    }

    public void preloadAllAsync() {
        CompletableFuture.runAsync(this::preloadAllBlockingSafe);
    }

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
                        // Direkt in den Cache legen, ohne Registrierung zu prüfen
                        cache.computeIfAbsent(lp.getPlayerId(), __ -> new ConcurrentHashMap<>())
                                .put(lp.getKey(), lp);
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
                        plugin.getLogger().severe("[NexLevel] Rollback after flushBatch failed: " + rollbackEx.getMessage());
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
            plugin.getLogger().severe("[NexLevel] Batch-Flush failed: " + e.getMessage());
            e.printStackTrace();
            if (!shuttingDown.get()) {
                // Nur im Normalbetrieb requeue
                plugin.getLogger().severe("[NexLevel] Requeue dirty entries for next flush attempt.");
                batch.forEach(this::markDirty);
            } else {
                plugin.getLogger().severe("[NexLevel] Shutdown in progress, will NOT requeue dirty entries.");
            }
        }
    }
}