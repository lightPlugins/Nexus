package io.nexstudios.internal.nms.v1_21_10.services;

import io.nexstudios.internal.nms.v1_21_10.items.PaperItemBuilder;
import io.nexstudios.internal.nms.v1_21_10.packets.ItemPacketModifierImpl;
import io.nexstudios.nexus.bukkit.items.ItemBuilderFactory;
import io.nexstudios.nexus.bukkit.items.ItemPacketModifier;
import io.nexstudios.nexus.bukkit.platform.NmsServiceProvider;
import io.nexstudios.nexus.bukkit.platform.VersionedServiceRegistry;

public final class ServicesProvider implements NmsServiceProvider {

    @Override
    public void registerServices(VersionedServiceRegistry registry) {
        // item builder
        registry.registerSupplier(ItemBuilderFactory.class, PaperItemBuilder::new);
        // item display/lore packet modifier
        registry.registerSupplier(ItemPacketModifier.class, ItemPacketModifierImpl::new);

    }

}

