package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.conditions.NexusConditionContext;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ConditionLuckPermission implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    /**
     * Config:
     * - permission: "node"            (optional)
     * - permissions: ["a", "b"]       (optional)
     * - group: "vip"                 (optional)
     * - groups: ["vip", "admin"]     (optional)
     * - require-all: false           (optional, default false)
     *
     * Logic:
     * - If permissions are provided -> they must match (OR/AND via require-all)
     * - If groups are provided -> they must match (OR/AND via require-all)
     * - If both permissions and groups are provided -> BOTH sections must pass (AND)
     */
    @Override
    public CompletableFuture<Boolean> checkAsync(NexusConditionContext context) {
        ConditionData data = context.data();

        List<String> permissions = readStringList(data, "permissions", "permission");
        List<String> groups = readStringList(data, "groups", "group");

        if (permissions.isEmpty() && groups.isEmpty()) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Invalid permission condition data.",
                    "Missing 'permission'/'permissions' and 'group'/'groups' for condition permission"
            ));
            return CompletableFuture.completedFuture(false);
        }

        boolean requireAll = readBoolean(data);

        LuckPerms luckPerms = NexusPlugin.getInstance().getLuckPermsHook().getLuckPermsAPI();
        if (luckPerms == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "LuckPermsHook returned null API instance in ConditionLuckPermission.",
                    "Is LuckPerms enabled and registered correctly?"
            ));
            return CompletableFuture.completedFuture(false);
        }

        Player online = context.player();
        if (online != null) {
            boolean ok = evaluateOnline(luckPerms, online, permissions, groups, requireAll);
            return CompletableFuture.completedFuture(ok);
        }

        UUID uuid = context.offlinePlayer().getUniqueId();
        return luckPerms.getUserManager()
                .loadUser(uuid)
                .thenApply(user -> user != null && evaluateUser(luckPerms, user, permissions, groups, requireAll))
                .exceptionally(ex -> {
                    NexusPlugin.nexusLogger.error(List.of(
                            "Error while checking LuckPerms permission/groups for offline user " + uuid,
                            "Error: " + ex.getMessage()
                    ));
                    ex.printStackTrace();
                    return false;
                });
    }

    private boolean evaluateOnline(LuckPerms luckPerms,
                                   Player player,
                                   List<String> permissions,
                                   List<String> groups,
                                   boolean requireAll) {

        PlayerAdapter<Player> adapter = luckPerms.getPlayerAdapter(Player.class);

        // Permissions: über CachedPermissionData (schnell)
        CachedPermissionData permissionData = adapter.getPermissionData(player);
        boolean permsOk = permissions.isEmpty() || matchesPermissions(permissionData, permissions, requireAll);

        // Groups: über User (schnell, da online gecached)
        User user;
        try {
            user = adapter.getUser(player);
        } catch (Throwable t) {
            // Fallback: wenn aus irgendeinem Grund kein User verfügbar ist
            user = null;
        }
        boolean groupsOk = groups.isEmpty() || (user != null && matchesGroups(luckPerms, user, groups, requireAll));

        // Wenn beides gesetzt ist -> beides muss passen
        return permsOk && groupsOk;
    }

    private boolean evaluateUser(LuckPerms luckPerms,
                                 User user,
                                 List<String> permissions,
                                 List<String> groups,
                                 boolean requireAll) {

        CachedPermissionData permissionData = user.getCachedData().getPermissionData();
        boolean permsOk = permissions.isEmpty() || matchesPermissions(permissionData, permissions, requireAll);

        boolean groupsOk = groups.isEmpty() || matchesGroups(luckPerms, user, groups, requireAll);

        return permsOk && groupsOk;
    }

    private boolean matchesPermissions(CachedPermissionData permissionData,
                                       List<String> permissions,
                                       boolean requireAll) {
        if (permissions.isEmpty()) return true;

        if (requireAll) {
            for (String node : permissions) {
                if (!permissionData.checkPermission(node).asBoolean()) return false;
            }
            return true;
        } else {
            for (String node : permissions) {
                if (permissionData.checkPermission(node).asBoolean()) return true;
            }
            return false;
        }
    }

    private boolean matchesGroups(LuckPerms luckPerms,
                                  User user,
                                  List<String> groups,
                                  boolean requireAll) {
        if (groups.isEmpty()) return true;

        QueryOptions qo;
        try {
            qo = luckPerms.getContextManager().getQueryOptions(user).orElse(QueryOptions.defaultContextualOptions());
        } catch (Throwable t) {
            qo = QueryOptions.defaultContextualOptions();
        }

        Set<String> userGroups = new HashSet<>();
        for (Group g : user.getInheritedGroups(qo)) {
            if (g == null) {
                continue;
            } else {
                g.getName();
            }
            userGroups.add(g.getName().toLowerCase(Locale.ROOT));
        }

        if (requireAll) {
            for (String g : groups) {
                if (!userGroups.contains(g.toLowerCase(Locale.ROOT))) return false;
            }
            return true;
        } else {
            for (String g : groups) {
                if (userGroups.contains(g.toLowerCase(Locale.ROOT))) return true;
            }
            return false;
        }
    }

    private static boolean readBoolean(ConditionData data) {
        Object raw = data.getData().get("require-all");
        if (raw instanceof Boolean b) return b;
        if (raw == null) return false;
        return Boolean.parseBoolean(String.valueOf(raw).trim().toLowerCase(Locale.ROOT));
    }

    private static List<String> readStringList(ConditionData data, String listKey, String singleKey) {
        List<String> out = new ArrayList<>();

        Object rawList = data.getData().get(listKey);
        if (rawList instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                String s = String.valueOf(o).trim();
                if (!s.isEmpty()) out.add(s);
            }
        }

        if (out.isEmpty()) {
            Object rawOne = data.getData().get(singleKey);
            if (rawOne != null) {
                String s = String.valueOf(rawOne).trim();
                if (!s.isEmpty()) out.add(s);
            }
        }

        return List.copyOf(out);
    }

    @Override
    public void sendMessage(NexusConditionContext context) {
        ConditionData data = context.data();

        boolean sendMessage = (boolean) data.getData().getOrDefault("send-message", true);
        boolean asActionBar = (boolean) data.getData().getOrDefault("as-actionbar", false);

        if (!sendMessage) return;

        Player player = context.player();
        if (player == null) {
            return;
        }

        if (asActionBar) {
            player.sendActionBar(Component.text("Condition not met"));
        } else {
            NexusPlugin.getInstance().getMessageSender().send(player, "general.condition-not-met");
        }
    }
}