package io.nexstudios.nexus.bukkit.costs;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.NexParams;
import io.nexstudios.nexus.bukkit.files.NexusFileReader;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Showcase class for the Nexus Cost API.
 *
 * This class is NOT registered anywhere and only exists as
 * in-code documentation / usage examples.
 */
public final class CostApiShowcase {

    private CostApiShowcase() {
        // utility
    }

    /**
     * Example 1:
     * Reading a List<Map<String, Object>> "costs" from a config
     * and resolving them in a command or GUI click.
     */
    public static void exampleResolveCostsSync(Player player, FileConfiguration config) {

        // 1) Read the list from config:
        //    costs:
        //      - id: money
        //        amount: 15000
        //        send-message: true
        //        message: "<red>You need <dark_red><needed-amount> <red>to buy this"
        //        success-actions: []
        //        fail-actions: []
        List<Map<String, Object>> costs = NexusFileReader.getMapList(config, "costs");

        // 2) Optional: prepare additional placeholders for messages/actions
        NexParams params = NexParams.builder()
                .put("item-name", "Epic Sword")
                .build();

        // 3) Resolve the costs using the global CostFactory
        boolean success = NexusPlugin.getInstance()
                .getCostFactory()
                .newBuilder()
                .player(player)
                .location(player.getLocation())
                .costs(costs)
                .params(params)
                .resolveAsync()   // returns CompletableFuture<Boolean>
                .join();          // here we "wait" -> effectively sync usage

        if (!success) {
            // At least one cost failed or an error occurred.
            //
            // Note:
            // - The cost system already sent any fail-messages
            //   (unless send-message: false)
            // - And already executed fail-actions (if configured)
            return;
        }

        // All costs were resolved successfully:
        // - success-actions were already executed for each cost
        // -> Now you can continue (e.g. give the item)
        giveExampleItem(player);
    }

    /**
     * Example 2:
     * Fully asynchronous usage without blocking the calling thread.
     */
    public static void exampleResolveCostsAsync(Player player, FileConfiguration config) {

        List<Map<String, Object>> costs = NexusFileReader.getMapList(config, "costs");

        NexParams params = NexParams.builder()
                .put("item-name", "Epic Sword")
                .build();

        CompletableFuture<Boolean> future = NexusPlugin.getInstance()
                .getCostFactory()
                .newBuilder()
                .player(player)
                .location(player.getLocation())
                .costs(costs)
                .params(params)
                .resolveAsync();

        future.thenAccept(success -> {
            if (!success) {
                // Costs failed, messages/actions are already handled.
                return;
            }

            // If this callback might run off the main thread,
            // schedule Bukkit work back on the main thread:
            Bukkit.getScheduler().runTask(NexusPlugin.getInstance(), () -> {
                giveExampleItem(player);
            });
        });
    }

    /**
     * Example 3:
     * Using previewSync(...) to build lore lines (one per cost) and
     * to check whether the player could afford the costs BEFORE
     * actually resolving/paying them.
     */
    public static List<Component> exampleBuildLoreWithCostPreview(Player player,
                                                                  FileConfiguration config,
                                                                  String basePath) {

        // Base lore from config, e.g.:
        // my-item:
        //   lore:
        //     - "<gray>Some description..."
        List<String> rawLore = config.getStringList(basePath + ".lore");

        // Costs from config, e.g.:
        // my-item:
        //   costs:
        //     - id: money
        //       amount: 15000
        //       placeholder: "<needed-amount> Coins / <current-amount> Coins"
        List<Map<String, Object>> costs = NexusFileReader.getMapList(config, basePath + ".costs");

        NexParams baseParams = NexParams.builder()
                .put("item-name", "Epic Sword")
                .build();

        List<Component> out = new ArrayList<>();

        // 1) Add the normal lore lines (no cost info yet)
        for (String line : rawLore) {
            String processed = StringUtils.replaceKeyWithValue(line, baseParams);
            if (NexusPlugin.getInstance().papiHook != null) {
                processed = NexusPlugin.getInstance().papiHook.translateIntoString(player, processed);
            }
            out.add(MiniMessage.miniMessage().deserialize(processed));
        }

        // 2) For each cost, call previewSync(...) and build a display line
        for (Map<String, Object> costMap : costs) {
            Object idObj = costMap.get("id");
            if (!(idObj instanceof String costId)) {
                NexusPlugin.nexusLogger.warning("Cost entry without valid 'id' in Cost Section (preview example)");
                continue;
            }

            NexusCost cost = NexusPlugin.getInstance()
                    .getCostFactory()
                    .getCost(costId);

            if (cost == null) {
                NexusPlugin.nexusLogger.warning("Unknown cost '" + costId + "' in preview example");
                continue;
            }

            CostData data = new CostData();
            data.getData().putAll(costMap);
            NexusCostContext ctx = new NexusCostContext(player, player.getLocation(), data, baseParams);

            // Mandatory placeholder template (defined per cost entry in config)
            Object placeholderObj = data.getData().get("placeholder");
            if (!(placeholderObj instanceof String placeholder) || placeholder.isEmpty()) {
                NexusPlugin.nexusLogger.warning("Cost '" + costId + "' has no valid 'placeholder' entry (preview example).");
                continue;
            }

            // PREVIEW: no side effects, just "can afford?" + placeholder params
            CostResult preview = cost.previewSync(ctx);
            boolean canAfford = preview.succeeded();
            Map<String, String> previewParams = preview.params();

            // Merge baseParams + previewParams for internal placeholder replacement
            NexParams mergedParams = baseParams.merge(NexParams.of(previewParams));

            // Replace internal placeholders in the placeholder template
            String line = StringUtils.replaceKeyWithValue(placeholder, mergedParams);

            // Optional: color based on canAfford
            if (canAfford) {
                line = "<green>" + line;
            } else {
                line = "<red>" + line;
            }

            // Optional: PAPI
            if (NexusPlugin.getInstance().papiHook != null) {
                line = NexusPlugin.getInstance().papiHook.translateIntoString(player, line);
            }

            out.add(MiniMessage.miniMessage().deserialize(line));
        }

        return out;
    }

    /**
     * Example 4:
     * Registering a custom cost from another plugin.
     *
     * This code would typically live in the other plugin's onEnable().
     */
    public static void exampleRegisterCustomCost() {

        // Imagine this is called from another plugin's onEnable()
        CostFactory costFactory = NexusPlugin.getInstance().getCostFactory();

        // Register "my-item-cost" which is provided by another plugin
        costFactory.registerCost("my-item-cost", new ExampleItemCost());
    }

    /**
     * Example of a simple custom cost implementation.
     *
     * Config example:
     *
     * costs:
     *   - id: my-item-cost
     *     item-id: "minecraft:diamond"
     *     placeholder: "<item-id>"
     *     send-message: true
     *     message: "<red>You need a <aqua>diamond</aqua>!"
     *     fail-actions:
     *       - id: sound
     *         sound: ENTITY_VILLAGER_NO
     */
    public static final class ExampleItemCost implements NexusCost {

        @Override
        public org.bukkit.plugin.java.JavaPlugin getPlugin() {
            // In a real plugin, return your own plugin instance here.
            return NexusPlugin.getInstance();
        }

        @Override
        public CostResult resolveSync(NexusCostContext context) {
            Player player = context.player();
            if (player == null) {
                // This cost requires an online player.
                return CostResult.fail();
            }

            CostData data = context.data();

            Object itemIdObj = data.getData().get("item-id");
            if (itemIdObj == null) {
                // Config error: missing "item-id"
                return CostResult.fail();
            }

            String itemId = String.valueOf(itemIdObj);

            // Here you would implement your custom item check/removal logic.
            // For this showcase we just simulate:
            boolean hasItemAndRemoved = simulateCheckAndRemoveItem(player, itemId);

            Map<String, String> params = new HashMap<>();
            params.put("item-id", itemId);

            if (!hasItemAndRemoved) {
                // Cost failed -> params can be used in message placeholders
                return CostResult.fail(params);
            }

            // Cost succeeded
            return CostResult.success(params);
        }

        /**
         * Preview for this example cost.
         * Does NOT remove items, only checks if the player has them and exposes placeholders.
         */
        @Override
        public CostResult previewSync(NexusCostContext context) {
            Player player = context.player();
            if (player == null) {
                return CostResult.fail();
            }

            CostData data = context.data();

            Object itemIdObj = data.getData().get("item-id");
            if (itemIdObj == null) {
                return CostResult.fail();
            }

            String itemId = String.valueOf(itemIdObj);

            Map<String, String> params = new HashMap<>();
            params.put("item-id", itemId);

            // TODO: real check: does the player have the required item?
            boolean hasItem = false; // simulated

            if (!hasItem) {
                return CostResult.fail(params);
            }
            return CostResult.success(params);
        }

        /**
         * Example / dummy method – replace with real item logic.
         */
        private boolean simulateCheckAndRemoveItem(Player player, String itemId) {
            // TODO: Implement your item lookup & remove logic here.
            // For documentation purposes we just always fail:
            return false;
        }
    }

    // --- Helper only for the showcase ---

    private static void giveExampleItem(Player player) {
        // TODO: Give the actual item (e.g. via your ItemBuilder / item system).
        player.sendMessage("You have received your example item.");
    }
}