package io.nexstudios.nexus.bukkit.particle;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ParticleBuilder {

    void spawnParticle(Player bukkitPlayer, Location location, float offsetX, float offsetY, float offsetZ, float speed, int count);


}
