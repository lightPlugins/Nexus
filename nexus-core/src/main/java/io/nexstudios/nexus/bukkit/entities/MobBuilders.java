package io.nexstudios.nexus.bukkit.entities;

import io.nexstudios.nexus.bukkit.platform.NexServices;

public class MobBuilders {
    private MobBuilders() {}

    public static MobBuilder create() {
        return NexServices.newMobBuilder();
    }
}
