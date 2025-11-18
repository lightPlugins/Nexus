package io.nexstudios.nexus.bukkit.hooks.worldguard.flags;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import io.nexstudios.nexus.bukkit.NexusPlugin;

import java.util.List;

public class WoodStripFlag {

    public static BooleanFlag NEXUS_DISABLE_WOOD_STRIPPING;

    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            BooleanFlag flag = new BooleanFlag("nexus-disable-wood-stripping");
            Flag<?> existing = registry.get(flag.getName());

            if (existing == null) {
                registry.register(flag);
                NEXUS_DISABLE_WOOD_STRIPPING = flag;
            } else if (existing instanceof BooleanFlag booleanFlag) {
                // Flag existiert bereits im Registry, wir übernehmen sie
                NEXUS_DISABLE_WOOD_STRIPPING = booleanFlag;
            } else {
                // anderer Typ mit gleichem Namen -> nichts tun / ggf. loggen
                NexusPlugin.nexusLogger.error(List.of(
                        "Flag with name '" + flag.getName() + "' already exists but is not a BooleanFlag"
                ));
            }
        } catch (FlagConflictException e) {
            // Konflikt mit bereits registrierter Flag – versuche sie zu referenzieren
            Flag<?> existing = registry.get("nexus-disable-wood-stripping");
            if (existing instanceof BooleanFlag booleanFlag) {
                NEXUS_DISABLE_WOOD_STRIPPING = booleanFlag;
            }
        }
    }

    private WoodStripFlag() {
        // utility class
    }
}