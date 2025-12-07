package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.conditions.NexusConditionContext;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.util.Tristate;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConditionLuckPermission implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    /**
     * Asynchrone Prüfung mit LuckPerms.
     * - Online-Player: synchroner Check über PlayerAdapter, sofortige Future.
     * - Offline-Player: asynchroner Check über UserManager.loadUser(UUID).
     */
    @Override
    public CompletableFuture<Boolean> checkAsync(NexusConditionContext context) {
        ConditionData data = context.data();
        Object permObj = data.getData().get("permission");

        if (permObj == null || !data.validate(permObj, String.class)) {
            NexusPlugin.nexusLogger.error("Invalid permission data.");
            NexusPlugin.nexusLogger.error("Missing 'permission' parameter for condition");
            return CompletableFuture.completedFuture(false);
        }

        String permission = "";

        LuckPerms luckPerms = NexusPlugin.getInstance().getLuckPermsHook().getLuckPermsAPI();
        if (luckPerms == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "LuckPermsHook returned null API instance in ConditionLuckPermission.",
                    "Is LuckPerms enabled and registered correctly?"
            ));
            return CompletableFuture.completedFuture(false);
        }

        // Online-Fall: schneller synchroner Check über PlayerAdapter
        Player online = context.player();
        if (online != null) {
            PlayerAdapter<Player> adapter = luckPerms.getPlayerAdapter(Player.class);
            CachedPermissionData permissionData = adapter.getPermissionData(online);
            Tristate checkResult = permissionData.checkPermission(permission);
            return CompletableFuture.completedFuture(checkResult.asBoolean());
        }

        // Offline-Fall: asynchron über UserManager/loadUser(UUID)
        UUID uuid = context.offlinePlayer().getUniqueId();
        return luckPerms.getUserManager()
                .loadUser(uuid)
                .thenApply(user -> {
                    if (user == null) {
                        return false;
                    }
                    CachedPermissionData permissionData = user.getCachedData().getPermissionData();
                    return permissionData.checkPermission(permission).asBoolean();
                })
                .exceptionally(ex -> {
                    NexusPlugin.nexusLogger.error(List.of(
                            "Error while checking LuckPerms permission for offline user " + uuid,
                            "Error: " + ex.getMessage()
                    ));
                    ex.printStackTrace();
                    return false;
                });
    }

    @Override
    public void sendMessage(NexusConditionContext context) {
        ConditionData data = context.data();

        boolean sendMessage = (boolean) data.getData().getOrDefault("send-message", true);
        boolean asActionBar = (boolean) data.getData().getOrDefault("as-actionbar", false);

        if (!sendMessage) return;

        Player player = context.player();
        if (player == null) {
            // Subjekt ist offline, wir können niemanden direkt ansprechen
            return;
        }

        if (asActionBar) {
            player.sendActionBar(Component.text("Condition not met"));
        } else {
            NexusPlugin.getInstance().getMessageSender().send(player, "general.condition-not-met");
        }
    }
}