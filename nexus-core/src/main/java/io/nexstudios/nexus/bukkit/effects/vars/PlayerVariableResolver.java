package io.nexstudios.nexus.bukkit.effects.vars;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public interface PlayerVariableResolver {
    Map<String, String> resolve(Player player);

    static PlayerVariableResolver composite(PlayerVariableResolver... resolvers) {
        return player -> {
            Map<String, String> result = new HashMap<>();
            for (PlayerVariableResolver r : resolvers) {
                result.putAll(r.resolve(player));
            }
            return result;
        };
    }

    static PlayerVariableResolver ofStore() {
        return player -> new HashMap<>(PlayerVariables.snapshot(player.getUniqueId()));
    }

    static PlayerVariableResolver ofConstant(String key, BiFunction<Player, String, String> provider) {
        return player -> {
            String val = provider.apply(player, key);
            return val == null ? Map.of() : Map.of(key, val);
        };
    }
}

