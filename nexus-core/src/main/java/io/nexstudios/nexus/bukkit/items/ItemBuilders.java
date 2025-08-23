package io.nexstudios.nexus.bukkit.items;

import io.nexstudios.nexus.bukkit.platform.NexServices;

public final class ItemBuilders {
    private ItemBuilders() {}

    public static ItemBuilder create() {
        return NexServices.newItemBuilder();
    }
}
