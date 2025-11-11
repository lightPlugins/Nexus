package io.nexstudios.internal.nms.v1_21_8.services;

import io.nexstudios.internal.nms.v1_21_8.entities.PaperMobBuilder;
import io.nexstudios.internal.nms.v1_21_8.items.PaperItemBuilder;
import io.nexstudios.internal.nms.v1_21_8.packets.PaperHoloBuilder;
import io.nexstudios.nexus.bukkit.entities.MobBuilderFactory;
import io.nexstudios.nexus.bukkit.hologram.HoloBuilderFactory;
import io.nexstudios.nexus.bukkit.items.ItemBuilderFactory;
import io.nexstudios.nexus.bukkit.platform.NmsServiceProvider;
import io.nexstudios.nexus.bukkit.platform.VersionedServiceRegistry;

public final class ServicesProvider implements NmsServiceProvider {

    @Override
    public void registerServices(VersionedServiceRegistry registry) {
        registry.registerSupplier(ItemBuilderFactory.class, PaperItemBuilder::new);
        registry.registerSupplier(MobBuilderFactory.class, PaperMobBuilder::new);
        registry.registerSupplier(HoloBuilderFactory.class, PaperHoloBuilder::new);

    }
}

