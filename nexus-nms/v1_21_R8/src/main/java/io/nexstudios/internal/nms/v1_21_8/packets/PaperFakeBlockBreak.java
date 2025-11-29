package io.nexstudios.internal.nms.v1_21_8.packets;

import io.nexstudios.nexus.bukkit.fakebreak.FakeBlockBreakNms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * NMS-Implementierung für Paper 1.21.8 (v1_21_R8).
 *
 * Achtung:
 * - Prüfe die genauen Klassen-/Paketnamen deiner
 *   NMS- und CraftBukkit-Implementierung.
 * - Die Logik ist an die bekannte Struktur angelehnt.
 */
public final class PaperFakeBlockBreak implements FakeBlockBreakNms {

    @Override
    public void sendBlockBreakStage(Player player, Location location, int stage) {
        if (player == null || location == null || location.getWorld() == null) return;

        CraftPlayer craftPlayer = (CraftPlayer) player;
        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        // entityId: wir benutzen die EntityId des Spielers als "destroyerId",
        // das ist in Vanilla üblich.
        int destroyerId = craftPlayer.getEntityId();

        ClientboundBlockDestructionPacket packet =
                new ClientboundBlockDestructionPacket(destroyerId, pos, stage);

        craftPlayer.getHandle().connection.send(packet);
    }

    @Override
    public void sendFakeBlockChange(Player player, Location location, BlockData data) {
        if (player == null || location == null || location.getWorld() == null || data == null) return;

        CraftPlayer craftPlayer = (CraftPlayer) player;
        CraftWorld craftWorld = (CraftWorld) location.getWorld();

        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockState nmsState = ((CraftBlockData) data).getState();

        ClientboundBlockUpdatePacket packet =
                new ClientboundBlockUpdatePacket(pos, nmsState);

        craftPlayer.getHandle().connection.send(packet);
    }

    @Override
    public void sendResetBlock(Player player, Location location) {
        if (player == null || location == null || location.getWorld() == null) return;

        CraftPlayer craftPlayer = (CraftPlayer) player;
        CraftWorld craftWorld = (CraftWorld) location.getWorld();

        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockState realState = craftWorld.getHandle().getBlockState(pos);

        ClientboundBlockUpdatePacket packet =
                new ClientboundBlockUpdatePacket(pos, realState);

        craftPlayer.getHandle().connection.send(packet);
    }
}