package io.nexstudios.internal.nms.v1_21_11.services;

import io.nexstudios.internal.nms.v1_21_11.dialog.PaperDialogBuilderFactory;
import io.nexstudios.internal.nms.v1_21_11.entities.PaperMobBuilder;
import io.nexstudios.internal.nms.v1_21_11.items.PaperItemBuilder;
import io.nexstudios.internal.nms.v1_21_11.packets.ItemPacketModifierImpl;
import io.nexstudios.internal.nms.v1_21_11.packets.PaperFakeBlockBreak;
import io.nexstudios.internal.nms.v1_21_11.packets.PaperHoloBuilder;
import io.nexstudios.nexus.bukkit.dialog.NexDialogBuilderFactory;
import io.nexstudios.nexus.bukkit.entities.MobBuilderFactory;
import io.nexstudios.nexus.bukkit.fakebreak.FakeBlockBreakNms;
import io.nexstudios.nexus.bukkit.hologram.HoloBuilderFactory;
import io.nexstudios.nexus.bukkit.items.ItemBuilderFactory;
import io.nexstudios.nexus.bukkit.items.ItemPacketModifier;
import io.nexstudios.nexus.bukkit.platform.NmsServiceProvider;
import io.nexstudios.nexus.bukkit.platform.VersionedServiceRegistry;

public final class ServicesProvider implements NmsServiceProvider {

    @Override
    public void registerServices(VersionedServiceRegistry registry) {
        registry.registerSupplier(ItemBuilderFactory.class, PaperItemBuilder::new);
        registry.registerSupplier(MobBuilderFactory.class, PaperMobBuilder::new);
        registry.registerSupplier(HoloBuilderFactory.class, PaperHoloBuilder::new);
        registry.registerSupplier(NexDialogBuilderFactory.class, PaperDialogBuilderFactory::new);
        registry.registerSupplier(FakeBlockBreakNms.class, PaperFakeBlockBreak::new);
        registry.registerSupplier(ItemPacketModifier.class, ItemPacketModifierImpl::new);

    }
}

