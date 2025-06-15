package io.nexstudios.nexus.bukkit.inventory.models;

import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

@Getter
public class MenuItem {
    private final ItemStack itemStack;
    private final MenuItemClickHandler clickHandler;

    public MenuItem(ItemStack itemStack, MenuItemClickHandler clickHandler) {
        this.itemStack = itemStack;
        this.clickHandler = clickHandler;
    }

    public void onClick(InventoryClickEvent event) {
        clickHandler.onClick(event, this);
    }

    @FunctionalInterface
    public interface MenuItemClickHandler {
        void onClick(InventoryClickEvent event, MenuItem item);
    }
}

