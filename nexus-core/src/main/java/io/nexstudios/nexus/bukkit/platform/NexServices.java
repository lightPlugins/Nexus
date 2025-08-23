package io.nexstudios.nexus.bukkit.platform;

import io.nexstudios.nexus.bukkit.items.ItemBuilder;
import io.nexstudios.nexus.bukkit.items.ItemBuilderFactory;
import org.bukkit.Bukkit;

import java.util.Objects;

/**
 * Zentrale Fassade zum Zugriff auf versionsabhängige Services.
 * Beim Serverstart einmalig initialisieren, danach überall nutzen.
 */
public final class NexServices {

    private static volatile VersionedServiceRegistry REGISTRY;

    private NexServices() {}

    public static void init() {
        if (REGISTRY != null) return;

        String mcVersion = Bukkit.getMinecraftVersion(); // z. B. "1.21.8"
        String[] candidates = getCandidates(mcVersion);

        VersionedServiceRegistry reg = new VersionedServiceRegistry();
        Throwable last = null;

        for (String providerClass : candidates) {
            try {
                Class<?> clazz = Class.forName(providerClass);
                if (!NmsServiceProvider.class.isAssignableFrom(clazz)) {
                    throw new IllegalStateException("ServicesProvider implementiert NmsServiceProvider nicht: " + providerClass);
                }
                NmsServiceProvider provider = (NmsServiceProvider) clazz.getDeclaredConstructor().newInstance();
                provider.registerServices(reg);
                REGISTRY = reg;
                return; // Erfolg
            } catch (Throwable t) {
                last = t; // weiter zum nächsten Kandidaten
            }
        }

        throw new IllegalStateException(
                "Keine kompatible ServicesProvider-Implementierung gefunden für Minecraft-Version: " + mcVersion,
                last
        );
    }

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
        if (r == null) throw new IllegalStateException("Services.init() wurde nicht aufgerufen!");
        return r;
    }

    public static <T> T get(Class<T> type) {
        T instance = registry().get(Objects.requireNonNull(type, "type"));
        if (instance == null) {
            throw new IllegalStateException("Kein Service für Typ registriert: " + type.getName());
        }
        return instance;
    }

    // Convenience: ItemBuilder
    public static ItemBuilder newItemBuilder() {
        ItemBuilderFactory factory = get(ItemBuilderFactory.class);
        ItemBuilder builder = factory.create();
        if (builder == null) {
            throw new IllegalStateException("ItemBuilderFactory#create() lieferte null");
        }
        return builder;
    }
}