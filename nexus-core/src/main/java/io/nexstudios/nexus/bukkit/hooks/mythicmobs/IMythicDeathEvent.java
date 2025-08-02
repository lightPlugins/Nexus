package io.nexstudios.nexus.bukkit.hooks.mythicmobs;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public interface IMythicDeathEvent {
    void execute(MythicMobDeathEvent event);
    void execute(EntityDamageByEntityEvent event, ActiveMob activeMob);
}
