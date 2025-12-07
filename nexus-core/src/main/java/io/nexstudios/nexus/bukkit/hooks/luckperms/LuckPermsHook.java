package io.nexstudios.nexus.bukkit.hooks.luckperms;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;

public class LuckPermsHook {


    public LuckPerms getLuckPermsAPI() {

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            return provider.getProvider();

        }
        NexusPlugin.nexusLogger.error(List.of(
                "While LuckPerms was found on the server,",
                "we were unable to retrieve the API instance.",
                "Is LuckPerms enabled without errors?"
        ));
        return null;
    }

}
