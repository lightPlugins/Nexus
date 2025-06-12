package io.nexstudios.nexus.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.nexstudios.nexus.velocity.util.ProxyLogger;

/*
 * Copyright © 2023 LightStudios
 *
 * Name: Nexus
 * Description: Core plugin for Velocity plugins and all proxy plugins by Nexus.
 *
 * License:
 * This code is protected by copyright. It may not be copied,
 * modified, or distributed without explicit permission from LightStudios.
 */

@Plugin(id = "nexus", name = "Nexus Velocity", version = "0.1.0-SNAPSHOT",
        url = "https://example.org", description = "Core plugin for all Nexus plugins", authors = {"LightStudios"})

public class Nexus {

    private final ProxyServer server;
    private ProxyLogger logger;

    @Inject
    public Nexus(ProxyServer server ) {
        this.server = server;
        this.logger = new ProxyLogger("<reset>[<yellow>Nexus<reset>]");
        this.logger.info("Nexus Velocity plugin initialized successfully.");

    }
}
