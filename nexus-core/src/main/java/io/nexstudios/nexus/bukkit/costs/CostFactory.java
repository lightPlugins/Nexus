package io.nexstudios.nexus.bukkit.costs;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionFactory;
import io.nexstudios.nexus.bukkit.actions.NexParams;
import io.nexstudios.nexus.bukkit.costs.impl.MoneyCost;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter
public class CostFactory {

    private final HashMap<String, NexusCost> availableCosts;

    public CostFactory() {
        this.availableCosts = new HashMap<>();
        registerInternalCosts();
    }

    /**
     * Registers all built-in costs.
     */
    private void registerInternalCosts() {
        // "money" cost
        this.availableCosts.put("money", new MoneyCost());

        NexusPlugin.nexusLogger.info("Registered " + this.availableCosts.size() + " built-in costs.");
    }

    /**
     * Allows third-party plugins to register custom costs.
     */
    public boolean registerCost(String id, NexusCost cost) {
        if (this.availableCosts.containsKey(id)) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Third party plugin tried to register a cost with id: " + id,
                    "Cost with id " + id + " already exists!"
            ));
            return false;
        }

        this.availableCosts.put(id, cost);
        return true;
    }

    @Nullable
    public NexusCost getCost(String costId) {
        return this.availableCosts.get(costId);
    }

    /**
     * Creates a new builder for resolving a list of costs.
     */
    public CostChainBuilder newBuilder() {
        return new CostChainBuilder(this);
    }

    /**
     * Legacy synchronous API that blocks on the async chain.
     */
    @Deprecated
    public boolean resolveCostsBlocking(OfflinePlayer subject,
                                        @Nullable Location location,
                                        List<Map<String, Object>> costs) {

        if (costs == null || costs.isEmpty()) {
            return true;
        }

        try {
            return newBuilder()
                    .subject(subject)
                    .location(location)
                    .costs(costs)
                    .params(NexParams.empty())
                    .resolveAsync()
                    .join(); // blocking – only for legacy use
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Error while resolving costs (legacy API).",
                    "Error: " + e.getMessage()
            ));
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Builder for asynchronous cost chains.
     *
     * Behavior:
     * - All costs are resolved in order.
     * - On first failure:
     *   - optional message is sent (default or overridden),
     *   - "fail-actions" are executed (if present),
     *   - the chain stops and returns false.
     * - On each success:
     *   - "success-actions" are executed (if present),
     *   - the chain continues with the next cost.
     * - The CompletableFuture<Boolean> resolves to:
     *   - true  => at least one cost was checked and none failed.
     *   - false => a cost failed or an unexpected error occurred.
     */
    public static final class CostChainBuilder {

        private final CostFactory factory;

        private OfflinePlayer subject;
        private Location location;
        private List<Map<String, Object>> rawCosts;
        private NexParams params = NexParams.empty();

        private CostChainBuilder(CostFactory factory) {
            this.factory = factory;
        }

        public CostChainBuilder subject(OfflinePlayer subject) {
            this.subject = subject;
            return this;
        }

        public CostChainBuilder player(Player player) {
            this.subject = player;
            return this;
        }

        public CostChainBuilder location(@Nullable Location location) {
            this.location = location;
            return this;
        }

        public CostChainBuilder costs(List<Map<String, Object>> costs) {
            this.rawCosts = costs;
            return this;
        }

        public CostChainBuilder params(NexParams params) {
            this.params = (params == null ? NexParams.empty() : params);
            return this;
        }

        public CostChainBuilder param(String key, String value) {
            if (this.params == null) {
                this.params = NexParams.builder().put(key, value).build();
            } else {
                this.params = this.params.with(key, value);
            }
            return this;
        }

        /**
         * Resolves all configured costs asynchronously.
         */
        public CompletableFuture<Boolean> resolveAsync() {

            if (rawCosts == null || rawCosts.isEmpty()) {
                return CompletableFuture.completedFuture(true);
            }
            if (subject == null) {
                return CompletableFuture.completedFuture(false);
            }

            return resolveIndex(0, false);
        }

        private CompletableFuture<Boolean> resolveIndex(int index, boolean anyCostResolved) {
            if (rawCosts == null || index >= rawCosts.size()) {
                // No more costs -> return true if at least one cost has been evaluated
                return CompletableFuture.completedFuture(anyCostResolved);
            }

            Map<String, Object> costMap = rawCosts.get(index);
            Object idObj = costMap.get("id");
            if (!(idObj instanceof String costId)) {
                NexusPlugin.nexusLogger.warning("Cost entry without valid 'id' in Cost Section");
                // Skip invalid entry
                return resolveIndex(index + 1, anyCostResolved);
            }

            NexusCost cost = factory.getCost(costId);
            if (cost == null) {
                NexusPlugin.nexusLogger.warning("Unknown cost: " + costId + " in Cost Section");
                // Skip unknown cost
                return resolveIndex(index + 1, anyCostResolved);
            }

            CostData costData = new CostData();
            costData.getData().putAll(costMap);
            anyCostResolved = true;

            NexusCostContext ctx = new NexusCostContext(subject, location, costData, params);

            try {
                return cost.resolveAsync(ctx).thenCompose(result -> {

                    // Merge cost result params into current NexParams for messages/actions
                    NexParams mergedParams = mergeParamsWithResult(params, result);

                    if (result == null || result.failed()) {
                        // Cost failed: send message & run fail-actions, then stop
                        handleFailure(costId, ctx, mergedParams);
                        return CompletableFuture.completedFuture(false);
                    }

                    // Cost succeeded: run success-actions, then continue with next
                    handleSuccess(costId, ctx, mergedParams);

                    return resolveIndex(index + 1, true);
                });
            } catch (Exception e) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Error while resolving cost: " + costId,
                        "Error: " + e.getMessage()
                ));
                e.printStackTrace();
                return CompletableFuture.completedFuture(false);
            }
        }

        /**
         * Merges existing NexParams with CostResult params.
         */
        private NexParams mergeParamsWithResult(NexParams base, CostResult result) {
            if (result == null || result.params().isEmpty()) {
                return base == null ? NexParams.empty() : base;
            }
            Map<String, String> merged = new HashMap<>(base == null ? Map.of() : base.asMap());
            merged.putAll(result.params());
            return NexParams.of(merged, base == null ? null : base.tagResolver());
        }

        /**
         * Handles a failed cost:
         * - sends message (if configured),
         * - executes fail-actions.
         */
        private void handleFailure(String costId, NexusCostContext ctx, NexParams mergedParams) {

            CostData data = ctx.data();
            boolean sendMessage = (boolean) data.getData().getOrDefault("send-message", true);

            if (sendMessage) {
                sendFailureMessage(costId, ctx, mergedParams);
            }

            List<Map<String, Object>> failActions = data.getActionList("fail-actions");
            if (!failActions.isEmpty()) {
                executeActions(ctx, failActions, mergedParams);
            }
        }

        /**
         * Handles a successful cost:
         * - executes success-actions.
         */
        private void handleSuccess(String costId, NexusCostContext ctx, NexParams mergedParams) {

            CostData data = ctx.data();
            List<Map<String, Object>> successActions = data.getActionList("success-actions");
            if (!successActions.isEmpty()) {
                executeActions(ctx, successActions, mergedParams);
            }
        }

        /**
         * Sends the failure message according to the config:
         *
         * - message: <string>  -> overrides default message
         *   - if message is empty (""), no default message is sent
         * - send-message: false -> no message at all
         */
        private void sendFailureMessage(String costId, NexusCostContext ctx, NexParams mergedParams) {

            Player player = ctx.player();
            if (player == null) {
                // No online player -> nothing to send to
                return;
            }

            CostData data = ctx.data();
            Object rawMessageObj = data.getData().get("message");

            if (rawMessageObj instanceof String override) {
                // Override provided (can be empty)
                if (override.isEmpty()) {
                    // Explicit empty string: override default and send nothing
                    return;
                }

                String replaced = StringUtils.replaceKeyWithValue(override, mergedParams);
                Component component;
                if (NexusPlugin.getInstance().papiHook != null) {
                    component = NexusPlugin.getInstance().papiHook.translate(player, replaced);
                } else {
                    component = MiniMessage.miniMessage().deserialize(replaced);
                }

                player.sendMessage(component);
                return;
            }

            // No override -> use default message from language file
            try {
                // NOTE: message key is intentionally generic.
                // Plugin authors should provide this key in their language file.
                NexusPlugin.getInstance().getMessageSender().send(player, "general.cost-failed");
            } catch (Exception e) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Failed to send default cost-failed message for cost: " + costId,
                        "Error: " + e.getMessage()
                ));
                e.printStackTrace();
            }
        }

        /**
         * Executes a list of actions using the global ActionFactory.
         */
        private void executeActions(NexusCostContext ctx,
                                    List<Map<String, Object>> actions,
                                    NexParams mergedParams) {

            Player player = ctx.player();
            if (player == null) {
                // Actions require a living entity context; if no online player, skip
                return;
            }

            LivingEntity actor = player;
            Location targetLocation = ctx.location();

            ActionFactory actionFactory = NexusPlugin.getInstance().getActionFactory();
            if (actionFactory == null) {
                NexusPlugin.nexusLogger.warning("Tried to execute actions for cost, but ActionFactory is null.");
                return;
            }

            try {
                actionFactory.newExecution()
                        .actor(actor)
                        .targetLocation(targetLocation)
                        .actions(actions)
                        .params(mergedParams)
                        .execute();
            } catch (Exception e) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Error while executing actions for cost.",
                        "Error: " + e.getMessage()
                ));
                e.printStackTrace();
            }
        }
    }
}