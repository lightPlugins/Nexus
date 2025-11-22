package io.nexstudios.nexus.bukkit.actions.handler;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import io.nexstudios.nexus.bukkit.utils.NexusStringMath;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class ActionEXP implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation) {

        if(!data.validate(data.getData().get("amount"), String.class)) {
            NexusPlugin.nexusLogger.error("Invalid command data.");
            NexusPlugin.nexusLogger.error("Missing 'amount' parameter for action exp");
            return;
        }

        String translatedExpression = "1";

        if(NexusPlugin.getInstance().getPapiHook() != null) {
            translatedExpression = NexusPlugin.getInstance().getPapiHook()
                    .translateIntoString(player, (String) data.getData().get("amount"));
        }

        double amountExpression = NexusStringMath.evaluateExpression(translatedExpression);
        boolean dropExp = (boolean) data.getData().getOrDefault("drop", false);

        if(!dropExp) {
            player.giveExp((int) amountExpression);
            return;
        }

        if(targetLocation == null) {
            NexusPlugin.nexusLogger.error("Target location is null while parameter drop is true!");
            return;
        }
        ExperienceOrb orb = targetLocation.getWorld().spawn(targetLocation, ExperienceOrb.class);
        orb.setExperience((int) amountExpression);
    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation, Map<String, Object> params) {
        execute(player, data, targetLocation);
    }

    @Override
    public void execute(Player player, ActionData data, Location location, TagResolver tagResolver) {
        execute(player, data, location);
    }
}
