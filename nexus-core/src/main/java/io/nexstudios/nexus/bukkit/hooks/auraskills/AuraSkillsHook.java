package io.nexstudios.nexus.bukkit.hooks.auraskills;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.AuraSkillsBukkit;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.registry.NamespacedRegistry;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.stat.StatModifier;
import dev.aurelium.auraskills.api.util.AuraSkillsModifier.Operation;
import dev.aurelium.auraskills.api.trait.Trait;
import dev.aurelium.auraskills.api.user.SkillsUser;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.files.NexusFile;
import lombok.Getter;

import java.util.HashMap;
import java.util.UUID;


@Getter
public class AuraSkillsHook {

    private final HashMap<String, Operation> activeModifiers = new HashMap<>();

    private final AuraSkillsApi auraSkillsApi;
    private final AuraSkillsBukkit auraSkillsBukkit;

    private NexusFile statsFile;

    public AuraSkillsHook() {
        this.auraSkillsApi = AuraSkillsApi.get();
        this.auraSkillsBukkit = AuraSkillsBukkit.get();
        // this.statsFile = new NexusFile(NexusPlugin.getInstance(), "auraskills/stats.yml", NexusPlugin.getInstance().getNexusLogger(), false);
    }

    public SkillsUser getUser(UUID uuid) {
        return this.auraSkillsApi.getUser(uuid);
    }

    public double getFinalStatLevel(SkillsUser skillsUser, Stat stat) {
        return skillsUser.getStatLevel(stat);
    }

    public double getBaseStatLevel(SkillsUser skillsUser, Stat stat) {
        return skillsUser.getBaseStatLevel(stat);
    }

    public void addStatModifier(SkillsUser skillsUser, String modifierName, Stat stat, double value, Operation operation) {
        skillsUser.addStatModifier(new StatModifier(modifierName, stat, value, operation));
        activeModifiers.put(modifierName, operation);
    }

    public boolean removeStatModifier(SkillsUser skillsUser, String modifierName) {
        if(activeModifiers.containsKey(modifierName)) {
            skillsUser.removeStatModifier(modifierName);
            activeModifiers.remove(modifierName);
            return true;
        }
        NexusPlugin.nexusLogger.error("Could not remove stat modifier: " + modifierName);
        NexusPlugin.nexusLogger.error("Make sure, that the modifier is currently active!");
        return false;
    }

    public double getMana(SkillsUser skillsUser) {
        return skillsUser.getMana();
    }

    public boolean consumeMana(SkillsUser skillsUser, double amount) {
        return skillsUser.consumeMana(amount);
    }

    public Skill getSkill(String id) {
        return this.auraSkillsApi.getGlobalRegistry().getSkill(NamespacedId.of("nexus", id));
    }

    public Trait getTrait(String id) {
        return this.auraSkillsApi.getGlobalRegistry().getTrait(NamespacedId.of("nexus", id));
    }

    public Stat getStat(String id) {
        return this.auraSkillsApi.getGlobalRegistry().getStat(NamespacedId.of("nexus", id));
    }

    public NamespacedRegistry getRegistry() {
        return this.auraSkillsApi.useRegistry("nexus", NexusPlugin.getInstance().getDataFolder());
    }

}
