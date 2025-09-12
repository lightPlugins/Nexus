package io.nexstudios.nexus.bukkit.inv;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;

import java.util.Map;

public interface NexClickContext {
    /**
     * Retrieves the player associated with the current inventory click context.
     * This represents the player who performed the action triggering the context.
     *
     * @return the Player object associated with the inventory click context.
     */
    Player player();

    /**
     * Retrieves the ID of the inventory associated with the current inventory click context.
     * This ID is typically used to identify the specific inventory instance
     * involved in the interaction.
     *
     * @return the inventory ID as a string.
     */
    String inventoryId();

    /**
     * Retrieves the current page index within the inventory context.
     * The page index is typically used to represent the specific page
     * being interacted with in paginated inventory systems.
     *
     * @return the zero-based index of the current inventory page.
     */
    int pageIndex();

    /**
     * Retrieves the index of the top inventory slot associated with the current inventory
     * click context. The slot index is 0-based, with 0 representing the first slot.
     *
     * @return the 0-based index of the top inventory slot being interacted with.
     */
    int slot();          // Top-Inventory Slot (0-basiert)

    /**
     * Retrieves the namespace string associated with the current inventory click context.
     * The namespace serves as a unique identifier for specific interactions or operations
     * and follows a predefined format (e.g., "navigation:close", "required:<id>", "custom:<id>", "body").
     *
     * @return the namespace string representing the type or context of the interaction.
     */
    String namespace();  // z. B. "navigation:close", "required:<id>", "custom:<id>", "body"

    /**
     * Determines if the current inventory interaction is identified as a navigation action.
     * Navigation actions typically describe interactions meant for moving through interfaces,
     * such as navigating between pages, closing inventories, or switching views.
     *
     * @return true if the interaction corresponds to a navigation action; false otherwise.
     */
    boolean isNavigation();

    /**
     * Determines if the current inventory interaction is marked as required.
     * This is often used to identify operations or contexts that are mandatory
     * and cannot be bypassed or ignored.
     *
     * @return true if the interaction is considered required; false otherwise.
     */
    boolean isRequired();

    /**
     * Determines if the current inventory interaction is marked as custom.
     * A custom interaction often refers to user-defined or non-standard
     * actions within the inventory context that are not covered by predefined types.
     *
     * @return true if the interaction is identified as custom; false otherwise.
     */
    boolean isCustom();

    /**
     * Determines if the current inventory click context is associated with the "body" type or interaction.
     * This typically indicates that the interaction involves a specific designated area or component
     * in the inventory referred to as the "body."
     *
     * @return true if the interaction is identified as being within the "body" context; false otherwise.
     */
    boolean isBody();

    /**
     * Retrieves the zero-based index within the visible body page of an inventory click context.
     * This value is only available and meaningful if the interaction is identified as being within
     * the "body" context (i.e., when {@link #isBody()} returns true).
     *
     * @return the 0-based index within the visible body page if {@link #isBody()} is true, or null otherwise.
     */
    Integer bodyIndex(); // 0-basiert innerhalb der sichtbaren Body-Seite; nur wenn isBody() == true

    /**
     * Retrieves the additional configuration settings associated with the current inventory
     * click context. These settings provide extra, custom-defined options or data that
     * complement the inventory interaction.
     *
     * @return the ConfigurationSection containing supplementary custom settings, or null
     * if no extra settings are defined.
     */
    ConfigurationSection extraSettings();

    /**
     * Determines if the current click action performed by the player is a left-click.
     * A left-click typically refers to the primary mouse button or equivalent action.
     *
     * @return true if the click action is identified as a left-click; false otherwise.
     */
    boolean isLeftClick();

    /**
     * Determines if the current click action performed by the player is a right-click.
     * A right-click typically refers to the secondary interaction button on a mouse or equivalent.
     *
     * @return true if the click action is identified as a right-click; false otherwise.
     */
    boolean isRightClick();

    /**
     * Determines if the current click action performed by the player is a middle-click.
     * A middle-click refers to the interaction using the middle mouse button, often
     * used for specific inventory or interaction-related actions.
     *
     * @return true if the click action is identified as a middle-click; false otherwise.
     */
    boolean isMiddleClick();

    /**
     * Determines if the current click action is a shift-click.
     * A shift-click typically occurs when the player interacts with an inventory
     * while holding the Shift key, often used for quick item transfers between
     * the player's inventory and a container or vice versa.
     *
     * @return true if the click action is identified as a shift-click; false otherwise.
     */
    boolean isShiftClick();

    /**
     * Determines whether the current click interaction qualifies as a double-click.
     * A double-click is typically defined as two consecutive clicks within a short
     * period of time, often used for specific actions such as rapid item selection
     * or stack interactions within an inventory.
     *
     * @return true if the click interaction is identified as a double-click; false otherwise.
     */
    boolean isDoubleClick();

    /**
     * Determines if the input click is a keyboard-related click action.
     * Keyboard clicks include actions such as pressing number keys (NUMBER_KEY),
     * dropping items (DROP), or control-based drops (CONTROL_DROP).
     *
     * @return true if the click action originates from a keyboard interaction; false otherwise.
     */
    boolean isKeyboardClick();  // z. B. NUMBER_KEY, DROP, CONTROL_DROP

    /**
     * Retrieves the hotbar key if the click action corresponds to a keyboard number key action.
     * The method returns a value between 1 and 9 if the action involves NUMBER_KEY,
     * representing the respective hotbar slot key pressed by the player.
     * If the action does not involve a NUMBER_KEY, the method returns -1.
     *
     * @return an integer value from 1 to 9 for NUMBER_KEY actions, or -1 for other actions.
     */
    int hotbarKey();            // 1..9 bei NUMBER_KEY, sonst -1

    /**
     * Retrieves the type of click action performed by the player during the inventory interaction.
     * The click type denotes how the interaction was initiated, including left-click, right-click,
     * shift-click, and other specialized click actions.
     *
     * @return the ClickType representing the type of click detected during the player's interaction.
     */
    ClickType clickType();

    /**
     * Retrieves the type of inventory action performed during the click interaction.
     * The returned action indicates the specific operation being executed within the inventory,
     * such as picking up an item, placing an item, swapping items, etc.
     *
     * @return the InventoryAction corresponding to the inventory interaction being processed.
     */
    InventoryAction action();

}

