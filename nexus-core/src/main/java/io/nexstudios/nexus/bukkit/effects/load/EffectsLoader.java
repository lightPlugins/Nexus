package io.nexstudios.nexus.bukkit.effects.load;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.*;
import io.nexstudios.nexus.bukkit.effects.meta.FilterRegistry;
import io.nexstudios.nexus.bukkit.effects.meta.TriggerRegistry;

import java.util.*;

public final class EffectsLoader {

    private EffectsLoader(){}

    public static List<EffectBinding> load(org.bukkit.configuration.ConfigurationSection root, EffectFactory factory) {
        if (root == null || !root.isList("effects")) return List.of();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawList = (List<Map<String, Object>>) root.getList("effects");
        if (rawList == null) return List.of();

        return fromRawList(rawList, factory);
    }

    public static List<EffectBinding> loadFromEffectsSection(org.bukkit.configuration.ConfigurationSection effectsSection, EffectFactory factory) {
        if (effectsSection == null || !effectsSection.isList("")) return List.of();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawList = (List<Map<String, Object>>) effectsSection.getList("");
        if (rawList == null) return List.of();

        return fromRawList(rawList, factory);
    }

    private static List<EffectBinding> fromRawList(List<Map<String, Object>> rawList, EffectFactory factory) {
        List<EffectBinding> out = new ArrayList<>();
        for (Map<String, Object> raw : rawList) {
            if (raw == null) continue;

            String id = str(raw.get("id"));
            if (id == null) continue;

            NexusEffect effect = factory.create(id, new EffectConfig(raw));
            if (effect == null) continue;

            if (!validateSupports(factory, id, raw)) {
                // Fehler geloggt -> Binding überspringen
                continue;
            }

            List<TriggerSpec> triggers = parseTriggers(raw.get("trigger"));
            out.add(new EffectBinding(effect, triggers, raw));
        }
        return out;
    }

    private static boolean validateSupports(EffectFactory factory, String effectId, Map<String, Object> raw) {
        Set<String> allowedTriggers = factory.getAllowedTriggerIds(effectId);
        Set<String> allowedFilters  = factory.getAllowedFilterIds(effectId);

        // Trigger validieren
        Object trigRoot = raw.get("trigger");
        if (trigRoot instanceof List<?> tlist) {
            for (Object o : tlist) {
                if (!(o instanceof Map<?, ?> m)) continue;
                String tidRaw = normalize(str(m.get("id")));
                if (tidRaw == null || !TriggerRegistry.isRegistered(tidRaw)) {
                    NexusPlugin.nexusLogger.error(List.of(
                            "Effect binding skipped: unknown trigger in effect '" + effectId + "'.",
                            "Trigger used: '" + tidRaw + "'"
                    ));
                    return false;
                }
                if (allowedTriggers != null && !allowedTriggers.contains(tidRaw)) {
                    NexusPlugin.nexusLogger.error(List.of(
                            "Effect binding skipped: unsupported trigger for effect '" + effectId + "'.",
                            "Trigger used: '" + tidRaw + "', Allowed: " + allowedTriggers
                    ));
                    return false;
                }
            }
        } else {
            // Trigger fehlen: nur OK, wenn keine Restriktionen (optional – streng bleiben)
            if (allowedTriggers != null && !allowedTriggers.isEmpty()) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Effect binding skipped: no trigger defined for effect '" + effectId + "', but effect restricts triggers.",
                        "Allowed triggers: " + allowedTriggers
                ));
                return false;
            }
        }

        // Filter validieren
        Object filtersRoot = raw.get("filters");
        if (filtersRoot instanceof List<?> flist) {
            for (Object o : flist) {
                if (!(o instanceof Map<?, ?> m)) continue;
                String fidRaw = normalize(str(m.get("id")));
                if (fidRaw == null || !FilterRegistry.isRegistered(fidRaw)) {
                    NexusPlugin.nexusLogger.error(List.of(
                            "Effect binding skipped: unknown filter in effect '" + effectId + "'.",
                            "Filter used: '" + fidRaw + "'"
                    ));
                    return false;
                }
                if (allowedFilters != null && !allowedFilters.contains(fidRaw)) {
                    NexusPlugin.nexusLogger.error(List.of(
                            "Effect binding skipped: unsupported filter for effect '" + effectId + "'.",
                            "Filter used: '" + fidRaw + "', Allowed: " + allowedFilters
                    ));
                    return false;
                }
            }
        } else if (filtersRoot != null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Effect binding skipped: invalid 'filters' section for effect '" + effectId + "'.",
                    "Expected a list of filters."
            ));
            return false;
        }

        return true;
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private static List<TriggerSpec> parseTriggers(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        List<TriggerSpec> specs = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> map)) continue;
            Map<String, Object> m = new HashMap<>();
            map.forEach((k, v) -> m.put(String.valueOf(k), v));
            String tid = str(m.get("id"));
            if (tid == null) continue;
            m.remove("id"); // Rest sind Optionen
            specs.add(new TriggerSpec(tid, m));
        }
        return specs;
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}






