package io.nexstudios.nexus.bukkit.actions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import io.nexstudios.nexus.bukkit.actions.NexusActionContext;
import io.nexstudios.nexus.bukkit.utils.NexusStringMath;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ActionEXP implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(NexusActionContext context) {
        ActionData data = context.data();
        Player player = context.requirePlayer();
        Location targetLocation = context.location();

        Object amountObj = data.getData().get("amount");
        if (!(amountObj instanceof String)) {
            NexusPlugin.nexusLogger.error("Invalid exp action data.");
            NexusPlugin.nexusLogger.error("Missing or non-string 'amount' parameter for action exp");
            return;
        }

        String translatedExpression = "1";
        if (NexusPlugin.getInstance().getPapiHook() != null) {
            translatedExpression = NexusPlugin.getInstance().getPapiHook()
                    .translateIntoString(player, (String) amountObj);
        }

        double amountExpression = NexusStringMath.evaluateExpression(translatedExpression);
        boolean dropExp = (boolean) data.getData().getOrDefault("drop", false);

        if (!dropExp) {
            player.giveExp((int) amountExpression);
            return;
        }

        if (targetLocation == null) {
            NexusPlugin.nexusLogger.error("Target location is null while parameter 'drop' is true for action exp!");
            return;
        }

        ExperienceOrb orb = targetLocation.getWorld().spawn(targetLocation, ExperienceOrb.class);
        orb.setExperience((int) amountExpression);
    }
}