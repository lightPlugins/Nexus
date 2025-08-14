package io.nexstudios.nexus.bukkit.effects;

import java.util.List;
import java.util.Map;

public record EffectBinding(NexusEffect effect, List<TriggerSpec> triggers, Map<String, Object> rawConfig) {}

