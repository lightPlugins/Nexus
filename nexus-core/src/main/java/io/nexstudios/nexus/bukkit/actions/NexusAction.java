package io.nexstudios.nexus.bukkit.actions;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public interface NexusAction {

    JavaPlugin getPlugin();
    void execute(NexusActionContext context);
}
