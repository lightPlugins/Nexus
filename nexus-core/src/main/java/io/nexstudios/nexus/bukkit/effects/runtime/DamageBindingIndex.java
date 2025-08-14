package io.nexstudios.nexus.bukkit.effects.runtime;

import io.nexstudios.nexus.bukkit.effects.NexusDamageEffect;
import io.nexstudios.nexus.bukkit.effects.EffectBinding;
import io.nexstudios.nexus.bukkit.effects.filters.DamageContext;
import io.nexstudios.nexus.bukkit.effects.filters.FilterFactory;
import io.nexstudios.nexus.bukkit.effects.filters.NexusFilter;
import org.bukkit.entity.EntityType;

import java.util.*;

public record DamageBindingIndex(Map<EntityType, List<CompiledBinding>> perType, List<CompiledBinding> generic) {

    public static final class CompiledBinding {
        public final NexusDamageEffect effect;
        public final boolean matchAll;
        public final Set<EntityType> mcTypes;
        public final Set<String> mythicIds;
        public final List<NexusFilter<DamageContext>> filters;

        public CompiledBinding(NexusDamageEffect effect,
                               boolean matchAll,
                               Set<EntityType> mcTypes,
                               Set<String> mythicIds,
                               List<NexusFilter<DamageContext>> filters) {
            this.effect = effect;
            this.matchAll = matchAll;
            this.mcTypes = mcTypes;
            this.mythicIds = mythicIds;
            this.filters = (filters == null || filters.isEmpty()) ? List.of() : List.copyOf(filters);
        }
    }

    public static DamageBindingIndex build(List<EffectBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return new DamageBindingIndex(Map.of(), List.of());
        }

        Map<EntityType, List<CompiledBinding>> perType = new EnumMap<>(EntityType.class);
        List<CompiledBinding> generic = new ArrayList<>();

        for (EffectBinding b : bindings) {
            if (!(b.effect() instanceof NexusDamageEffect de)) {
                continue;
            }

            ParsedDamageTrigger parsed = ParsedDamageTrigger.from(b);
            if (!parsed.validForDamage()) continue;

            Object rawFilters = b.rawConfig().get("filters");
            List<NexusFilter<DamageContext>> compiledFilters = FilterFactory.compileForDamage(rawFilters);

            CompiledBinding cb = new CompiledBinding(
                    de,
                    parsed.matchAll,
                    parsed.mcTypes,
                    parsed.mythicIds,
                    compiledFilters
            );

            boolean onlyMythic = !parsed.mythicIds.isEmpty() && parsed.mcTypes.isEmpty();
            boolean genericMatchAll = parsed.matchAll && parsed.mcTypes.isEmpty(); // keine Duplikate erzeugen
            if (onlyMythic || genericMatchAll) {
                generic.add(cb);
            }

            for (EntityType t : parsed.mcTypes) {
                perType.computeIfAbsent(t, k -> new ArrayList<>()).add(cb);
            }
        }

        perType.replaceAll((k, v) -> List.copyOf(v));
        return new DamageBindingIndex(perType, List.copyOf(generic));
    }

    public List<CompiledBinding> forType(EntityType type) {
        return perType.getOrDefault(type, List.of());
    }

    public List<CompiledBinding> generic() {
        return generic;
    }

    private static final class ParsedDamageTrigger {
        final boolean matchAll;
        final Set<EntityType> mcTypes;
        final Set<String> mythicIds;
        final boolean hasAnyDamageTrigger;

        private ParsedDamageTrigger(boolean matchAll, Set<EntityType> mcTypes, Set<String> mythicIds, boolean hasAnyDamageTrigger) {
            this.matchAll = matchAll;
            this.mcTypes = mcTypes;
            this.mythicIds = mythicIds;
            this.hasAnyDamageTrigger = hasAnyDamageTrigger;
        }

        boolean validForDamage() {
            return hasAnyDamageTrigger;
        }

        static ParsedDamageTrigger from(EffectBinding b) {
            boolean matchAll = false;
            Set<EntityType> mcTypes = new HashSet<>();
            Set<String> mythicIds = new HashSet<>();
            boolean hasDamage = false;

            if (b.triggers() != null) {
                for (var spec : b.triggers()) {
                    String id = String.valueOf(spec.id()).toLowerCase(Locale.ROOT);
                    if (!id.equals("entity-damage") && !id.equals("damage")) continue;

                    hasDamage = true;

                    Map<String, Object> opts = spec.options();
                    if (opts == null) continue;

                    Object ma = opts.get("match-all");
                    if (ma instanceof Boolean bma && bma) matchAll = true;
                    else if (ma != null && Boolean.parseBoolean(String.valueOf(ma))) matchAll = true;

                    Object ents = opts.get("entities");
                    if (ents instanceof List<?> list) {
                        for (Object o : list) {
                            if (o == null) continue;
                            String s = String.valueOf(o).trim();
                            String[] parts = s.split(":", 2);
                            if (parts.length == 2 && "minecraft".equalsIgnoreCase(parts[0])) {
                                try {
                                    mcTypes.add(EntityType.valueOf(parts[1].toUpperCase(Locale.ROOT)));
                                } catch (IllegalArgumentException ignored) {}
                            } else if (parts.length == 2 && "mythicmobs".equalsIgnoreCase(parts[0])) {
                                mythicIds.add(parts[1]);
                            }
                        }
                    }
                }
            }

            return new ParsedDamageTrigger(matchAll, mcTypes, mythicIds, hasDamage);
        }
    }
}


