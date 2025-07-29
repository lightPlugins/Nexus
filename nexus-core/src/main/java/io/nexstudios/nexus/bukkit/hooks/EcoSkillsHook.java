package io.nexstudios.nexus.bukkit.hooks;

import com.willfp.ecoskills.api.EcoSkillsAPI;
import com.willfp.ecoskills.api.modifiers.StatModifier;
import com.willfp.ecoskills.stats.Stat;
import com.willfp.ecoskills.stats.Stats;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.List;

public class EcoSkillsHook {

    public List<StatModifier> getStatModifiers(Player player) {
        return EcoSkillsAPI.getStatModifiers(player);
    }

    @Nullable
    public Stat getStatByName(String statName, Player player) {
        return Stats.INSTANCE.getByID(statName);
    }


    public int getStatLevel(String statName, Player player) {

        Stat stat = getStatByName(statName, player);

        if(stat == null) {
            NexusPlugin.getInstance().getLogger().warning("Stat " + statName + " does not exist!");
            return 1;
        }

        return stat.getActualLevel$core_plugin(player);

    }
}
