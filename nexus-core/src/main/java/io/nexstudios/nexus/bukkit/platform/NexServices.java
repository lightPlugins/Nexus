package io.nexstudios.nexus.bukkit.platform;

import io.nexstudios.nexus.bukkit.entities.MobBuilder;
import io.nexstudios.nexus.bukkit.entities.MobBuilderFactory;
import io.nexstudios.nexus.bukkit.hologram.HoloBuilder;
import io.nexstudios.nexus.bukkit.hologram.HoloBuilderFactory;
import io.nexstudios.nexus.bukkit.items.ItemBuilder;
import io.nexstudios.nexus.bukkit.items.ItemBuilderFactory;
import io.nexstudios.nexus.bukkit.items.ItemPacketModifier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class NexServices {

    private static volatile VersionedServiceRegistry REGISTRY;

    private NexServices() {}

    public static void init() {
        if (REGISTRY != null) return;

        String mcVersion = Bukkit.getMinecraftVersion(); // zB. "1.21.8"
        String[] candidates = getCandidates(mcVersion);

        VersionedServiceRegistry reg = new VersionedServiceRegistry();
        Throwable last = null;

        for (String providerClass : candidates) {
            try {
                Class<?> clazz = Class.forName(providerClass);
                if (!NmsServiceProvider.class.isAssignableFrom(clazz)) {
                    throw new IllegalStateException("ServicesProvider could not implement NmsServiceProvider: " + providerClass);
                }
                NmsServiceProvider provider = (NmsServiceProvider) clazz.getDeclaredConstructor().newInstance();
                provider.registerServices(reg);
                REGISTRY = reg;
                return;
            } catch (Throwable t) {
                last = t; // weiter zum nächsten Kandidaten
            }
        }

        throw new IllegalStateException(
                "Your Minecraft version " + mcVersion + " is not supported by Nexus.",
                last
        );
    }
    // ... existing code ...
    /**
     * Bequeme Initialisierung inkl. versionsspezifischer Runtime-Hooks (z. B. für Hologram-Builder).
     * Rufe diese Variante in deinem onEnable(plugin) auf.
     */
    public static void init(Plugin plugin) {
        init();
        initRuntime(plugin);
    }

    /**
     * Führt optionale Runtime-Initialisierung in der jeweiligen NMS-Implementierung aus.
     * Aktuell: versucht PaperHoloBuilder.initPlugin(plugin) aufzurufen (versionsneutral via Reflection).
     */
    public static void initRuntime(Plugin plugin) {
        if (plugin == null) return;
        try {
            // 1) Über die tatsächlich registrierte HoloBuilderFactory die Implementierung finden
            HoloBuilderFactory factory = get(HoloBuilderFactory.class);
            Class<?> impl = factory.getClass();
            try {
                var m = impl.getDeclaredMethod("initPlugin", Plugin.class);
                m.setAccessible(true);
                m.invoke(null, plugin); // statische Methode
                return;
            } catch (NoSuchMethodException ignored) {
                // kein initPlugin in dieser Klasse -> weiter mit Fallback
            }

            // 2) Fallback: anhand der aktiven MC-Version Kandidatenpfade bilden und initPlugin suchen
            String mcVersion = Bukkit.getMinecraftVersion(); // zB. "1.21.8"
            String[] parts = mcVersion.split("\\.");
            String major = parts.length > 0 ? parts[0] : "1";
            String minor = parts.length > 1 ? parts[1] : "21";
            String patch = parts.length > 2 ? parts[2] : "0";

            String basePlain = "io.nexstudios.internal.nms." + major + "_" + minor + "_" + patch + ".packets.PaperHoloBuilder";
            String baseV     = "io.nexstudios.internal.nms.v" + major + "_" + minor + "_" + patch + ".packets.PaperHoloBuilder";
            String baseVR    = "io.nexstudios.internal.nms.v" + major + "_" + minor + "_R" + patch + ".packets.PaperHoloBuilder";

            for (String cn : new String[]{ basePlain, baseV, baseVR }) {
                try {
                    Class<?> clazz = Class.forName(cn);
                    var m = clazz.getDeclaredMethod("initPlugin", Plugin.class);
                    m.setAccessible(true);
                    m.invoke(null, plugin);
                    return;
                } catch (Throwable ignored) {
                    // nächster Kandidat
                }
            }
        } catch (Throwable ignored) {
            // weich fehlschlagen: Features arbeiten weiter, ggf. ohne Scheduler-Fallbacks
        }
    }
    // ... existing code ...

    private static String [] getCandidates(String mcVersion) {
        String[] parts = mcVersion.split("\\.");
        String major = parts.length > 0 ? parts[0] : "1";
        String minor = parts.length > 1 ? parts[1] : "21";
        String patch = parts.length > 2 ? parts[2] : "0";

        // Kandidaten: ohne Präfix, mit v-Präfix, mit R-Präfix
        String pkgPlain = "io.nexstudios.internal.nms." + major + "_" + minor + "_" + patch;
        String pkgV     = "io.nexstudios.internal.nms.v" + major + "_" + minor + "_" + patch;
        String pkgVR    = "io.nexstudios.internal.nms.v" + major + "_" + minor + "_R" + patch;

        return new String[] {
                pkgPlain + ".services.ServicesProvider",
                pkgV     + ".services.ServicesProvider",
                pkgVR    + ".services.ServicesProvider"
        };
    }

    private static VersionedServiceRegistry registry() {
        VersionedServiceRegistry r = REGISTRY;
        if (r == null) throw new IllegalStateException("Services.init() could not be called!");
        return r;
    }

    public static <T> T get(Class<T> type) {
        T instance = registry().get(Objects.requireNonNull(type, "type"));
        if (instance == null) {
            throw new IllegalStateException("Could not register Service for " + type.getName());
        }
        return instance;
    }

    // Convenience: ItemBuilder
    public static ItemBuilder newItemBuilder() {
        ItemBuilderFactory factory = get(ItemBuilderFactory.class);
        ItemBuilder builder = factory.create();
        if (builder == null) {
            throw new IllegalStateException("ItemBuilderFactory#create() is null. Contact the developer");
        }
        return builder;
    }

    public static MobBuilder newMobBuilder() {
        MobBuilderFactory factory = get(MobBuilderFactory.class);
        MobBuilder builder = factory.create();
        if (builder == null) {
            throw new IllegalStateException("MobBuilderFactory#create() is null. Contact the developer");
        }
        return builder;
    }

    public static HoloBuilder newHoloBuilder() {
        HoloBuilderFactory factory = get(HoloBuilderFactory.class);
        HoloBuilder builder = factory.create();
        if (builder == null) {
            throw new IllegalStateException("HoloBuilderFactory#create() is null. Contact the developer");
        }
        return builder;
    }


    public static ItemPacketModifier newItemPacketModifier() {
        return get(ItemPacketModifier.class);
    }
}