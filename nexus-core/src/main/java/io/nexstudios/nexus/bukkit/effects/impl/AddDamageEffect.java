package io.nexstudios.nexus.bukkit.effects.impl;

import io.nexstudios.nexus.bukkit.effects.NexusDamageEffect;
import io.nexstudios.nexus.bukkit.effects.cache.DamageValueCache;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class AddDamageEffect implements NexusDamageEffect {

    private final String expression;
    private final PlayerVariableResolver resolver;
    private final String contextKey; // z. B. "stat:<statId>" oder null

    public AddDamageEffect(String expression, PlayerVariableResolver resolver) {
        this(expression, resolver, null);
    }

    public AddDamageEffect(String expression, PlayerVariableResolver resolver, String contextKey) {
        this.expression = expression;
        this.resolver = resolver;
        this.contextKey = contextKey;
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        double add = DamageValueCache.getOrCompute(player, expression, resolver, contextKey);
        if (Double.isFinite(add) && add != 0.0) {
            event.setDamage(Math.max(0.0, event.getDamage() + add));
        }
    }
}