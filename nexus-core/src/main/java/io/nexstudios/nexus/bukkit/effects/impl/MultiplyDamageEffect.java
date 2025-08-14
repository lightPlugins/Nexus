package io.nexstudios.nexus.bukkit.effects.impl;

import io.nexstudios.nexus.bukkit.effects.NexusDamageEffect;
import io.nexstudios.nexus.bukkit.effects.cache.DamageValueCache;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MultiplyDamageEffect implements NexusDamageEffect {

    private final String expression;
    private final PlayerVariableResolver resolver;

    public MultiplyDamageEffect(String expression, PlayerVariableResolver resolver) {
        this.expression = expression;
        this.resolver = resolver;
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return; // nur Spieler-basiert; optional: anders behandeln
        }
        double factor = DamageValueCache.getOrCompute(player, expression, resolver);
        if (!Double.isFinite(factor) || factor == 1.0d) {
            return; // nichts zu tun
        }
        double result = event.getDamage() * factor;
        if (result < 0.0) result = 0.0; // negative Werte vermeiden
        event.setDamage(result);
    }
}
