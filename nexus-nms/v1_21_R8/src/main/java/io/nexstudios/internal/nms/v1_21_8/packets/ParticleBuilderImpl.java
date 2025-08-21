package io.nexstudios.internal.nms.v1_21_8.packets;

import io.nexstudios.nexus.bukkit.particle.ParticleBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;


public class ParticleBuilderImpl implements ParticleBuilder {

    @Override
    public void spawnParticle(Player bukkitPlayer, Location location, float offsetX, float offsetY, float offsetZ, float speed, int count) {
        // Wandle den Bukkit-Player in den NMS-Spieler um
        ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();

        if(nmsPlayer == null) {
            return;
        }

        // Erstelle ein neues `ClientboundLevelParticlesPacket`
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                ParticleTypes.FLAME,  // Partikeltyp (Flamme)
                true,                 // Override Partikel-Limiter (Clientseite)
                true,                 // Partikel auch außerhalb des Sichtbereichs sichtbar
                location.getX(),      // X-Koordinate
                location.getY(),      // Y-Koordinate
                location.getZ(),      // Z-Koordinate
                offsetX,              // Offset X
                offsetY,              // Offset Y
                offsetZ,              // Offset Z
                speed,                // Geschwindigkeit
                count                 // Partikelanzahl
        );

        // Sende das Paket explizit an den Spieler
        nmsPlayer.connection.send(packet);
    }
}




