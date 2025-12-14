package io.nexstudios.nexus.bukkit.effects.trigger;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.NexusDamageMultiplierEffect;
import io.nexstudios.nexus.bukkit.effects.filters.DamageContext;
import io.nexstudios.nexus.bukkit.effects.runtime.DamageBindingIndex;
import io.nexstudios.nexus.bukkit.effects.runtime.EffectBindingRegistry;
import org.bukkit.entity.EntityType;
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

        if (registry.isEmpty()) return;

        DamageBindingIndex index = registry.getDamageIndex();

        // OUTGOING: Damager muss Player sein (wie bisher)
        if (event.getDamager() instanceof Player) {
            EntityType targetType = event.getEntity().getType();
            Set<DamageBindingIndex.CompiledBinding> seen = new HashSet<>();
            applyOutgoing(index.outgoingForTargetType(targetType), event, seen);
            applyOutgoing(index.outgoingGeneric(), event, seen);
        }

        // INCOMING: Target muss Player sein
        if (event.getEntity() instanceof Player) {
            EntityType damagerType = event.getDamager().getType();
            Set<DamageBindingIndex.CompiledBinding> seen = new HashSet<>();
            applyIncoming(index.incomingForDamagerType(damagerType), event, seen);
            applyIncoming(index.incomingGeneric(), event, seen);
        }
    }

    private void applyOutgoing(List<DamageBindingIndex.CompiledBinding> list,
                               EntityDamageByEntityEvent event,
                               Set<DamageBindingIndex.CompiledBinding> seen) {
        apply(list, event, seen, MatchSide.OUTGOING);
    }

    private void applyIncoming(List<DamageBindingIndex.CompiledBinding> list,
                               EntityDamageByEntityEvent event,
                               Set<DamageBindingIndex.CompiledBinding> seen) {
        apply(list, event, seen, MatchSide.INCOMING);
    }

    private enum MatchSide { OUTGOING, INCOMING }

    private void apply(List<DamageBindingIndex.CompiledBinding> list,
                       EntityDamageByEntityEvent event,
                       Set<DamageBindingIndex.CompiledBinding> seen,
                       MatchSide side) {
        if (list == null || list.isEmpty()) return;

        var hook = NexusPlugin.getInstance().getMythicMobsHook();

        List<DamageBindingIndex.CompiledBinding> nonMultipliers = new ArrayList<>();
        List<DamageBindingIndex.CompiledBinding> multipliers = new ArrayList<>();

        for (var cb : list) {
            if (!seen.add(cb)) continue;

            if (cb.effect instanceof NexusDamageMultiplierEffect) {
                multipliers.add(cb);
            } else {
                nonMultipliers.add(cb);
            }
        }

        DamageContext ctx = new DamageContext(event);

        Consumer<DamageBindingIndex.CompiledBinding> run = cb -> {
            boolean mcMatch;
            boolean mythicMatch = false;

            if (side == MatchSide.OUTGOING) {
                mcMatch = !cb.mcTypes.isEmpty() && cb.mcTypes.contains(event.getEntity().getType());
                if (!cb.mythicIds.isEmpty() && hook != null) {
                    for (String id : cb.mythicIds) {
                        if (hook.isMythicMob(event.getEntity(), id)) {
                            mythicMatch = true;
                            break;
                        }
                    }
                }
            } else {
                mcMatch = !cb.mcTypes.isEmpty() && cb.mcTypes.contains(event.getDamager().getType());
                if (!cb.mythicIds.isEmpty() && hook != null) {
                    for (String id : cb.mythicIds) {
                        if (hook.isMythicMob(event.getDamager(), id)) {
                            mythicMatch = true;
                            break;
                        }
                    }
                }
            }

            boolean baseMatch = cb.matchAll || mcMatch || mythicMatch;
            if (!baseMatch) return;

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