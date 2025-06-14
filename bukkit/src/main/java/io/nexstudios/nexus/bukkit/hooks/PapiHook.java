package io.nexstudios.nexus.bukkit.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;

public class PapiHook {

    public Component translate(OfflinePlayer offlinePlayer, String key) {
        return MiniMessage.miniMessage().deserialize(PlaceholderAPI.setPlaceholders(offlinePlayer, key));
    }

}
