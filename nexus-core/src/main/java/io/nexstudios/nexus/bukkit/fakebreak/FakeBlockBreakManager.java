package io.nexstudios.nexus.bukkit.fakebreak;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default-Implementierung von FakeBlockBreakService.
 */
public final class FakeBlockBreakManager implements FakeBlockBreakService {

    private final Plugin plugin;
    private final FakeBlockBreakNms nms;

    /**
     * key: player UUID + block key
     */
    private final Map<String, FakeBlockBreakSessionImpl> sessions = new ConcurrentHashMap<>();

    public FakeBlockBreakManager(Plugin plugin, FakeBlockBreakNms nms) {
        this.plugin = plugin;
        this.nms = nms;

        // Ticker für Crack-Updates
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    @Override
    public FakeBlockBreakSession startFakeBreak(Player player, Block block, FakeBlockBreakConfig config) {
        String key = sessionKey(player, block);
        // Bestehende Session abbrechen
        FakeBlockBreakSessionImpl existing = sessions.remove(key);
        if (existing != null) {
            existing.internalCancel(false);
        }

        FakeBlockBreakSessionImpl session = new FakeBlockBreakSessionImpl(player, block, config);
        sessions.put(key, session);
        return session;
    }

    @Override
    public boolean cancelFakeBreak(Player player, Block block) {
        String key = sessionKey(player, block);
        FakeBlockBreakSessionImpl session = sessions.remove(key);
        if (session == null) return false;
        session.internalCancel(true);
        return true;
    }

    @Override
    public @Nullable FakeBlockBreakSession getSession(Player player, Block block) {
        return sessions.get(sessionKey(player, block));
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (FakeBlockBreakSessionImpl session : sessions.values().toArray(new FakeBlockBreakSessionImpl[0])) {
            if (session.isFinished()) {
                sessions.remove(session.key);
                continue;
            }
            session.tick(now);
        }
    }

    private static String sessionKey(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        Location loc = block.getLocation();
        return uuid + ":" + loc.getWorld().getUID() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private final class FakeBlockBreakSessionImpl implements FakeBlockBreakSession {

        private final String key;
        private final Player player;
        private final Block block;
        private final FakeBlockBreakConfig config;
        private final Instant startTime;

        private volatile boolean finished = false;
        private int lastStage = -1;

        private FakeBlockBreakSessionImpl(Player player, Block block, FakeBlockBreakConfig config) {
            this.key = sessionKey(player, block);
            this.player = player;
            this.block = block;
            this.config = config;
            this.startTime = Instant.now();
        }

        @Override
        public Player player() {
            return player;
        }

        @Override
        public Block block() {
            return block;
        }

        @Override
        public FakeBlockBreakConfig config() {
            return config;
        }

        @Override
        public Instant startTime() {
            return startTime;
        }

        @Override
        public void cancel() {
            sessions.remove(key);
            internalCancel(true);
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        private void internalCancel(boolean resetToRealBlock) {
            if (finished) return;
            finished = true;
            Location loc = block.getLocation();
            if (lastStage >= 0) {
                nms.sendBlockBreakStage(player, loc, -1);
            }
            if (resetToRealBlock) {
                nms.sendResetBlock(player, loc);
            }
        }

        private void tick(long nowMillis) {
            if (finished) return;
            if (!player.isOnline()) {
                cancel();
                return;
            }

            long durationMillis = config.breakDuration().toMillis();
            if (durationMillis <= 0) {
                finishInstant();
                return;
            }

            long elapsed = nowMillis - startTime.toEpochMilli();
            double progress = Math.min(1.0, Math.max(0.0, (double) elapsed / durationMillis));

            int stage = (int) Math.floor(progress * 9.0); // 0..9
            Location loc = block.getLocation();

            if (stage != lastStage) {
                lastStage = stage;
                nms.sendBlockBreakStage(player, loc, stage);
            }

            if (progress >= 1.0) {
                finishAtEnd(loc);
            }
        }

        private void finishInstant() {
            Location loc = block.getLocation();
            finishAtEnd(loc);
        }

        private void finishAtEnd(Location loc) {
            if (finished) return;
            finished = true;

            // Optional: Cracks resetten
            if (config.resetCracksAfterFinish()) {
                nms.sendBlockBreakStage(player, loc, -1);
            }

            // Ergebnisblock nur für diesen Spieler anzeigen
            BlockData resultData = config.resultMaterial().createBlockData();
            nms.sendFakeBlockChange(player, loc, resultData);

            // Session aus Registry entfernen
            sessions.remove(key);
        }
    }
}