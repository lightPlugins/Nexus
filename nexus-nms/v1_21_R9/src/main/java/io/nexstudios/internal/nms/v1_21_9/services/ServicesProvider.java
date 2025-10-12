package io.nexstudios.internal.nms.v1_21_9.services;

import io.nexstudios.internal.nms.v1_21_9.items.PaperItemBuilder;
import io.nexstudios.nexus.bukkit.items.ItemBuilderFactory;
import io.nexstudios.nexus.bukkit.platform.NmsServiceProvider;
import io.nexstudios.nexus.bukkit.platform.VersionedServiceRegistry;

public final class ServicesProvider implements NmsServiceProvider {

    @Override
    public void registerServices(VersionedServiceRegistry registry) {
        registry.registerSupplier(ItemBuilderFactory.class, PaperItemBuilder::new);
    }
}

