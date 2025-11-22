package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

@CommandAlias("nexus")
public class ItemsAdderCommand extends BaseCommand {

    @Subcommand("itemsadder give")
    @CommandPermission("nexus.command.admin.reload")
    @Description("Reloads the plugin configuration and settings.")
    public void getItem(CommandSender sender, String item) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }

        if(NexusPlugin.getInstance().itemsAdderHook == null) {
            NexusPlugin.getInstance().getMessageSender().send(sender, "ItemsAdder hook is not registered.");
            return;
        }

        ItemStack stack = NexusPlugin.getInstance().itemsAdderHook.getItem(item);

        if(stack == null) {
            NexusPlugin.getInstance().getMessageSender().send(sender, "Item with id: " + item + " does not exist.");
            return;
        }

        player.getInventory().addItem(stack);

    }
}
