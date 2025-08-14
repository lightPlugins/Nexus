package io.nexstudios.nexus.bukkit.effects.filters;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class DamageContext implements TriggerContext, PlayerContext, WorldContext {
    private final EntityDamageByEntityEvent event;

    public DamageContext(EntityDamageByEntityEvent event) {
        this.event = event;
    }

    public EntityDamageByEntityEvent event() {
        return event;
    }

    public Player damagerAsPlayer() {
        return (event.getDamager() instanceof Player p) ? p : null;
    }

    public Entity target() {
        return event.getEntity();
    }

    @Override
    public Player player() {
        return damagerAsPlayer();
    }

    @Override
    public World world() {
        return event.getEntity().getWorld();
    }

}


