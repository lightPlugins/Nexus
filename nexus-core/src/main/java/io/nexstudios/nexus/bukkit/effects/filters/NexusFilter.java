package io.nexstudios.nexus.bukkit.effects.filters;

@FunctionalInterface
public interface NexusFilter<C extends TriggerContext> {
    boolean test(C ctx);
}

