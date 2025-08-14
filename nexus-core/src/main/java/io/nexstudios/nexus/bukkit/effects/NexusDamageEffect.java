package io.nexstudios.nexus.bukkit.effects;

import org.bukkit.event.entity.EntityDamageByEntityEvent;

public interface NexusDamageEffect extends NexusEffect {
    void onDamage(EntityDamageByEntityEvent event);
}

