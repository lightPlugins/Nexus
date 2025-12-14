package io.nexstudios.nexus.bukkit.effects.runtime;

import io.nexstudios.nexus.bukkit.effects.EffectBinding;
import io.nexstudios.nexus.bukkit.effects.NexusDamageEffect;
import io.nexstudios.nexus.bukkit.effects.filters.DamageContext;
import io.nexstudios.nexus.bukkit.effects.filters.FilterFactory;
import io.nexstudios.nexus.bukkit.effects.filters.NexusFilter;
import org.bukkit.entity.EntityType;

import java.util.*;

public record DamageBindingIndex(
        Map<EntityType, List<CompiledBinding>> outgoingPerTargetType,
        List<CompiledBinding> outgoingGeneric,
        Map<EntityType, List<CompiledBinding>> incomingPerDamagerType,
        List<CompiledBinding> incomingGeneric
) {

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
            return new DamageBindingIndex(Map.of(), List.of(), Map.of(), List.of());
        }

        Map<EntityType, List<CompiledBinding>> outgoingPerTarget = new EnumMap<>(EntityType.class);
        List<CompiledBinding> outgoingGeneric = new ArrayList<>();

        Map<EntityType, List<CompiledBinding>> incomingPerDamager = new EnumMap<>(EntityType.class);
        List<CompiledBinding> incomingGeneric = new ArrayList<>();

        for (EffectBinding b : bindings) {
            if (!(b.effect() instanceof NexusDamageEffect de)) {
                continue;
            }

            ParsedDamageTriggers parsed = ParsedDamageTriggers.from(b);
            if (!parsed.hasOutgoing && !parsed.hasIncoming) continue;

            Object rawFilters = b.rawConfig().get("filters");
            List<NexusFilter<DamageContext>> compiledFilters = FilterFactory.compileForDamage(rawFilters);

            if (parsed.hasOutgoing) {
                CompiledBinding cb = new CompiledBinding(de, parsed.outgoingMatchAll, parsed.outgoingMcTypes, parsed.outgoingMythicIds, compiledFilters);

                boolean onlyMythic = !parsed.outgoingMythicIds.isEmpty() && parsed.outgoingMcTypes.isEmpty();
                boolean genericMatchAll = parsed.outgoingMatchAll && parsed.outgoingMcTypes.isEmpty();
                if (onlyMythic || genericMatchAll) {
                    outgoingGeneric.add(cb);
                }
                for (EntityType t : parsed.outgoingMcTypes) {
                    outgoingPerTarget.computeIfAbsent(t, k -> new ArrayList<>()).add(cb);
                }
            }

            if (parsed.hasIncoming) {
                CompiledBinding cb = new CompiledBinding(de, parsed.incomingMatchAll, parsed.incomingMcTypes, parsed.incomingMythicIds, compiledFilters);

                boolean onlyMythic = !parsed.incomingMythicIds.isEmpty() && parsed.incomingMcTypes.isEmpty();
                boolean genericMatchAll = parsed.incomingMatchAll && parsed.incomingMcTypes.isEmpty();
                if (onlyMythic || genericMatchAll) {
                    incomingGeneric.add(cb);
                }
                for (EntityType t : parsed.incomingMcTypes) {
                    incomingPerDamager.computeIfAbsent(t, k -> new ArrayList<>()).add(cb);
                }
            }
        }

        outgoingPerTarget.replaceAll((k, v) -> List.copyOf(v));
        incomingPerDamager.replaceAll((k, v) -> List.copyOf(v));

        return new DamageBindingIndex(
                outgoingPerTarget,
                List.copyOf(outgoingGeneric),
                incomingPerDamager,
                List.copyOf(incomingGeneric)
        );
    }

    public List<CompiledBinding> outgoingForTargetType(EntityType type) {
        return outgoingPerTargetType.getOrDefault(type, List.of());
    }

    public List<CompiledBinding> incomingForDamagerType(EntityType type) {
        return incomingPerDamagerType.getOrDefault(type, List.of());
    }

    private static final class ParsedDamageTriggers {
        final boolean hasOutgoing;
        final boolean outgoingMatchAll;
        final Set<EntityType> outgoingMcTypes;
        final Set<String> outgoingMythicIds;

        final boolean hasIncoming;
        final boolean incomingMatchAll;
        final Set<EntityType> incomingMcTypes;
        final Set<String> incomingMythicIds;

        private ParsedDamageTriggers(
                boolean hasOutgoing,
                boolean outgoingMatchAll,
                Set<EntityType> outgoingMcTypes,
                Set<String> outgoingMythicIds,
                boolean hasIncoming,
                boolean incomingMatchAll,
                Set<EntityType> incomingMcTypes,
                Set<String> incomingMythicIds
        ) {
            this.hasOutgoing = hasOutgoing;
            this.outgoingMatchAll = outgoingMatchAll;
            this.outgoingMcTypes = outgoingMcTypes;
            this.outgoingMythicIds = outgoingMythicIds;
            this.hasIncoming = hasIncoming;
            this.incomingMatchAll = incomingMatchAll;
            this.incomingMcTypes = incomingMcTypes;
            this.incomingMythicIds = incomingMythicIds;
        }

        static ParsedDamageTriggers from(EffectBinding b) {
            boolean hasOutgoing = false;
            boolean outgoingMatchAll = false;
            Set<EntityType> outgoingMcTypes = new HashSet<>();
            Set<String> outgoingMythicIds = new HashSet<>();

            boolean hasIncoming = false;
            boolean incomingMatchAll = false;
            Set<EntityType> incomingMcTypes = new HashSet<>();
            Set<String> incomingMythicIds = new HashSet<>();

            if (b.triggers() != null) {
                for (var spec : b.triggers()) {
                    String id = String.valueOf(spec.id()).toLowerCase(Locale.ROOT);

                    boolean outgoing = id.equals("entity-damage") || id.equals("damage");
                    boolean incoming = id.equals("incoming-damage");

                    if (!outgoing && !incoming) continue;

                    Map<String, Object> opts = spec.options();
                    if (opts == null) opts = Map.of();

                    if (outgoing) {
                        hasOutgoing = true;
                        outgoingMatchAll |= readBool(opts.get("match-all"));
                        readEntities(opts.get("entities"), outgoingMcTypes, outgoingMythicIds);
                    }

                    if (incoming) {
                        hasIncoming = true;
                        incomingMatchAll |= readBool(opts.get("match-all"));
                        readEntities(opts.get("entities"), incomingMcTypes, incomingMythicIds);
                    }
                }
            }

            return new ParsedDamageTriggers(
                    hasOutgoing, outgoingMatchAll, outgoingMcTypes, outgoingMythicIds,
                    hasIncoming, incomingMatchAll, incomingMcTypes, incomingMythicIds
            );
        }

        private static boolean readBool(Object v) {
            if (v instanceof Boolean b) return b;
            return v != null && Boolean.parseBoolean(String.valueOf(v));
        }

        private static void readEntities(Object ents, Set<EntityType> mcTypes, Set<String> mythicIds) {
            if (!(ents instanceof List<?> list)) return;

            for (Object o : list) {
                if (o == null) continue;
                String s = String.valueOf(o).trim();
                String[] parts = s.split(":", 2);

                if (parts.length == 2 && "minecraft".equalsIgnoreCase(parts[0])) {
                    try {
                        mcTypes.add(EntityType.valueOf(parts[1].toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {
                    }
                } else if (parts.length == 2 && "mythicmobs".equalsIgnoreCase(parts[0])) {
                    mythicIds.add(parts[1]);
                }
            }
        }
    }
}