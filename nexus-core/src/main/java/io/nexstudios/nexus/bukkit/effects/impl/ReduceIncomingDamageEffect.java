package io.nexstudios.nexus.bukkit.effects.impl;

import io.nexstudios.nexus.bukkit.effects.NexusDamageEffect;
import io.nexstudios.nexus.bukkit.effects.cache.DamageValueCache;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ReduceIncomingDamageEffect implements NexusDamageEffect {

    private final String expression;
    private final PlayerVariableResolver resolver;
    private final String contextKey; // z. B. "stat:<statId>" oder null

    public ReduceIncomingDamageEffect(String expression, PlayerVariableResolver resolver) {
        this(expression, resolver, null);
    }

    public ReduceIncomingDamageEffect(String expression, PlayerVariableResolver resolver, String contextKey) {
        this.expression = expression;
        this.resolver = resolver;
        this.contextKey = contextKey;
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent event) {
        // incoming-damage: Variablen/Expression auf dem Opfer (Player) berechnen
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        double reduce = DamageValueCache.getOrCompute(player, expression, resolver, contextKey);
        if (!Double.isFinite(reduce) || reduce == 0.0) {
            return;
        }

        event.setDamage(Math.max(0.0, event.getDamage() - reduce));
    }
}