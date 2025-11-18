package io.nexstudios.nexus.bukkit.hooks.beeminions;

import me.leo_s.beeminions.api.events.MinionItemsProduceEvent;
import me.leo_s.beeminions.api.events.MinionItemsRemoveEvent;
import me.leo_s.beeminions.api.events.MinionSellItemsEvent;

public interface IBeeMinionsEvent {

    void execute(MinionItemsRemoveEvent event);
    void execute(MinionItemsProduceEvent event);
    void execute(MinionSellItemsEvent event);

}
