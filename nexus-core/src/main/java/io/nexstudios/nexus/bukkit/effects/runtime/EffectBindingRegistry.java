package io.nexstudios.nexus.bukkit.effects.runtime;

import io.nexstudios.nexus.bukkit.effects.EffectBinding;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class EffectBindingRegistry {

    private final AtomicReference<List<EffectBinding>> ref = new AtomicReference<>(List.of());
    private final AtomicReference<DamageBindingIndex> damageIndexRef = new AtomicReference<>(new DamageBindingIndex(java.util.Map.of(), java.util.List.of()));

    public List<EffectBinding> getBindings() {
        return ref.get(); // Snapshot
    }

    public void setBindings(List<EffectBinding> newBindings) {
        List<EffectBinding> snapshot = newBindings == null ? List.of() : List.copyOf(newBindings);
        ref.set(snapshot);
        // Rebuild Damage-Index einmalig
        damageIndexRef.set(DamageBindingIndex.build(snapshot));
    }

    public boolean isEmpty() {
        return ref.get().isEmpty();
    }

    // Schneller Zugriff auf den fertigen Index
    public DamageBindingIndex getDamageIndex() {
        return damageIndexRef.get();
    }
}


