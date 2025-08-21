package io.nexstudios.nexus.bukkit.effects;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.load.EffectConfig;
import io.nexstudios.nexus.bukkit.effects.load.EffectsLoader;
import io.nexstudios.nexus.bukkit.effects.meta.FilterRegistry;
import io.nexstudios.nexus.bukkit.effects.meta.TriggerRegistry;
import io.nexstudios.nexus.bukkit.effects.runtime.EffectBindingRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Function;

public final class NexusEffectsApi {

    private NexusEffectsApi() {}

    private static EffectFactory factory() {
        return Objects.requireNonNull(NexusPlugin.getInstance().getEffectFactory(), "EffectFactory not initialized");
    }
    private static EffectBindingRegistry registry() {
        return Objects.requireNonNull(NexusPlugin.getInstance().getBindingRegistry(), "EffectBindingRegistry not initialized");
    }

    private static final List<EffectBinding> CORE = new ArrayList<>();
    private static final Map<String, List<EffectBinding>> BY_NAMESPACE = new LinkedHashMap<>();

    public static boolean registerTriggerType(String id) { return TriggerRegistry.register(id); }
    public static boolean registerFilterType(String id) { return FilterRegistry.register(id); }

    public static boolean registerEffectType(String id, Function<EffectConfig, NexusEffect> builder) {
        return factory().register(id, builder);
    }
    public static boolean registerEffectType(String id,
                                             Function<EffectConfig, NexusEffect> builder,
                                             Set<String> allowedTriggerIds,
                                             Set<String> allowedFilterIds) {
        return factory().register(id, builder, allowedTriggerIds, allowedFilterIds);
    }

    public static List<EffectBinding> registerBindingsFromSection(ConfigurationSection rootSection) {
        List<EffectBinding> loaded = EffectsLoader.load(rootSection, factory());
        if (!loaded.isEmpty()) {
            CORE.addAll(loaded);
            rebuild();
        }
        return loaded;
    }

    public static List<EffectBinding> registerBindingsFromSection(Plugin owner, ConfigurationSection rootSection) {
        String ns = owner == null ? "external" : owner.getName().toLowerCase(Locale.ROOT);
        List<EffectBinding> loaded = EffectsLoader.load(rootSection, factory());
        if (!loaded.isEmpty()) {
            BY_NAMESPACE.computeIfAbsent(ns, k -> new ArrayList<>()).addAll(loaded);
            rebuild();
        }
        return loaded;
    }

    // NEU: Bereits geladene Bindings (z. B. via EffectsLoader mit eigener Factory) in einen Namespace hängen
    public static void addBindings(Plugin owner, List<EffectBinding> toAdd) {
        if (owner == null || toAdd == null || toAdd.isEmpty()) return;
        String ns = owner.getName().toLowerCase(Locale.ROOT);
        BY_NAMESPACE.computeIfAbsent(ns, k -> new ArrayList<>()).addAll(toAdd);
        rebuild();
    }

    public static void replaceExternalBindings(String namespace, List<EffectBinding> newBindings) {
        String ns = safeNs(namespace);
        if (newBindings == null || newBindings.isEmpty()) {
            BY_NAMESPACE.remove(ns);
        } else {
            BY_NAMESPACE.put(ns, new ArrayList<>(newBindings));
        }
        rebuild();
    }

    public static void removeExternalNamespace(String namespace) {
        String ns = safeNs(namespace);
        BY_NAMESPACE.remove(ns);
        rebuild();
    }

    public static void replaceExternalBindings(Plugin owner, List<EffectBinding> newBindings) {
        String ns = owner == null ? "external" : owner.getName().toLowerCase(Locale.ROOT);
        replaceExternalBindings(ns, newBindings);
    }

    public static void removeExternalNamespace(Plugin owner) {
        String ns = owner == null ? "external" : owner.getName().toLowerCase(Locale.ROOT);
        removeExternalNamespace(ns);
    }

    public static void replaceBindings(List<EffectBinding> newBindings) {
        CORE.clear();
        BY_NAMESPACE.clear();
        if (newBindings != null && !newBindings.isEmpty()) {
            BY_NAMESPACE.put("external", new ArrayList<>(newBindings));
        }
        rebuild();
    }

    public static List<EffectBinding> getActiveBindings() {
        return List.copyOf(registry().getBindings());
    }

    private static void rebuild() {
        int size = CORE.size();
        for (var e : BY_NAMESPACE.values()) size += e.size();

        List<EffectBinding> merged = new ArrayList<>(size);
        merged.addAll(CORE);
        for (var e : BY_NAMESPACE.values()) merged.addAll(e);

        registry().setBindings(merged);
    }

    private static String safeNs(String ns) {
        if (ns == null || ns.isBlank()) return "external";
        return ns.trim().toLowerCase(Locale.ROOT);
    }
}







