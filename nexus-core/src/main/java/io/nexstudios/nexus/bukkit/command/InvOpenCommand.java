package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.inv.api.NexFillerEntry;
import io.nexstudios.nexus.bukkit.inv.api.NexMenuSession;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import io.nexstudios.nexus.bukkit.items.ItemHideFlag;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
        //openMenu(player, namespace, inventory, 11, 33);
        openMenuWithDelayedExecutorCF(player, namespace, inventory, 11, 33);
    }

    // Java
    public void openMenu(Player player, String namespace, String inventory, int startSlot1b, int endSlot1b) {
        // Build items (initial render state)
        ItemStack diamond = NexServices.newItemBuilder()
                .material(Material.DIAMOND)
                .stackSize(1)
                .build();

        boolean fullHealth = player.getHealth() >= player.getMaxHealth();

        ItemStack emeraldBase = NexServices.newItemBuilder()
                .material(Material.EMERALD)
                .stackSize(1)
                .build();

        ItemStack emeraldInitial = fullHealth
                ? NexServices.newItemBuilder()
                .itemStack(emeraldBase)
                .enchantments(java.util.Map.of(Enchantment.FORTUNE, 1)) // glow
                .hideFlags(Set.of(io.nexstudios.nexus.bukkit.items.ItemHideFlag.HIDE_ENCHANTS))
                .lore(List.of(Component.text("Already at full health")))
                .build()
                : emeraldBase;

        // First: get a binding by populating plain items (ensures future single-slot updates)
        NexMenuSession session = NexusPlugin.getInstance()
                .getInvService()
                .menu(namespace, inventory);

        NexMenuSession.FillerBinding binding = session.populateFiller(
                List.of(diamond, emeraldInitial),
                startSlot1b, endSlot1b,
                InvAlignment.LEFT
        );

        // Then: attach per-item handlers via entries (use binding inside handlers)
        List<NexFillerEntry> entries = new ArrayList<>();

        // Diamond -> set 1500 tick cooldown
        entries.add(NexFillerEntry.of(diamond, (event, ctx) -> {
            ctx.player().setCooldown(Material.DIAMOND, 1500);
            ctx.player().sendMessage(Component.text("Diamond cooldown set to 1500 ticks."));
        }));

        // Emerald -> heal; if now full, glow + disable (by early-return on next clicks)
        entries.add(NexFillerEntry.of(emeraldInitial, (event, ctx) -> {
            Integer bodyIdx = ctx.bodyIndex();
            if (bodyIdx == null) return;

            Player p = ctx.player();
            double max = p.getMaxHealth();

            // Already full -> ignore clicks
            if (p.getHealth() >= max) return;

            // Heal
            p.setHealth(Math.min(max, p.getHealth() + 4.0D));
            p.sendMessage(Component.text("You have been healed (+4 health)."));

            // If now full -> update only this body item (glow + lore)
            if (p.getHealth() >= max) {
                binding.update(bodyIdx, current -> {
                    if (current == null) return null;
                    return NexServices.newItemBuilder()
                            .itemStack(current)
                            .enchantments(java.util.Map.of(Enchantment.FORTUNE, 1)) // glow
                            .hideFlags(Set.of(ItemHideFlag.HIDE_ENCHANTS))
                            .lore(List.of(Component.text("Already at full health")))
                            .build();
                });
            }
        }));

        // Render entries (handlers) and open
        session.populateFillerEntries(entries, startSlot1b, endSlot1b, InvAlignment.LEFT).openFor(player);
    }

    public void openMenuWithDelayedExecutorCF(Player player, String namespace, String inventory, int startSlot1b, int endSlot1b) {
        NexMenuSession session = NexusPlugin.getInstance()
                .getInvService()
                .menu(namespace, inventory);

        session.openFor(player);

        CompletableFuture.supplyAsync(() -> {
            // Build entries after delay, still off-thread
            List<NexFillerEntry> entries = new ArrayList<>();

            ItemStack diamond = NexServices.newItemBuilder()
                    .material(Material.DIAMOND)
                    .stackSize(1)
                    .build();
            entries.add(NexFillerEntry.of(diamond, (event, ctx) -> {
                ctx.player().setCooldown(Material.DIAMOND, 1500);
                ctx.player().sendMessage(Component.text("Diamond cooldown set to 1500 ticks."));
            }));

            boolean full = player.getHealth() >= player.getMaxHealth();
            ItemStack emeraldBase = NexServices.newItemBuilder().material(Material.EMERALD).stackSize(1).build();
            ItemStack emeraldInitial = full
                    ? NexServices.newItemBuilder()
                    .itemStack(emeraldBase)
                    .enchantments(java.util.Map.of(Enchantment.FORTUNE, 1))
                    .hideFlags(Set.of(io.nexstudios.nexus.bukkit.items.ItemHideFlag.HIDE_ENCHANTS))
                    .lore(List.of(Component.text("Already at full health")))
                    .build()
                    : emeraldBase;

            entries.add(NexFillerEntry.of(emeraldInitial, (event, ctx) -> {
                Player p = ctx.player();
                double max = p.getMaxHealth();
                if (p.getHealth() >= max) return;
                p.setHealth(Math.min(max, p.getHealth() + 4.0D));
                if (p.getHealth() >= max) {
                    int slot = ctx.slot();
                    ItemStack current = event.getView().getTopInventory().getItem(slot);
                    if (current != null) {
                        ItemStack glowing = NexServices.newItemBuilder()
                                .itemStack(current)
                                .enchantments(java.util.Map.of(Enchantment.FORTUNE, 1))
                                .hideFlags(Set.of(io.nexstudios.nexus.bukkit.items.ItemHideFlag.HIDE_ENCHANTS))
                                .lore(List.of(Component.text("Already at full health")))
                                .build();
                        event.getView().getTopInventory().setItem(slot, glowing);
                    }
                }
            }));
            return entries;
        },CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)).thenAccept(entries -> {
            Bukkit.getScheduler().runTask(NexusPlugin.getInstance(), () ->
                    session.populateFillerEntries(entries, startSlot1b, endSlot1b, InvAlignment.LEFT)
            );
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(NexusPlugin.getInstance(), () -> {
                ItemStack error = NexServices.newItemBuilder()
                        .material(Material.BARRIER)
                        .stackSize(1)
                        .build();
                session.populateFiller(List.of(error), startSlot1b, endSlot1b, InvAlignment.LEFT);
                player.sendMessage(Component.text("Failed to load content. Please try again."));
            });
            return null;
        });
    }



}
