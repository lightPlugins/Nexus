package io.nexstudios.nexus.bukkit.effects.trigger;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.filters.DamageContext;
import io.nexstudios.nexus.bukkit.effects.impl.MultiplyDamageEffect;
import io.nexstudios.nexus.bukkit.effects.runtime.DamageBindingIndex;
import io.nexstudios.nexus.bukkit.effects.runtime.EffectBindingRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public record EntityDamageTriggerListener(EffectBindingRegistry registry) implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        if (registry.isEmpty()) return;

        DamageBindingIndex index = registry.getDamageIndex();
        var type = event.getEntity().getType();

        // Deduplizieren, falls ein Binding sowohl perType als auch generic matched
        Set<DamageBindingIndex.CompiledBinding> seen = new HashSet<>();
        apply(index.forType(type), event, seen);
        apply(index.generic(), event, seen);
    }

    private void apply(List<DamageBindingIndex.CompiledBinding> list,
                       EntityDamageByEntityEvent event,
                       Set<DamageBindingIndex.CompiledBinding> seen) {
        if (list == null || list.isEmpty()) return;

        var hook = NexusPlugin.getInstance().getMythicMobsHook();

        List<DamageBindingIndex.CompiledBinding> nonMultipliers = new ArrayList<>();
        List<DamageBindingIndex.CompiledBinding> multipliers = new ArrayList<>();

        for (var cb : list) {
            // Überspringen, wenn bereits eingeplant
            if (!seen.add(cb)) continue;

            if (cb.effect instanceof MultiplyDamageEffect) {
                multipliers.add(cb);
            } else {
                nonMultipliers.add(cb);
            }
        }

        DamageContext ctx = new DamageContext(event);

        Consumer<DamageBindingIndex.CompiledBinding> run = cb -> {
            boolean mcMatch = !cb.mcTypes.isEmpty() && cb.mcTypes.contains(event.getEntity().getType());
            boolean mythicMatch = false;

            if (!cb.mythicIds.isEmpty() && hook != null) {
                for (String id : cb.mythicIds) {
                    if (hook.isMythicMob(event.getEntity(), id)) {
                        mythicMatch = true;
                        break;
                    }
                }
            }

            boolean baseMatch = cb.matchAll || mcMatch || mythicMatch;
            if (!baseMatch) return;

            // vorkompilierte Filter prüfen
            if (!cb.filters.isEmpty()) {
                for (var f : cb.filters) {
                    if (!f.test(ctx)) return;
                }
            }

            cb.effect.onDamage(event);
        };

        for (var cb : nonMultipliers) run.accept(cb);
        for (var cb : multipliers) run.accept(cb);
    }
}









