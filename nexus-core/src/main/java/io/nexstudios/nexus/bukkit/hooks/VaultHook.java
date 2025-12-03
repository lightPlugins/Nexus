package io.nexstudios.nexus.bukkit.hooks;

import io.nexstudios.nexus.bukkit.utils.NexusLogger;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@Getter
public class VaultHook implements Listener {

    private Economy economy;
    private final NexusLogger logger;
    private final JavaPlugin plugin;

    public VaultHook(JavaPlugin plugin, NexusLogger logger) {
        this.logger = logger;
        this.plugin = plugin;

        this.economy = setupVault();
        if (this.economy != null) {
            logger.info("<yellow>Vault Economy<reset> hook registered successfully.");
        } else {
            logger.warning("Vault Economy provider not available yet. Waiting for service registration ...");
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() != Economy.class) {
            return;
        }

        if (this.economy != null) {
            return;
        }

        economy = setupVault();

        if(economy == null) {
            logger.error(List.of(
                    "Registered service provider for Vault Economy is null.",
                    "Please ensure that Vault is installed and enabled on your server.",
                    "Also, make sure that the Economy plugin is properly registered in Vault."
            ));
        } else {
            logger.info("<yellow>Vault Economy<reset> hook registered successfully.");
        }
    }

    private Economy setupVault() {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp == null) {
            return null;
        }
        return rsp.getProvider();
    }
}
