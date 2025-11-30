package io.nexstudios.internal.nms.v1_21_8.dialog;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.NexParams;
import io.nexstudios.nexus.bukkit.dialog.NexDialogBuilder;
import io.nexstudios.nexus.bukkit.dialog.NexDialogResult;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PaperDialogBuilder implements NexDialogBuilder {


    private String dialogId = "dialog";

    private Component title = Component.text("Dialog");
    private Component subtitle = Component.empty();
    private final List<Component> bodyLines = new ArrayList<>();

    private String rawTitleMiniMessage = "Dialog";
    private String rawSubtitleMiniMessage = "";
    private final List<String> rawBodyLines = new ArrayList<>();

    private final List<DialogInput> inputs = new ArrayList<>();
    private final List<ButtonDef> buttons = new ArrayList<>();
    private final Set<String> dialogKeys = new LinkedHashSet<>();

    private boolean closeOnSubmit = true;
    private boolean closeOnCancel = true;
    private int buttonColumns = 1;

    private String primaryTextKey = "value";

    private DialogSubmitHandler submitHandler;
    private Consumer<Player> cancelHandler;

    private static final ConcurrentHashMap<UUID, PaperDialogBuilder> OPEN_DIALOGS = new ConcurrentHashMap<>();

    private record ButtonDef(
            DialogButtonRole role,
            Component label,
            Component tooltip,
            int weight,
            boolean closeOnClick,
            List<Map<String, Object>> actions
    ) {}

    private record ContentEntry(
            Map<String, Object> raw,
            List<Map<String, Object>> conditions
    ) {}

    private final Map<String, InputKind> inputKinds = new HashMap<>();

    private enum InputKind {
        TEXT,
        NUMBER,
        BOOL
    }

    private final List<ContentEntry> bodyContentEntries = new ArrayList<>();
    private final List<ContentEntry> exitContentEntries = new ArrayList<>();


    public static PaperDialogBuilder create() {
        return new PaperDialogBuilder();
    }

    public PaperDialogBuilder() {
    }

    @Override
    public NexDialogBuilder id(String id) {
        if (id != null && !id.isBlank()) {
            this.dialogId = id;
        }
        return this;
    }

    @Override
    public NexDialogBuilder title(Component title) {
        if (title != null) this.title = title;
        return this;
    }

    @Override
    public NexDialogBuilder titleMiniMessage(String miniMessage) {
        if (miniMessage != null && !miniMessage.isBlank()) {
            this.rawTitleMiniMessage = miniMessage;
            this.title = MiniMessage.miniMessage().deserialize(miniMessage);
        }
        return this;
    }

    @Override
    public NexDialogBuilder subtitle(Component subtitle) {
        this.subtitle = (subtitle == null ? Component.empty() : subtitle);
        return this;
    }

    @Override
    public NexDialogBuilder subtitleMiniMessage(String miniMessage) {
        this.rawSubtitleMiniMessage = (miniMessage == null ? "" : miniMessage);
        if (miniMessage == null || miniMessage.isBlank()) {
            this.subtitle = Component.empty();
        } else {
            this.subtitle = MiniMessage.miniMessage().deserialize(miniMessage);
        }
        return this;
    }

    @Override
    public NexDialogBuilder clearBody() {
        this.bodyLines.clear();
        this.rawBodyLines.clear();
        return this;
    }

    @Override
    public NexDialogBuilder addBodyLine(Component line) {
        if (line != null) this.bodyLines.add(line);
        return this;
    }

    @Override
    public NexDialogBuilder addBodyLineMiniMessage(String miniMessage) {
        if (miniMessage == null) return this;
        this.rawBodyLines.add(miniMessage);
        this.bodyLines.add(MiniMessage.miniMessage().deserialize(miniMessage));
        return this;
    }

    @Override
    public NexDialogBuilder bodyMiniMessageLines(List<String> lines) {
        this.bodyLines.clear();
        this.rawBodyLines.clear();
        if (lines != null) {
            for (String l : lines) {
                if (l == null) continue;
                this.rawBodyLines.add(l);
                this.bodyLines.add(MiniMessage.miniMessage().deserialize(l));
            }
        }
        return this;
    }

    @Override
    public NexDialogBuilder addTextInput(String key,
                                         Component label,
                                         String prefill,
                                         int width) {
        if (key == null || key.isBlank()) key = "value";
        if (label == null) label = Component.empty();
        if (prefill == null) prefill = "";
        if (width <= 0) width = 300;

        DialogInput input = DialogInput.text(key, label)
                .initial(prefill)
                .width(width)
                .build();

        this.inputs.add(input);
        return this;
    }

    @Override
    public NexDialogBuilder addButton(DialogButtonRole role,
                                      Component label,
                                      Component tooltip,
                                      int weight,
                                      boolean closeOnClick) {
        if (role == null) role = DialogButtonRole.CUSTOM;
        if (label == null) label = Component.text("Button");
        if (tooltip == null) tooltip = Component.empty();
        if (weight <= 0) weight = 100;

        this.buttons.add(new ButtonDef(role, label, tooltip, weight, closeOnClick, List.of()));
        return this;
    }

    @Override
    public NexDialogBuilder closeOnSubmit(boolean closeOnSubmit) {
        this.closeOnSubmit = closeOnSubmit;
        return this;
    }

    @Override
    public NexDialogBuilder closeOnCancel(boolean closeOnCancel) {
        this.closeOnCancel = closeOnCancel;
        return this;
    }

    @Override
    public NexDialogBuilder primaryTextKey(String key) {
        if (key != null && !key.isBlank()) {
            this.primaryTextKey = key;
        }
        return this;
    }

    @Override
    public NexDialogBuilder fromConfig(ConfigurationSection dialogSection) {
        Objects.requireNonNull(dialogSection, "dialogSection");

        // id
        String id = dialogSection.getString("id");
        if (id != null && !id.isBlank()) this.dialogId = id;

        // title + subtitle (roh speichern; PAPI erfolgt in open(player))
        this.rawTitleMiniMessage = dialogSection.getString("title", "Dialog");
        this.rawSubtitleMiniMessage = dialogSection.getString("subtitle", "");

        // body (roh speichern)
        List<String> bodyStrings = dialogSection.getStringList("body");
        this.rawBodyLines.clear();
        this.bodyLines.clear();
        for (String line : bodyStrings) {
            if (line == null) continue;
            this.rawBodyLines.add(line);
        }

        // options
        ConfigurationSection options = dialogSection.getConfigurationSection("options");
        if (options != null) {
            this.closeOnSubmit = options.getBoolean("close-on-submit", true);
            this.closeOnCancel = options.getBoolean("close-on-cancel", true);
            this.buttonColumns = options.getInt("button-columns", 1);
            if (this.buttonColumns <= 0) this.buttonColumns = 1;
        } else {
            this.buttonColumns = 1;
        }

        // raw content entries (getrennt: body-content / exit-content)
        this.inputs.clear();
        this.buttons.clear();
        this.bodyContentEntries.clear();
        this.exitContentEntries.clear();
        this.dialogKeys.clear();
        this.inputKinds.clear();

        List<Map<?, ?>> bodyContent = dialogSection.getMapList("body-content");
        for (Map<?, ?> rawElement : bodyContent) {
            if (!(rawElement instanceof Map<?, ?> map)) continue;

            Map<String, Object> normalized = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null) continue;
                normalized.put(String.valueOf(e.getKey()), e.getValue());
            }

            List<Map<String, Object>> conditions = new ArrayList<>();
            Object rawConds = normalized.get("conditions");
            if (rawConds instanceof List<?> list) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    Map<String, Object> cond = new HashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        if (e.getKey() == null) continue;
                        cond.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    conditions.add(cond);
                }
            }

            bodyContentEntries.add(new ContentEntry(
                    Collections.unmodifiableMap(normalized),
                    conditions.isEmpty() ? List.of() : List.copyOf(conditions)
            ));
        }

        List<Map<?, ?>> exitContent = dialogSection.getMapList("exit-content");
        for (Map<?, ?> rawElement : exitContent) {
            if (!(rawElement instanceof Map<?, ?> map)) continue;

            Map<String, Object> normalized = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() == null) continue;
                normalized.put(String.valueOf(e.getKey()), e.getValue());
            }

            List<Map<String, Object>> conditions = new ArrayList<>();
            Object rawConds = normalized.get("conditions");
            if (rawConds instanceof List<?> list) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    Map<String, Object> cond = new HashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        if (e.getKey() == null) continue;
                        cond.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    conditions.add(cond);
                }
            }

            exitContentEntries.add(new ContentEntry(
                    Collections.unmodifiableMap(normalized),
                    conditions.isEmpty() ? List.of() : List.copyOf(conditions)
            ));
        }

        // primaryTextKey: auto-pick first text-input key from body-content, if any
        for (Map<?, ?> rawElement : bodyContent) {
            if (!(rawElement instanceof Map<?, ?> map)) continue;
            Object typeObj = map.get("type");
            String type = typeObj != null ? String.valueOf(typeObj) : null;
            if (type == null) continue;
            if (type.equalsIgnoreCase("text-input")) {
                Object keyObj = map.get("key");
                if (keyObj != null) {
                    this.primaryTextKey = String.valueOf(keyObj);
                }
                break;
            }
        }

        return this;
    }

    private static DialogButtonRole parseRole(Object rawRole) {
        if (rawRole == null) return DialogButtonRole.CUSTOM;
        String s = rawRole.toString().trim();
        if (s.isEmpty()) return DialogButtonRole.CUSTOM;

        try {
            return DialogButtonRole.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DialogButtonRole.CUSTOM;
        }
    }

    private static String string(Object o, String def) {
        if (o == null) return def;
        String s = String.valueOf(o);
        return s != null ? s : def;
    }

    private static int intOrDefault(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static boolean boolOrDefault(Object o, boolean def) {
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) return Boolean.parseBoolean(s.trim());
        return def;
    }

    private static Component componentOrEmpty(Object o) {
        if (o == null) return Component.empty();
        String s = String.valueOf(o);
        if (s.isBlank()) return Component.empty();
        return MiniMessage.miniMessage().deserialize(s);
    }

    private static Component componentOrEmpty(Player player, Object o) {
        if (o == null) return Component.empty();
        String s = String.valueOf(o);
        if (s.isBlank()) return Component.empty();
        s = StringUtils.parsePlaceholderAPI(player, s);
        return MiniMessage.miniMessage().deserialize(s);
    }

    private static DialogInput buildTextInputFromMapWithPapi(Player player, Map<String, Object> map) {
        String key = string(map.get("key"), "value");
        String rawLabel = string(map.get("label"), "");
        if (player != null) {
            rawLabel = StringUtils.parsePlaceholderAPI(player, rawLabel);
        }
        Component label = componentOrEmpty(rawLabel);
        String prefill = string(map.get("prefill"), "");
        int width = intOrDefault(map.get("width"), 300);

        return DialogInput.text(key, label)
                .initial(prefill)
                .width(width)
                .build();
    }

    private static ButtonDef buildButtonDefFromMapWithPapi(Player player, Map<String, Object> map, DialogButtonRole role) {
        Component label = componentOrEmpty(player, map.get("label"));
        Component tooltip = componentOrEmpty(player, map.get("tooltip"));
        int weight = intOrDefault(map.get("weight"), 100);
        boolean closeOnClick = boolOrDefault(map.get("close-on-click"), true);

        List<Map<String, Object>> actions = new ArrayList<>();
        Object rawActions = map.get("actions");
        if (rawActions instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) continue;
                Map<String, Object> actionMap = new HashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getKey() == null) continue;
                    actionMap.put(String.valueOf(e.getKey()), e.getValue());
                }

                if (!actionMap.containsKey("id") && actionMap.containsKey("type")) {
                    actionMap.put("id", actionMap.get("type"));
                }

                actions.add(actionMap);
            }
        }

        return new ButtonDef(
                role,
                label,
                tooltip,
                weight,
                closeOnClick,
                actions.isEmpty() ? List.of() : List.copyOf(actions)
        );
    }

    private static DialogInput buildBoolInputFromMapWithPapi(Player player, Map<String, Object> map) {
        String key = string(map.get("key"), "value");
        String rawLabel = string(map.get("label"), "");
        if (player != null) {
            rawLabel = StringUtils.parsePlaceholderAPI(player, rawLabel);
        }
        Component label = componentOrEmpty(rawLabel);
        boolean def = boolOrDefault(map.get("default"), false);

        String onTrue = "true";
        String onFalse = "false";

        return DialogInput.bool(key, label, def, onTrue, onFalse);
    }

    private static DialogInput buildNumberRangeInputFromMap(Map<String, Object> map, Player player) {
        String key = string(map.get("key"), "value");

        String rawLabel = string(map.get("label"), "");
        if (player != null) {
            rawLabel = StringUtils.parsePlaceholderAPI(player, rawLabel);
        }
        Component label = componentOrEmpty(rawLabel);

        float min = 0f;
        float max = 100f;

        Object minObj = map.get("min");
        if (minObj != null) {
            try {
                min = Float.parseFloat(String.valueOf(minObj));
            } catch (NumberFormatException ignored) {}
        }
        Object maxObj = map.get("max");
        if (maxObj != null) {
            try {
                max = Float.parseFloat(String.valueOf(maxObj));
            } catch (NumberFormatException ignored) {}
        }

        Float initial = null;
        Object prefillObj = map.get("prefill");
        if (prefillObj != null) {
            try {
                float pf = Float.parseFloat(String.valueOf(prefillObj));
                if (pf < min) pf = min;
                if (pf > max) pf = max;
                initial = pf;
            } catch (NumberFormatException ignored) {}
        }

        Float step = null;
        Object stepObj = map.get("step");
        if (stepObj != null) {
            try {
                float st = Float.parseFloat(String.valueOf(stepObj));
                if (st > 0) step = st;
            } catch (NumberFormatException ignored) {}
        }

        int width = intOrDefault(map.get("width"), 200);

        String explicitFormat = string(map.get("label-format"), "%s");
        String labelFormat;
        if (explicitFormat != null && !explicitFormat.isBlank()) {
            String fmt = explicitFormat;
            fmt = fmt
                    .replace("<min>", String.valueOf(min))
                    .replace("<max>", String.valueOf(max));
            labelFormat = fmt;
        } else {
            labelFormat = "%s";
        }

        var builder = DialogInput
                .numberRange(key, label, min, max)
                .width(width)
                .labelFormat(labelFormat)
                .initial(initial)
                .step(step);

        return builder.build();
    }

    @Override
    public NexDialogBuilder onSubmit(DialogSubmitHandler handler) {
        this.submitHandler = handler;
        return this;
    }

    @Override
    public NexDialogBuilder onCancel(Consumer<Player> handler) {
        this.cancelHandler = handler;
        return this;
    }

    @Override
    public void open(Player player) {
        Objects.requireNonNull(player, "player");

        UUID uuid = player.getUniqueId();
        PaperDialogBuilder existing = OPEN_DIALOGS.put(uuid, this);
        if (existing != null && existing != this) {
            existing.internalCancel(player);
        }

        // Titel + Untertitel + Body mit PAPI + MiniMessage aufbauen
        {
            String titleRaw = (rawTitleMiniMessage != null ? rawTitleMiniMessage : "Dialog");
            titleRaw = StringUtils.parsePlaceholderAPI(player, titleRaw);
            this.title = MiniMessage.miniMessage().deserialize(titleRaw);

            String subtitleRaw = (rawSubtitleMiniMessage != null ? rawSubtitleMiniMessage : "");
            subtitleRaw = StringUtils.parsePlaceholderAPI(player, subtitleRaw);
            if (subtitleRaw.isBlank()) {
                this.subtitle = Component.empty();
            } else {
                this.subtitle = MiniMessage.miniMessage().deserialize(subtitleRaw);
            }

            this.bodyLines.clear();
            for (String line : rawBodyLines) {
                if (line == null) continue;
                String parsed = StringUtils.parsePlaceholderAPI(player, line);
                this.bodyLines.add(MiniMessage.miniMessage().deserialize(parsed));
            }
        }

        // rebuild inputs/buttons based on conditions
        this.inputs.clear();
        this.buttons.clear();

        // BODY-CONTENT verarbeiten
        for (ContentEntry entry : bodyContentEntries) {
            Map<String, Object> raw = entry.raw();
            List<Map<String, Object>> conditions = entry.conditions();

            if (!conditionsPass(player, conditions)) {
                continue;
            }

            Object typeObj = raw.get("type");
            String type = typeObj != null ? String.valueOf(typeObj) : null;
            if (type == null) continue;

            switch (type.toLowerCase(Locale.ROOT)) {
                case "text-input" -> {
                    DialogInput input = buildTextInputFromMapWithPapi(player, raw);
                    this.inputs.add(input);
                    String key = string(raw.get("key"), "value");
                    dialogKeys.add(key);
                    inputKinds.put(key, InputKind.TEXT);
                }
                case "slider", "number-input" -> {
                    DialogInput input = buildNumberRangeInputFromMap(raw, player);
                    this.inputs.add(input);
                    String key = string(raw.get("key"), "value");
                    dialogKeys.add(key);
                    inputKinds.put(key, InputKind.NUMBER);
                }
                case "checkbox", "toggle" -> {
                    DialogInput input = buildBoolInputFromMapWithPapi(player, raw);
                    this.inputs.add(input);
                    String key = string(raw.get("key"), "value");
                    dialogKeys.add(key);
                    inputKinds.put(key, InputKind.BOOL);
                }
                case "button" -> {
                    DialogButtonRole role = parseRole(raw.get("role"));
                    ButtonDef def = buildButtonDefFromMapWithPapi(player, raw, role);
                    this.buttons.add(def);
                }
                case "info-text" -> {
                    Object textObj = raw.get("text");
                    if (textObj instanceof List<?> list) {
                        for (Object o : list) {
                            if (o == null) continue;
                            String line = String.valueOf(o);
                            line = StringUtils.parsePlaceholderAPI(player, line);
                            this.bodyLines.add(
                                    MiniMessage.miniMessage().deserialize(line)
                            );
                        }
                    }
                }
                case "separator" -> {
                    // optional
                }
                default -> {
                    // andere Typen später
                }
            }
        }

        // Body-Komponente bauen
        Component bodyComp;
        if (bodyLines.isEmpty()) {
            bodyComp = Component.empty();
        } else if (bodyLines.size() == 1) {
            bodyComp = bodyLines.get(0);
        } else {
            Component combined = Component.empty();
            for (Component line : bodyLines) {
                if (!combined.equals(Component.empty())) {
                    combined = combined.append(Component.newline());
                }
                combined = combined.append(line);
            }
            bodyComp = combined;
        }

        DialogBase base = DialogBase.builder(this.title)
                .body(List.of(DialogBody.plainMessage(bodyComp)))
                .inputs(this.inputs)
                .build();

        // Buttons aus body-content
        List<ActionButton> actionButtons = new ArrayList<>();

        for (ButtonDef def : this.buttons) {
            switch (def.role()) {
                case SUBMIT -> actionButtons.add(wrapSubmitButton(player, def));
                case CANCEL -> actionButtons.add(wrapCancelButton(player, def));
                case CUSTOM -> {
                    DialogAction action = DialogAction.customClick(
                            (DialogResponseView view, Audience audience) -> {
                                if (audience instanceof Player p && p.getUniqueId().equals(player.getUniqueId())) {
                                    runButtonActions(p, def, view);
                                }
                            },
                            ClickCallback.Options.builder().uses(1).build()
                    );
                    actionButtons.add(ActionButton.create(def.label(), def.tooltip(), def.weight(), action));
                }
            }
        }

        // Exit-Buttons aus exit-content
        List<ButtonDef> exitButtons = new ArrayList<>();
        for (ContentEntry entry : exitContentEntries) {
            Map<String, Object> raw = entry.raw();
            if (!"button".equalsIgnoreCase(String.valueOf(raw.get("type")))) continue;
            if (!conditionsPass(player, entry.conditions())) continue;

            DialogButtonRole role = parseRole(raw.get("role"));
            ButtonDef def = buildButtonDefFromMapWithPapi(player, raw, role);
            exitButtons.add(def);
        }

        Dialog dialog;

        // Spezialfall: genau 2 Exit-Buttons und KEINE weiteren Buttons -> Confirmation-Dialog (Footer)
        if (exitButtons.size() == 2 && actionButtons.isEmpty()) {
            ButtonDef yesDef = exitButtons.get(0);
            ButtonDef noDef  = exitButtons.get(1);

            ActionButton yesButton = (yesDef.role() == DialogButtonRole.CANCEL)
                    ? wrapCancelButton(player, yesDef)
                    : wrapSubmitButton(player, yesDef);

            ActionButton noButton = (noDef.role() == DialogButtonRole.CANCEL)
                    ? wrapCancelButton(player, noDef)
                    : wrapSubmitButton(player, noDef);

            dialog = Dialog.create(builder -> builder.empty()
                    .base(base)
                    .type(DialogType.confirmation(yesButton, noButton))
            );
        } else {
            // Standard: alle Buttons in ein multiAction-Grid
            for (ButtonDef def : exitButtons) {
                if (def.role() == DialogButtonRole.SUBMIT) {
                    actionButtons.add(wrapSubmitButton(player, def));
                } else {
                    actionButtons.add(wrapCancelButton(player, def));
                }
            }

            if (actionButtons.isEmpty()) {
                NexusPlugin.nexusLogger.warning("Dialog '" + dialogId + "' hat keine Buttons in der Config definiert.");
            }

            int columns = this.buttonColumns <= 0 ? 1 : this.buttonColumns;

            dialog = Dialog.create(builder -> builder.empty()
                    .base(base)
                    .type(DialogType.multiAction(actionButtons, null, columns))
            );
        }

        player.showDialog(dialog);
    }

    /**
     * Evaluates the "conditions" list for a content entry.
     * Delegates to the core ConditionFactory.
     */
    private boolean conditionsPass(Player player, List<Map<String, Object>> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            // no conditions -> always visible
            return true;
        }

        try {
            return NexusPlugin.getInstance()
                    .getConditionFactory()
                    .checkConditions(player, player.getLocation(), conditions);
        } catch (Exception ex) {
            NexusPlugin.nexusLogger.error("Failed to evaluate dialog conditions for player " + player.getName());
            ex.printStackTrace();
            // fail-safe: hide element if conditions cannot be evaluated
            return false;
        }
    }

    /**
     * Executes configured actions for a button using the ActionFactory.
     * Optionally passes dialog values (e.g. text inputs) to the action data.
     */
    private void runButtonActions(Player player, ButtonDef def, DialogResponseView view) {
        if (def.actions == null || def.actions.isEmpty()) return;

        // 1) Alle Dialog-Werte einsammeln
        Map<String, Object> dialogValues = new HashMap<>();

        if (view != null) {
            for (String key : dialogKeys) {
                if (key == null || key.isBlank()) continue;

                InputKind kind = inputKinds.getOrDefault(key, InputKind.TEXT);
                Object value = null;

                switch (kind) {
                    case BOOL -> {
                        Boolean b = view.getBoolean(key);
                        if (b != null) value = b;
                    }
                    case NUMBER -> {
                        Float f = view.getFloat(key);
                        if (f != null) value = f;
                    }
                    case TEXT -> {
                        String text = view.getText(key);
                        if (text != null) value = text;
                    }
                }

                // Fallback: wenn der "richtige" Getter nichts liefert, versuche Text
                if (value == null) {
                    String text = view.getText(key);
                    if (text != null) value = text;
                }

                if (value != null) {
                    dialogValues.put(key, value);
                }
            }
        }

        // 2) Actions-Daten anreichern
        List<Map<String, Object>> actionData = new ArrayList<>();
        for (Map<String, Object> a : def.actions) {
            Map<String, Object> copy = new HashMap<>(a);
            if (!dialogValues.isEmpty()) {
                copy.put("dialog-values", dialogValues);
            }
            actionData.add(copy);
        }

        // 3) TagResolver aus allen Werten bauen
        TagResolver resolver;
        if (dialogValues.isEmpty()) {
            resolver = TagResolver.empty();
        } else {
            List<TagResolver> list = new ArrayList<>();
            for (Map.Entry<String, Object> entry : dialogValues.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (key == null || key.isBlank() || val == null) continue;

                String asString = String.valueOf(val);
                list.add(Placeholder.parsed(key, asString));
            }
            resolver = TagResolver.resolver(list);
        }

        Map<String, String> paramMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : dialogValues.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) continue;
            paramMap.put(key, string(entry.getValue(), ""));
        }

        NexParams params = NexParams.of(paramMap, resolver);

        // 4) Actions mit TagResolver ausführen
        NexusPlugin.getInstance()
                .getActionFactory()
                .newExecution()
                .actor(player)
                .targetLocation(player.getLocation())
                .actions(actionData)
                .params(params)
                .execute();
    }


    private ActionButton wrapSubmitButton(Player player, ButtonDef def) {
        DialogAction action = DialogAction.customClick(
                (DialogResponseView view, Audience audience) -> {
                    if (!(audience instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;

                    fireSubmit(p, view);
                    runButtonActions(p, def, view);

                    if (closeOnSubmit && def.closeOnClick) {
                        // Paper schließt den Dialog in der Regel selbst.
                    }
                },
                ClickCallback.Options.builder().uses(1).build()
        );

        return ActionButton.create(def.label(), def.tooltip(), def.weight(), action);
    }

    private ActionButton wrapCancelButton(Player player, ButtonDef def) {
        DialogAction action = DialogAction.customClick(
                (DialogResponseView view, Audience audience) -> {
                    if (audience instanceof Player p && p.getUniqueId().equals(player.getUniqueId())) {
                        fireCancel(p);
                        runButtonActions(p, def, view);
                    }
                },
                ClickCallback.Options.builder().uses(1).build()
        );

        return ActionButton.create(def.label(), def.tooltip(), def.weight(), action);
    }

    private void fireSubmit(Player player, DialogResponseView response) {
        OPEN_DIALOGS.remove(player.getUniqueId(), this);
        DialogSubmitHandler handler = this.submitHandler;
        if (handler != null) {
            NexDialogResult result = new PaperDialogResult(response);
            Bukkit.getScheduler().runTask(
                    NexusPlugin.getInstance(),
                    () -> handler.accept(player, result)
            );
        }
    }

    private void fireCancel(Player player) {
        OPEN_DIALOGS.remove(player.getUniqueId(), this);
        internalCancel(player);
    }

    private void internalCancel(Player player) {
        Consumer<Player> handler = this.cancelHandler;
        if (handler != null) {
            Bukkit.getScheduler().runTask(
                    NexusPlugin.getInstance(),
                    () -> handler.accept(player)
            );
        }
    }

    public static PaperDialogBuilder getOpenDialog(Player player) {
        if (player == null) return null;
        return OPEN_DIALOGS.get(player.getUniqueId());
    }
}