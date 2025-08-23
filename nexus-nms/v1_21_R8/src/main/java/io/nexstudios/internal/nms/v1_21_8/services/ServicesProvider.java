package io.nexstudios.internal.nms.v1_21_8.services;

import io.nexstudios.internal.nms.v1_21_8.items.PaperItemBuilder;
import io.nexstudios.nexus.bukkit.items.ItemBuilderFactory;
import io.nexstudios.nexus.bukkit.platform.NmsServiceProvider;
import io.nexstudios.nexus.bukkit.platform.VersionedServiceRegistry;

public final class ServicesProvider implements NmsServiceProvider {

    @Override
    public void registerServices(VersionedServiceRegistry registry) {
        // ItemBuilder-Factory registrieren (pro Aufruf eine neue Builder-Instanz)
        registry.registerSupplier(ItemBuilderFactory.class, PaperItemBuilder::new);

        // Hier später weitere versionierte Services registrieren (Particles, Packets, usw.)
    }
}

