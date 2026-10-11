/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the assembly screens of the computers say: the slots' names, the state pill, the tiles and the power
 * buttons, most of them shared by every computer. Kept apart from the screens so the language generator can read
 * it on a server too, where screens do not exist.
 */
@TextHolder
final class AssemblyTexts {

    // The name field.
    static final TextKey NAME = TextKey.of("jsc.assembly.name", "Name");
    static final TextKey NAME_THIS_COMPUTER = TextKey.of("jsc.assembly.name_this_computer", "Name this computer...");
    static final TextKey NAME_THIS_PC = TextKey.of("jsc.assembly.name_this_pc", "Name this PC...");
    static final TextKey NAME_THIS_SERVER = TextKey.of("jsc.assembly.name_this_server", "Name this server...");

    // Which computer the screen is.
    static final TextKey TITLE_PC = TextKey.of("jsc.assembly.title_pc", "PC");
    static final TextKey TITLE_CRAFTING = TextKey.of("jsc.assembly.title_crafting", "CC");
    static final TextKey TITLE_CLUSTER_MANAGEMENT = TextKey.of("jsc.assembly.title_cluster_management", "CMC");
    static final TextKey TITLE_MAINFRAME = TextKey.of("jsc.assembly.title_mainframe", "MAINFRAME");
    static final TextKey TITLE_SERVER = TextKey.of("jsc.assembly.title_server", "SERVER");

    // The state pill.
    static final TextKey OFFLINE = TextKey.of("jsc.assembly.offline", "OFFLINE");
    static final TextKey ONLINE = TextKey.of("jsc.assembly.online", "ONLINE");
    static final TextKey READY = TextKey.of("jsc.assembly.ready", "READY");
    static final TextKey CONFLICT = TextKey.of("jsc.assembly.conflict", "CONFLICT");
    /* A passive failover member: powered and synced, not orchestrating. */
    static final TextKey STANDBY = TextKey.of("jsc.assembly.standby", "STANDBY");
    static final TextKey UNASSEMBLED = TextKey.of("jsc.assembly.unassembled", "UNASSEMBLED");
    static final TextKey ERROR = TextKey.of("jsc.assembly.error", "ERROR");
    static final TextKey WARN = TextKey.of("jsc.assembly.warn", "WARN");

    // The slots.
    static final TextKey CPU = TextKey.of("jsc.assembly.cpu", "CPU");
    static final TextKey RAM = TextKey.of("jsc.assembly.ram", "RAM");
    static final TextKey DISK = TextKey.of("jsc.assembly.disk", "DISK");
    static final TextKey GPU = TextKey.of("jsc.assembly.gpu", "GPU");
    static final TextKey PCIE = TextKey.of("jsc.assembly.pcie", "PCIE");

    // The tiles.
    static final TextKey CAPACITY = TextKey.of("jsc.assembly.capacity", "CAPACITY");
    static final TextKey RAM_BUFFER = TextKey.of("jsc.assembly.ram_buffer", "RAM BUFFER");
    static final TextKey QUEUES = TextKey.of("jsc.assembly.queues", "QUEUES");
    static final TextKey GPU_BONUS = TextKey.of("jsc.assembly.gpu_bonus", "GPU BONUS");
    static final TextKey GPU_BONUS_VALUE = TextKey.of("jsc.assembly.gpu_bonus_value", "+%s%% (%s)");
    static final TextKey QUEUE_CPU = TextKey.of("jsc.assembly.queue_cpu", "CPU: %s it/t");
    static final TextKey QUEUE_GPU = TextKey.of("jsc.assembly.queue_gpu", "GPU %s - %s: %s it/t");
    static final TextKey QUEUE_HELD = TextKey.of("jsc.assembly.queue_held",
            "A card's queue runs at most at the CPU's speed, and no faster than the card");
    static final TextKey ITEMS_PER_TICK = TextKey.of("jsc.assembly.items_per_tick", "it/t");
    static final TextKey ITEMS = TextKey.of("jsc.assembly.items", "it");
    static final TextKey NETWORK = TextKey.of("jsc.assembly.network", "NETWORK");
    static final TextKey LINKED = TextKey.of("jsc.assembly.linked", "LINKED");
    static final TextKey ONE_SERVER = TextKey.of("jsc.assembly.one_server", "%s server");
    static final TextKey SERVERS = TextKey.of("jsc.assembly.servers", "%s servers");
    static final TextKey CRAFT = TextKey.of("jsc.assembly.craft", "CRAFT");
    static final TextKey NO_CARD = TextKey.of("jsc.assembly.no_card", "NO CARD");
    static final TextKey RECIPE_ROM = TextKey.of("jsc.assembly.recipe_rom", "RECIPE ROM");
    static final TextKey OF = TextKey.of("jsc.assembly.of", "%s / %s");

    // The Mainframe's operations.
    static final TextKey QUEUED = TextKey.of("jsc.assembly.queued", "QUEUED");
    static final TextKey RUNNING = TextKey.of("jsc.assembly.running", "RUNNING");
    static final TextKey DONE = TextKey.of("jsc.assembly.done", "DONE");

    // The Server's assembly.
    static final TextKey ORCHESTRATION = TextKey.of("jsc.assembly.orchestration", "ORCHESTRATION");
    static final TextKey RAM_BUFFER_SHORT = TextKey.of("jsc.assembly.ram_buffer_short", "RAM BUF");
    static final TextKey DRAW = TextKey.of("jsc.assembly.draw", "DRAW");
    static final TextKey DRAW_OF = TextKey.of("jsc.assembly.draw_of", "%s/%sW");
    static final TextKey NO_WATTS = TextKey.of("jsc.assembly.no_watts", "-- W");
    static final TextKey POWER = TextKey.of("jsc.assembly.power", "POWER");
    static final TextKey STORAGE = TextKey.of("jsc.assembly.storage", "STORAGE");
    static final TextKey DRIVES_IN_BAYS = TextKey.of("jsc.assembly.drives_in_bays", "drives mount in the rack bays");
    static final TextKey INSERT_TO_BEGIN =
            TextKey.of("jsc.assembly.insert_to_begin", "Insert a motherboard and PSU to begin");
    static final TextKey ALL_CHECKS_PASSED = TextKey.of("jsc.assembly.all_checks_passed", "All checks passed");
    static final TextKey MORE = TextKey.of("jsc.assembly.more", "+%s more");

    // The case: which way it faces, what its top row seats, the board's bus.
    static final TextKey REAR = TextKey.of("jsc.assembly.rear", "REAR");
    static final TextKey FRONT = TextKey.of("jsc.assembly.front", "FRONT");
    static final TextKey DIMM = TextKey.of("jsc.assembly.dimm", "DIMM");
    static final TextKey BUS_ISA = TextKey.of("jsc.assembly.bus_isa", "ISA");
    static final TextKey BUS_PCI = TextKey.of("jsc.assembly.bus_pci", "PCI");

    // The front panel and the service panel.
    static final TextKey AUTO_START = TextKey.of("jsc.assembly.auto_start", "AUTO START");
    static final TextKey SIDE_PANEL = TextKey.of("jsc.assembly.side_panel", "SIDE PANEL");
    static final TextKey FAILOVER = TextKey.of("jsc.assembly.failover", "FAILOVER");
    static final TextKey OPERATIONS = TextKey.of("jsc.assembly.operations", "OPERATIONS");
    static final TextKey LAMP_POWER = TextKey.of("jsc.assembly.lamp_power", "PWR");
    static final TextKey LAMP_DISK = TextKey.of("jsc.assembly.lamp_disk", "DISK");
    static final TextKey LAMP_NETWORK = TextKey.of("jsc.assembly.lamp_network", "NET");
    static final TextKey NOT_A_COMPUTER = TextKey.of("jsc.assembly.not_a_computer", "Not a computer yet");
    static final TextKey NEEDS_PARTS = TextKey.of("jsc.assembly.needs_parts",
            "Needs a board, a CPU, RAM, a disk and a PSU");
    static final TextKey THEN_SWITCH_ON = TextKey.of("jsc.assembly.then_switch_on", "Then press to switch on");
    static final TextKey AUTO_KEEPS_ON = TextKey.of("jsc.assembly.auto_keeps_on",
            "Auto start keeps it on: switch auto start off to switch it off here");
    static final TextKey CHECKS = TextKey.of("jsc.assembly.checks", "CHECKS");
    static final TextKey IN_THE_RACK = TextKey.of("jsc.assembly.in_the_rack", "in the rack");
    /* The Crafting Card's two stats as one readout: how many stages it runs at once, and how much faster. */
    static final TextKey CRAFT_THREADS = TextKey.of("jsc.assembly.craft_threads", "%sT x%s");
    static final TextKey CRAFT_THROUGHPUT = TextKey.of("jsc.assembly.craft_throughput", "Crafts %s it/t");
    static final TextKey RAM_BUFFER_LINE = TextKey.of("jsc.assembly.ram_buffer_line", "RAM buffer: %s it");
    static final TextKey CLUSTERS_SHORT = TextKey.of("jsc.assembly.clusters_short", "CLUSTERS");
    static final TextKey IN_REACH = TextKey.of("jsc.assembly.in_reach", "%s in reach");
    static final TextKey NONE = TextKey.of("jsc.assembly.none", "none");
    static final TextKey INSTALLED = TextKey.of("jsc.assembly.installed", "installed");
    static final TextKey NOT_INSTALLED = TextKey.of("jsc.assembly.not_installed", "not installed");

    // The Cluster Management Computer's readouts.
    static final TextKey NO_INTERFACE_CARD = TextKey.of("jsc.assembly.no_interface_card", "no interface card");
    /* Supercomputers, datacenters and HPC lanes in reach. */
    static final TextKey CLUSTERS = TextKey.of("jsc.assembly.clusters", "%s SC · %s DC · %s lanes");
    static final TextKey MANAGEMENT = TextKey.of("jsc.assembly.management", "MANAGEMENT");

    private AssemblyTexts() {
    }
}
