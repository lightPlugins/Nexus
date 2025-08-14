package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariables;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("nexus")
public class StatCommand extends BaseCommand {

    @Subcommand("setstat")
    @Description("Setzt deinen persönlichen stat-level Wert für Effekte.")
    @CommandPermission("nexus.command.stat.set")
    public void onCommand(CommandSender sender, String amount) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl ist nur für Spieler.");
            return;
        }

        // Eingabe validieren
        double value;
        try {
            value = Double.parseDouble(amount);
        } catch (NumberFormatException ex) {
            player.sendMessage(Component.text("Bitte gib eine gültige Zahl an, z. B. 25 oder 12.5"));
            return;
        }

        // Als String ablegen (Evaluator liest Strings)
        PlayerVariables.set(player.getUniqueId(), "stat-level", String.valueOf(value));
        player.sendMessage(Component.text("stat-level gesetzt auf " + value));
    }
}

