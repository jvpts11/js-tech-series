/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Network Manager says: its tabs, tables, hardware readout, map, node tooltips and the detail of an
 * Operation. Node and machine names, ids, counts and the query language's keywords are data. Kept apart from the
 * window so the language generator can read it on a server too, where windows do not exist.
 */
@TextHolder
final class NetworkManagerTexts {

    // The tabs.
    static final TextKey DEVICES = TextKey.of("jsc.network_manager.devices", "Devices");
    static final TextKey PROCESSES = TextKey.of("jsc.network_manager.processes", "Processes");
    static final TextKey HARDWARE = TextKey.of("jsc.network_manager.hardware", "Hardware");
    static final TextKey MAP = TextKey.of("jsc.network_manager.map", "Map");
    static final TextKey LOG = TextKey.of("jsc.network_manager.log", "Log");
    static final TextKey STATS = TextKey.of("jsc.network_manager.stats", "Stats");
    static final TextKey LOADING = TextKey.of("jsc.network_manager.loading", "Loading network...");
    static final TextKey NETWORK_LINE = TextKey.of("jsc.network_manager.network_line", "Network %s   -   %s node(s)");
    static final TextKey NO_NETWORK = TextKey.of("jsc.network_manager.no_network", "(none)");

    // Devices.
    static final TextKey NODE_COLUMN = TextKey.of("jsc.network_manager.node_column", "NODE");
    static final TextKey TYPE_COLUMN = TextKey.of("jsc.network_manager.type_column", "TYPE");
    static final TextKey STATUS_COLUMN = TextKey.of("jsc.network_manager.status_column", "STATUS");
    static final TextKey UNNAMED = TextKey.of("jsc.network_manager.unnamed", "unnamed");
    static final TextKey ONLINE = TextKey.of("jsc.network_manager.online", "online");
    static final TextKey OFFLINE = TextKey.of("jsc.network_manager.offline", "offline");

    // Processes.
    static final TextKey CRAFT_SLOTS = TextKey.of("jsc.network_manager.craft_slots", "Craft slots  %s / %s");
    static final TextKey RUNNING_COUNT = TextKey.of("jsc.network_manager.running_count", "%s running");
    static final TextKey NO_OPERATIONS = TextKey.of("jsc.network_manager.no_operations", "No Operations in flight.");

    // Hardware.
    static final TextKey ORCHESTRATION = TextKey.of("jsc.network_manager.orchestration", "Orchestration capacity");
    static final TextKey PER_TICK = TextKey.of("jsc.network_manager.per_tick", "%s it/t");
    static final TextKey QUEUES = TextKey.of("jsc.network_manager.queues", "Parallel queues");
    static final TextKey RAM_BUFFER = TextKey.of("jsc.network_manager.ram_buffer", "RAM buffer");
    static final TextKey ITEM_EQUIVALENTS = TextKey.of("jsc.network_manager.item_equivalents", "%s it");
    static final TextKey NETWORK_STORAGE = TextKey.of("jsc.network_manager.network_storage", "Network storage");
    static final TextKey ITEMS = TextKey.of("jsc.network_manager.items", "%s items");
    static final TextKey MAINFRAMES = TextKey.of("jsc.network_manager.mainframes", "Mainframes");
    static final TextKey SERVERS = TextKey.of("jsc.network_manager.servers", "Servers");
    static final TextKey SUBFRAMES = TextKey.of("jsc.network_manager.subframes", "Subframes");
    static final TextKey SUPERCOMPUTERS = TextKey.of("jsc.network_manager.supercomputers", "Supercomputers");
    static final TextKey CRAFTING_COMPUTERS =
            TextKey.of("jsc.network_manager.crafting_computers", "Crafting computers");
    static final TextKey PERSONAL_COMPUTERS =
            TextKey.of("jsc.network_manager.personal_computers", "Personal computers");
    static final TextKey CLUSTER_MANAGERS = TextKey.of("jsc.network_manager.cluster_managers", "Cluster managers");
    static final TextKey NODES = TextKey.of("jsc.network_manager.nodes", "NODES");

    // The map and a node's tooltip.
    static final TextKey MAP_HINT =
            TextKey.of("jsc.network_manager.map_hint", "drag nodes  -  middle-drag to pan  -  wheel to zoom");
    static final TextKey KIND_AND_STATE = TextKey.of("jsc.network_manager.kind_and_state", "%s  -  %s");
    static final TextKey CPU = TextKey.of("jsc.network_manager.cpu", "CPU %s");
    static final TextKey CPU_AND_VRAM = TextKey.of("jsc.network_manager.cpu_and_vram", "CPU %s   VRAM %s MB");
    static final TextKey OS = TextKey.of("jsc.network_manager.os", "OS %s");
    static final TextKey STORAGE_OF = TextKey.of("jsc.network_manager.storage_of", "Storage %s / %s MB free");
    static final TextKey STORAGE_FREE = TextKey.of("jsc.network_manager.storage_free", "Storage %s MB free");
    static final TextKey PRIVATE = TextKey.of("jsc.network_manager.private", "Private %s%%");
    static final TextKey ID = TextKey.of("jsc.network_manager.id", "id %s");

    // Log and stats.
    static final TextKey NO_LOG = TextKey.of("jsc.network_manager.no_log", "No Operations logged yet.");
    static final TextKey NO_STATS =
            TextKey.of("jsc.network_manager.no_stats", "No Operations settled in the last hour.");
    static final TextKey OPS_PER_HOUR = TextKey.of("jsc.network_manager.ops_per_hour", "OPS/H");
    static final TextKey WAIT = TextKey.of("jsc.network_manager.wait", "WAIT");
    static final TextKey RUN = TextKey.of("jsc.network_manager.run", "RUN");
    static final TextKey FAIL = TextKey.of("jsc.network_manager.fail", "FAIL");
    static final TextKey LAST_HOUR = TextKey.of("jsc.network_manager.last_hour",
            "Last hour: %s items moved   -   peak %s in flight today");
    static final TextKey SECONDS = TextKey.of("jsc.network_manager.seconds", "%ss");
    static final TextKey TICKS = TextKey.of("jsc.network_manager.ticks", "%st");

    // An Operation's detail.
    static final TextKey CLOSE = TextKey.of("jsc.network_manager.close", "Close");
    static final TextKey CANCEL = TextKey.of("jsc.network_manager.cancel", "Cancel");
    static final TextKey PRIORITY = TextKey.of("jsc.network_manager.priority", "PRIORITY");
    static final TextKey ALL = TextKey.of("jsc.network_manager.all", "all");
    static final TextKey AMOUNT = TextKey.of("jsc.network_manager.amount", "%s of %s   %s");
    static final TextKey TIMING = TextKey.of("jsc.network_manager.timing", "waited %s, ran %s");
    static final TextKey STAGES = TextKey.of("jsc.network_manager.stages", "STAGES");
    static final TextKey SOURCES = TextKey.of("jsc.network_manager.sources", "SOURCES");
    static final TextKey NO_SUB_OPERATIONS = TextKey.of("jsc.network_manager.no_sub_operations", "No sub-operations.");

    // What state an Operation is in.
    static final TextKey DONE = TextKey.of("jsc.network_manager.done", "done");
    static final TextKey PARTIAL = TextKey.of("jsc.network_manager.partial", "partial");
    static final TextKey FAILED = TextKey.of("jsc.network_manager.failed", "failed");
    static final TextKey RUNNING = TextKey.of("jsc.network_manager.running", "running");
    static final TextKey WAITING = TextKey.of("jsc.network_manager.waiting", "waiting");
    static final TextKey LOCKED = TextKey.of("jsc.network_manager.locked", "locked");
    static final TextKey QUEUED = TextKey.of("jsc.network_manager.queued", "queued");
    static final TextKey DISCARDED = TextKey.of("jsc.network_manager.discarded", "discarded");

    private NetworkManagerTexts() {
    }
}
