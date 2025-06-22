package io.nexstudios.nexus.bukkit.hooks;

import io.nexstudios.nexus.common.logging.NexusLogger;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@Getter
public class VaultHook {

    public Economy economy;
    private final NexusLogger logger;

    public VaultHook(JavaPlugin plugin, NexusLogger logger) {
        this.logger = logger;
        this.economy = setupVault(plugin);

    if(this.economy == null) {
            logger.error(List.of(
                    "While trying to hook into Vault Economy, the provider was null.",
                    "Please ensure that Vault is installed and enabled on your server.",
                    "Also, make sure you installed an Economy plugin that is compatible with Vault."
            ));
        } else {
            logger.info("Vault Economy successfully hooked.");
        }
    }

    private Economy setupVault(JavaPlugin plugin) {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp == null) {
            logger.error(List.of(
                    "Registered service provider for Vault Economy is null.",
                    "Please ensure that Vault is installed and enabled on your server.",
                    "Also, make sure that the Economy plugin is properly registered in Vault."
            ));
            return null;
        }
        return rsp.getProvider();
    }
}
