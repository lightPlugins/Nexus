package io.nexstudios.nexus.bukkit.utils;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class BlockUtil implements Listener {

    private final JavaPlugin plugin;

    public BlockUtil(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockPlacedByPlayer(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (player.getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }

        setPlacer(block, player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }
        clearPlacer(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block b : event.blockList()) {
            clearPlacer(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block b : event.blockList()) {
            clearPlacer(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        BlockFace face = event.getDirection();
        List<Block> moved = event.getBlocks();

        for (int i = moved.size() - 1; i >= 0; i--) {
            Block from = moved.get(i);
            int toX = from.getX() + face.getModX();
            int toY = from.getY() + face.getModY();
            int toZ = from.getZ() + face.getModZ();
            movePlacer(from.getWorld(), from.getX(), from.getY(), from.getZ(), toX, toY, toZ);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;
        BlockFace face = event.getDirection().getOppositeFace();
        List<Block> moved = event.getBlocks();

        for (int i = moved.size() - 1; i >= 0; i--) {
            Block from = moved.get(i);
            int toX = from.getX() + face.getModX();
            int toY = from.getY() + face.getModY();
            int toZ = from.getZ() + face.getModZ();
            movePlacer(from.getWorld(), from.getX(), from.getY(), from.getZ(), toX, toY, toZ);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        for (NamespacedKey key : pdc.getKeys()) {
            String raw = key.getKey();
            if (!raw.startsWith("pb_")) continue;

            // Erwartetes Format: pb_x_y_z
            String[] parts = raw.split("_");
            if (parts.length != 4) {
                // Ungültiger
                NexusPlugin.nexusLogger.error("Invalid block key: " + raw);
                pdc.remove(key);
                continue;
            }

            int x, y, z;
            try {
                x = Integer.parseInt(parts[1]);
                y = Integer.parseInt(parts[2]);
                z = Integer.parseInt(parts[3]);
            } catch (NumberFormatException ex) {
                // Korrupt
                NexusPlugin.nexusLogger.error("Invalid block coordinates in key: " + raw);
                pdc.remove(key);
                continue;
            }

            Block b = world.getBlockAt(x, y, z);
            if (b.getType().isAir()) {
                // Block existiert nicht mehr
                NexusPlugin.nexusLogger.error("Block does not exist anymore, removing key: " + raw);
                pdc.remove(key);
            }

            // - Wenn b.getType() != gespeichertes Material -> pdc.remove(key)
        }
    }

    public UUID getPlacer(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = coordKey(block.getX(), block.getY(), block.getZ());
        String uuidStr = pdc.get(key, PersistentDataType.STRING);
        if (uuidStr == null) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isPlayerPlaced(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = coordKey(block.getX(), block.getY(), block.getZ());
        return pdc.has(key, PersistentDataType.STRING);
    }

    private void setPlacer(Block block, UUID uuid) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = coordKey(block.getX(), block.getY(), block.getZ());
        pdc.set(key, PersistentDataType.STRING, uuid.toString());
    }

    private void clearPlacer(Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        NamespacedKey key = coordKey(block.getX(), block.getY(), block.getZ());
        pdc.remove(key);
    }

    private void movePlacer(World world, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {

        Chunk fromChunk = world.getChunkAt(fromX >> 4, fromZ >> 4);
        PersistentDataContainer fromPdc = fromChunk.getPersistentDataContainer();
        NamespacedKey fromKey = coordKey(fromX, fromY, fromZ);
        String val = fromPdc.get(fromKey, PersistentDataType.STRING);
        if (val == null) return;

        Chunk toChunk = world.getChunkAt(toX >> 4, toZ >> 4);
        PersistentDataContainer toPdc = toChunk.getPersistentDataContainer();
        NamespacedKey toKey = coordKey(toX, toY, toZ);
        toPdc.set(toKey, PersistentDataType.STRING, val);

        fromPdc.remove(fromKey);
    }

    private NamespacedKey coordKey(int x, int y, int z) {
        String keyStr = "pb_" + x + "_" + y + "_" + z;
        return new NamespacedKey(plugin, keyStr);
    }
}