package io.nexstudios.nexus.bukkit.effects.impl;

import io.nexstudios.nexus.bukkit.effects.NexusDamageEffect;
import io.nexstudios.nexus.bukkit.effects.cache.DamageValueCache;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class AddDamageEffect implements NexusDamageEffect {

    private final String expression;
    private final PlayerVariableResolver resolver;

    public AddDamageEffect(String expression, PlayerVariableResolver resolver) {
        this.expression = expression;
        this.resolver = resolver;
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return; // nur Spieler-basiert; optional: anders behandeln
        }
        double add = DamageValueCache.getOrCompute(player, expression, resolver);
        if (Double.isFinite(add) && add != 0.0) {
            event.setDamage(Math.max(0.0, event.getDamage() + add));
        }
    }
}



