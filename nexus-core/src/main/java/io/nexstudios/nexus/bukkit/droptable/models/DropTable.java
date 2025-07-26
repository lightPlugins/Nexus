package io.nexstudios.nexus.bukkit.droptable.models;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.List;

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
        private int amount;
        private String chance;
        private DropSettings settings;

        @Getter
        @Setter
        public static class DropSettings {

            private boolean pickUpOwner;
            private boolean visibleOwner;
            private NamedTextColor glowColor;
            private String trailColor;
            private Component itemName;

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
