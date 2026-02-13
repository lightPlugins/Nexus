package io.nexstudios.proxy.velocity;


import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.nexstudios.proxy.velocity.commands.NexusProxyCommand;
import io.nexstudios.proxy.velocity.file.ProxyConfigUpdater;
import io.nexstudios.proxy.velocity.file.ProxyFile;
import io.nexstudios.proxy.velocity.util.VelocityLogger;
import lombok.Getter;

import java.nio.file.Path;

@Plugin(
        id= "nexus",
        name = "Nexus",
        version = "1.0.0-snapshot",
        authors = {"NexStudios", "light"},
        description = "Nexus Core plugin for Velocity Proxy"
)

@Getter
public class NexVelocity {

    @Getter
    private static NexVelocity instance;
    private final VelocityLogger velocityLogger;
    private final ProxyServer proxyServer;
    private final Path dataDirectory;

    private ProxyFile settingsFile;

    @Inject
    public NexVelocity(ProxyServer proxyServer, @DataDirectory Path dataDirectory) {
        instance = this;
        this.proxyServer = proxyServer;
        this.dataDirectory = dataDirectory;

        velocityLogger = new VelocityLogger(proxyServer, "<reset>[<yellow>Nexus<reset>]", true, 99, "<yellow>");
        logAsciiLogo();
        velocityLogger.info("Loading Nexus on Velocity ...");

        onReload();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        velocityLogger.info("Registering commands ...");
        registerCommands();
        velocityLogger.info("Nexus on Velocity has been initialized!");
    }

    public void onReload() {
        velocityLogger.info("Reloading Nexus on Velocity ...");
        loadNexusFiles();
    }

    private void loadNexusFiles() {
        velocityLogger.info("Loading Nexus files ...");
        settingsFile = new ProxyFile(
                dataDirectory,
                "proxy-settings.yml",
                velocityLogger,
                getClass().getClassLoader()
        );

        int added = ProxyConfigUpdater.updateKeepingComments(
                dataDirectory.resolve("proxy-settings.yml"),
                getClass().getClassLoader(),
                "proxy-settings.yml"
        );

        velocityLogger.info("Added %d missing top-level keys to proxy-settings.yml".formatted(added));

    }

    private void registerCommands() {
        var commandManager = proxyServer.getCommandManager();

        CommandMeta meta = commandManager.metaBuilder("nexusproxy")
                .aliases("nproxy")
                .build();

        commandManager.register(meta, new NexusProxyCommand(this).build());
    }

    private void logAsciiLogo() {
        String version = "1.0.0-SNAPSHOT";
        String paperVersion = getProxyServer().getVersion().getVersion();
        velocityLogger.info("""
                     
                     <gradient:yellow:white>▄▄▄    ▄▄▄  ▄▄▄▄▄▄▄ ▄▄▄   ▄▄▄ ▄▄▄  ▄▄▄  ▄▄▄▄▄▄▄</gradient>\s
                     <gradient:yellow:white>████▄  ███ ███▀▀▀▀▀ ████▄████ ███  ███ █████▀▀▀</gradient>\s
                     <gradient:yellow:white>███▀██▄███ ███▄▄     ▀█████▀  ███  ███  ▀████▄ </gradient>\s
                     <gradient:yellow:white>███  ▀████ ███      ▄███████▄ ███▄▄███    ▀████</gradient>\s
                     <gradient:yellow:white>███    ███ ▀███████ ███▀ ▀███ ▀██████▀ ███████▀</gradient>\s
            
               <gray>The most powerful core plugin that drives the entire <yellow>Nex</yellow><gray> series.</gray>
               <gray>Version: <yellow>%s</yellow></gray><reset>
               <gray>Author: <yellow>light (NexStudios)
               <gray>Paper Version: %s</gray><reset>
            """.formatted(version, paperVersion));
    }
}
