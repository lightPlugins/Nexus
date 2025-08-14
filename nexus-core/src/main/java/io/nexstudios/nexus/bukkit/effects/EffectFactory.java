package io.nexstudios.nexus.bukkit.effects;

import io.nexstudios.nexus.bukkit.effects.impl.AddDamageEffect;
import io.nexstudios.nexus.bukkit.effects.impl.MultiplyDamageEffect;
import io.nexstudios.nexus.bukkit.effects.load.EffectConfig;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;

import java.util.*;
import java.util.function.Function;

public class EffectFactory {

    private static final class EffectType {
        final Function<EffectConfig, NexusEffect> builder;
        final Set<String> allowedTriggers; // null => alle erlaubt
        final Set<String> allowedFilters;  // null => alle erlaubt

        EffectType(Function<EffectConfig, NexusEffect> builder,
                   Set<String> allowedTriggers,
                   Set<String> allowedFilters) {
            this.builder = builder;
            this.allowedTriggers = allowedTriggers == null ? null : toLowerSet(allowedTriggers);
            this.allowedFilters  = allowedFilters  == null ? null : toLowerSet(allowedFilters);
        }

        private static Set<String> toLowerSet(Set<String> in) {
            Set<String> out = new HashSet<>();
            for (String s : in) if (s != null) out.add(s.toLowerCase(Locale.ROOT));
            return Set.copyOf(out);
        }
    }

    private final Map<String, EffectType> registry = new HashMap<>();
    private final PlayerVariableResolver resolver;

    public EffectFactory(PlayerVariableResolver resolver) {
        this.resolver = resolver;
        registerBuiltins();
    }

    public EffectFactory() {
        this(PlayerVariableResolver.ofStore());
    }

    private void registerBuiltins() {
        // Builtins: Damage-Trigger + match-item Filter
        register("add-damage",
                cfg -> new AddDamageEffect(cfg.getString("expression", "0"), this.resolver),
                Set.of("entity-damage"),
                Set.of("match-item-hand", "has-permission", "in-world", "match-item-inventory")
        );
        register("multiply-damage",
                cfg -> new MultiplyDamageEffect(cfg.getString("expression", "1"), this.resolver),
                Set.of("entity-damage"),
                Set.of("match-item-hand", "has-permission", "in-world", "match-item-inventory")
        );
    }

    // Kompatibel: keine Restriktion
    public boolean register(String id, Function<EffectConfig, NexusEffect> builder) {
        return register(id, builder, null, null);
    }

    // Registrierung mit erlaubten Trigger-/Filter-IDs
    public boolean register(String id,
                            Function<EffectConfig, NexusEffect> builder,
                            Set<String> allowedTriggers,
                            Set<String> allowedFilters) {
        String key = id.toLowerCase(Locale.ROOT);
        if (registry.containsKey(key)) return false;
        registry.put(key, new EffectType(builder, allowedTriggers, allowedFilters));
        return true;
    }

    public NexusEffect create(String id, EffectConfig cfg) {
        EffectType t = registry.get(id.toLowerCase(Locale.ROOT));
        return t != null ? t.builder.apply(cfg) : null;
    }

    public int getRegisteredEffectTypeCount() {
        return registry.size();
    }

    public Set<String> getAllowedTriggerIds(String id) {
        EffectType t = registry.get(id.toLowerCase(Locale.ROOT));
        return t == null ? null : t.allowedTriggers;
    }

    public Set<String> getAllowedFilterIds(String id) {
        EffectType t = registry.get(id.toLowerCase(Locale.ROOT));
        return t == null ? null : t.allowedFilters;
    }
}





