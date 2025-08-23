package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

@CommandAlias("nexus")
public class InvOpenCommand extends BaseCommand {
    @Subcommand("inv open")
    @CommandCompletion("@inventories")
    @CommandPermission("nexus.command.admin.reload")
    @Description("Reloads the plugin configuration and settings.")
    public void onReload(CommandSender sender, String inventory) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können dieses Inventar öffnen.");
            return;
        }

        String namespace = NexusPlugin.getInstance().getName().toLowerCase(Locale.ROOT);

        // Beispiel: dynamische Filler-Liste bauen
        java.util.List<ItemStack> filler = java.util.List.of(
                ItemStack.of(Material.GOLD_INGOT),
                ItemStack.of(Material.DIAMOND),
                ItemStack.of(Material.EMERALD)
        );

        // Beispiel: zusätzliches Info-Item in feste Slots
        ItemStack info = ItemStack.of(Material.PAPER);

        // Öffnen + dynamische Inhalte/Handler binden
        NexusPlugin.getInstance().getInvService()
                .menu(namespace, inventory)
                .forPlayer(player)
                // Filler/Body in einem 1-basierten Slotbereich befüllen, zentriert
                .populateFiller(filler, 11, 33, InvAlignment.LEFT)
                .onClick((event, ctx) -> {
                    // Klick auf Filler (Body). 2. Item (Index 1) -> heilen
                    player.sendMessage(Component.text("Clicked " + ctx.bodyIndex()));
                    if (ctx.isBody() && ctx.bodyIndex() != null && ctx.bodyIndex() == 1) {
                        Player p = ctx.player();
                        double max = p.getMaxHealth();
                        p.setHealth(Math.min(max, p.getHealth() + 4.0D));
                        p.sendMessage("Filler: 2. Eintrag geklickt – geheilt!");
                    }
                })
                // Required-Handler gezielt für 'heal' aus der Config
                .onRequireClick("heal", (event, ctx) -> {
                    org.bukkit.entity.Player p = ctx.player();
                    double max = p.getMaxHealth();
                    p.setHealth(Math.min(max, p.getHealth() + 8.0D));
                    p.sendMessage("Required: Heal ausgeführt (+4 Herzen).");
                })
                // Optional: Custom-Handler (falls in der Datei vorhanden)
                .onCustomClick("my-custom-item", (event, ctx) -> ctx.player().sendMessage("Custom-Item geklickt."))
                // Optional: zusätzliches Navi-Handling (Standardaktionen bleiben erhalten)
                .onNavigationClick((event, ctx) -> ctx.player().sendMessage("Navigation: " + ctx.namespace()))
                // Jetzt öffnen
                .open();
    }

}
