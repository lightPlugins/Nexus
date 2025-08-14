package io.nexstudios.nexus.bukkit.hooks.mythicmobs;

import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobInteractEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

public class MythicMobsHook implements Listener {

    public static List<IMythicDeathEvent> mythicDeathEvents = new ArrayList<>();

    public MythicMobsHook(PluginManager pluginManager) {
        pluginManager.registerEvents(this, NexusPlugin.getInstance());
    }

    public boolean isMythicMob(Entity entity, String id) {
        ActiveMob activeMob = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(entity);
        if (activeMob == null) {
            return false;
        }
        return activeMob.getType().getInternalName().equals(id);
    }

    public static void registerMythicDeathEvent(IMythicDeathEvent event) {
        mythicDeathEvents.add(event);
    }


    @EventHandler
    public void onMythicDeathEvent(MythicMobDeathEvent event) {
        mythicDeathEvents.forEach(single -> single.execute(event));
    }

    @EventHandler
    public void onMythicDamageEvent(EntityDamageByEntityEvent event) {
        if (MythicBukkit.inst().getAPIHelper().isMythicMob(event.getEntity())) {

            ActiveMob activeMob = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(event.getEntity());
            if (activeMob == null) {
                return;
            }
            mythicDeathEvents.forEach(single -> single.execute(
                    event,
                    activeMob
            ));
        }
    }
}
