package io.nexstudios.nexus.bukkit.effects.impl;

import io.nexstudios.nexus.bukkit.effects.NexusDamageEffect;
import io.nexstudios.nexus.bukkit.effects.cache.DamageValueCache;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MultiplyDamageEffect implements NexusDamageEffect {

    private final String expression;
    private final PlayerVariableResolver resolver;
    private final String contextKey; // z. B. "stat:<statId>" oder null

    public MultiplyDamageEffect(String expression, PlayerVariableResolver resolver) {
        this(expression, resolver, null);
    }

    public MultiplyDamageEffect(String expression, PlayerVariableResolver resolver, String contextKey) {
        this.expression = expression;
        this.resolver = resolver;
        this.contextKey = contextKey;
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        double factor = DamageValueCache.getOrCompute(player, expression, resolver, contextKey);
        if (!Double.isFinite(factor) || factor == 1.0d) {
            return;
        }
        double result = event.getDamage() * factor;
        if (result < 0.0) result = 0.0;
        event.setDamage(result);
    }
}