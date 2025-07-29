package io.nexstudios.nexus.bukkit.droptable.models;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.ActionFactory;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DropTable {

    private String id;
    private List<Drop> drops;
    private DropConditions conditions;
    private DropStyle style;
    private YamlConfiguration configuration;


    @Getter
    @Setter
    public static class Drop {

        private String item;
        private Object amount;
        private String chance;
        private DropSettings settings;
        private List<Map<String, Object>> actions;

        public void executeActions(Player player) {

            if (actions == null || actions.isEmpty()) {
                return;
            }

            // Hole die ActionFactory vom Plugin
            ActionFactory actionFactory = NexusPlugin.getInstance().getActionFactory();

            for (Map<String, Object> actionDataMap : actions) {
                String actionId = (String) actionDataMap.get("id");
                NexusAction action = actionFactory.getAction(actionId);

                // Überprüfen, ob die Aktion registriert ist
                if (action == null) {
                    NexusPlugin.nexusLogger.warning("Unknown action: " + actionId + " in Drop.");
                    continue;
                }

                // Erstelle ein ActionData-Objekt basierend auf den YAML-Daten
                ActionData actionData = new ActionData();
                actionData.getData().putAll(actionDataMap);

                try {
                    // Führe die Aktion aus
                    action.execute(player, actionData);
                } catch (Exception e) {
                    // Fehler beim Ausführen der Aktion abfangen und protokollieren
                    NexusPlugin.nexusLogger.error("Failed to execute action " + actionId);
                }
            }
        }

        @Getter
        @Setter
        public static class DropSettings {

            private boolean pickUpOwner;
            private boolean visibleOwner;
            private boolean isVirtualItem;
            private NamedTextColor glowColor;
            private String trailColor;
            private Component itemName;
            private String dropMultiplierExpression;

        }
    }

    @Getter
    @Setter
    public static class DropConditions {

        private KillCondition killCondition;
        private MineCondition mineCondition;

        @Getter
        @Setter
        public static class KillCondition {
            private List<String> entityTypes;
        }

        @Getter
        @Setter
        public static class MineCondition {
            private List<String> blockTypes;
        }
    }

    @Getter
    @Setter
    public static class DropStyle {

        private FancyDropStyle fancyDropStyle;

        @Getter
        @Setter
        public static class FancyDropStyle {
            private Vector velocity;
        }
    }

}
