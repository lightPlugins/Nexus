package io.nexstudios.nexus.bukkit.conditions;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.NexParams;
import io.nexstudios.nexus.bukkit.conditions.impl.*;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter
public class ConditionFactory {

    private final HashMap<String, NexusCondition> availableConditions;

    public ConditionFactory() {
        this.availableConditions = new HashMap<>();
        registerInternalConditions();
    }

    private void registerInternalConditions() {

        availableConditions.put("above-block", new ConditionAboveBlock());
        availableConditions.put("in-world", new ConditionInWorld());
        availableConditions.put("has-age", new ConditionHasAge());

        if (NexusPlugin.getInstance().getWorldGuardHook() != null) {
            availableConditions.put("in-region", new ConditionInRegion());
        }

        // auto use 'permission' for bukkit or luckperms
        if (NexusPlugin.getInstance().getLuckPermsHook() != null) {
            availableConditions.put("permission", new ConditionLuckPermission());
        } else {
            availableConditions.put("permission", new ConditionPermission());
        }
    }

    public boolean registerCondition(String id, NexusCondition condition) {
        if (this.availableConditions.containsKey(id)) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Third party plugin tried to register an condition with id: " + id,
                    "Condition with id " + id + " already exists!"
            ));
            return false;
        }

        this.availableConditions.put(id, condition);
        return true;
    }

    public NexusCondition getCondition(String conditionID) {
        return this.availableConditions.get(conditionID);
    }

    /**
     * Neuer Builder für Condition-Auswertungen.
     */
    public ConditionChainBuilder newBuilder() {
        return new ConditionChainBuilder(this);
    }

    /**
     * Alte synchrone API – optional als Übergang behalten.
     * Nutzt intern den asynchronen Builder und blockiert bis zum Ergebnis.
     */
    @Deprecated
    public boolean checkConditions(Player player, @Nullable Location targetLocation, List<Map<String, Object>> conditions) {

        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        OfflinePlayer subject = player; // Player ist auch OfflinePlayer

        try {
            return newBuilder()
                    .subject(subject)
                    .location(targetLocation)
                    .conditions(conditions)
                    .params(NexParams.empty())
                    .evaluateAsync()
                    .join(); // Achtung: blockierend – nur für Legacy-Verwendung
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Error while checking conditions (legacy API).",
                    "Error: " + e.getMessage()
            ));
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Builder-Klasse für asynchrone Condition-Ketten.
     */
    public static final class ConditionChainBuilder {

        private final ConditionFactory factory;

        private OfflinePlayer subject;
        private Location location;
        private List<Map<String, Object>> rawConditions;
        private NexParams params = NexParams.empty();

        private ConditionChainBuilder(ConditionFactory factory) {
            this.factory = factory;
        }

        public ConditionChainBuilder subject(OfflinePlayer subject) {
            this.subject = subject;
            return this;
        }

        public ConditionChainBuilder player(Player player) {
            this.subject = player;
            return this;
        }

        public ConditionChainBuilder location(@Nullable Location location) {
            this.location = location;
            return this;
        }

        public ConditionChainBuilder conditions(List<Map<String, Object>> conditions) {
            this.rawConditions = conditions;
            return this;
        }

        public ConditionChainBuilder params(NexParams params) {
            this.params = (params == null ? NexParams.empty() : params);
            return this;
        }

        public ConditionChainBuilder param(String key, String value) {
            if (this.params == null) {
                this.params = NexParams.builder().put(key, value).build();
            } else {
                this.params = this.params.with(key, value);
            }
            return this;
        }

        /**
         * Asynchrone Auswertung aller Conditions.
         * Gibt true zurück, wenn mindestens eine Condition geprüft wurde
         * und keine davon fehlgeschlagen ist.
         */
        public CompletableFuture<Boolean> evaluateAsync() {

            if (rawConditions == null || rawConditions.isEmpty()) {
                return CompletableFuture.completedFuture(true);
            }
            if (subject == null) {
                return CompletableFuture.completedFuture(false);
            }

            return evaluateIndex(0, false);
        }

        private CompletableFuture<Boolean> evaluateIndex(int index, boolean anyConditionChecked) {
            if (rawConditions == null || index >= rawConditions.size()) {
                // keine weiteren Conditions
                return CompletableFuture.completedFuture(anyConditionChecked);
            }

            Map<String, Object> conditionMap = rawConditions.get(index);
            Object idObj = conditionMap.get("id");
            if (!(idObj instanceof String conditionID)) {
                NexusPlugin.nexusLogger.warning("Condition entry without valid 'id' in Condition Section");
                // nächste Condition
                return evaluateIndex(index + 1, anyConditionChecked);
            }

            NexusCondition condition = factory.getCondition(conditionID);
            if (condition == null) {
                NexusPlugin.nexusLogger.warning("Unknown condition: " + conditionID + " in Condition Section");
                // nächste Condition
                return evaluateIndex(index + 1, anyConditionChecked);
            }

            ConditionData conditionData = new ConditionData();
            conditionData.getData().putAll(conditionMap);
            anyConditionChecked = true;

            NexusConditionContext ctx = new NexusConditionContext(subject, location, conditionData, params);

            try {
                return condition.checkAsync(ctx).thenCompose(result -> {
                    if (!result) {
                        try {
                            condition.sendMessage(ctx);
                        } catch (Exception ex) {
                            NexusPlugin.nexusLogger.error(List.of(
                                    "Error while sending condition message: " + conditionID,
                                    "Error: " + ex.getMessage()
                            ));
                            ex.printStackTrace();
                        }
                        // Kette abbrechen
                        return CompletableFuture.completedFuture(false);
                    }
                    // nächste Condition
                    return evaluateIndex(index + 1, true);
                });
            } catch (Exception e) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Error while checking condition: " + conditionID,
                        "Error: " + e.getMessage()
                ));
                e.printStackTrace();
                return CompletableFuture.completedFuture(false);
            }
        }
    }
}