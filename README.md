# Nexus

**Nexus** is a modular Minecraft platform project that powers the **Nex** plugin series across multiple runtimes (**Paper** and proxy platforms).  
It provides a shared, config-driven core with platform-specific adapters and optional integrations

> **Runtime requirements:** Java 21

---

## Modules

- **`nexus-core`** (Paper/Bukkit)  
  Main server-side core: effects, actions, conditions, language/messaging, inventories, placeholders, hooks, database/redis, levels, utilities, etc.

- **`nexus-nms`** (NMS adapters)  
  Version-specific internals split into submodules per Minecraft version.

- **`nexus-velocity`** (Velocity proxy)  
  Proxy bootstrap, Brigadier command structure, comment-safe config handling and updates.

- **`nexus-bungee`** (BungeeCord proxy)  
  Proxy module for BungeeCord integration.

---

## Features (High-Level)

### Core Platform (Paper / `nexus-core`)
- **Config system**
    - YAML-based configuration files with reload support.
    - File readers for languages, inventories, and other namespaces.

- **Language & messaging**
    - Per-player language selection.
    - Translation lookup for single messages and lists.
    - Prefix handling and MiniMessage formatting.

- **Placeholder system**
    - Internal placeholder registry/bootstrap.
    - Optional PlaceholderAPI integration.

- **Action system**
    - Configurable actions executed in a unified pipeline (e.g. send messages, run commands, economy actions, EXP).
    - Optional delayed execution support.

- **Condition system**
    - Configurable condition checks used by other systems (filters/visibility/requirements).

- **Effect system**
    - Effect bindings with triggers and filters.
    - Built-in trigger/filter registration.
    - Runtime statistics logging (effects/bindings/triggers/filters).

- **Inventory system**
    - Namespaced inventory registry built from YAML definitions.
    - Reload pipeline (close/reopen views, rebuild registry).

- **Packet-based UI features (client-side only)**
    - **Packet-based item lore**: send fake items with custom lore to the client without modifying the server-side item.
    - **Packet-based holograms**: render holograms via packets (client-side displays), enabling lightweight and flexible visuals.

- **NMS custom mobs**
    - Versioned NMS adapters for custom mob behaviour and internals (kept in `nexus-nms` modules).

- **Dialog & UI utilities**
    - Dialog builders/adapters (platform-dependent).
    - Shared UI helpers used by the Nex series.

- **Damage indicator**
    - Config-driven floating damage indicators with formatting, conditions and styling.

- **Hologram service**
    - Hologram builder/service integration for visual output.

- **Level system**
    - Level service with preload and scheduled flushing.
    - Database-backed persistence.

- **Database layer**
    - Storage backends: SQLite / MySQL / MariaDB.
    - Pooled connections via HikariCP.
    - Exposed database service registration.

- **Redis (optional)**
    - Optional Redis connection and service registration.
    - Used for cross-server communication when enabled.

- **Cross-server configuration**
    - Cross-server settings + sanity checks (e.g. server-name validation).

- **Third-party hook system**
    - Optional integrations (when plugins are present), e.g.:
        - PlaceholderAPI, Vault
        - LuckPerms
        - WorldGuard
        - MythicMobs
        - ItemsAdder
        - AuraSkills / EcoSkills / EcoItems
        - MMOItems
        - AuroraCollections
        - (and other Nex series hooks)

### Proxy Platform (Velocity / `nexus-velocity`)
- **Brigadier command structure**
    - Root command with subcommands (e.g. `/nexusproxy reload`).

- **Comment-preserving config workflow**
    - Default configs are copied byte-for-byte on first start.
    - Reload reads configs without re-emitting YAML.
    - Optional updater patches missing keys/blocks without destroying comments.

---