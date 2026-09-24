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
    static final TextKey BOARD = TextKey.of("jsc.assembly.board", "BOARD");
    static final TextKey CPU = TextKey.of("jsc.assembly.cpu", "CPU");
    static final TextKey PSU = TextKey.of("jsc.assembly.psu", "PSU");
    static final TextKey RAM = TextKey.of("jsc.assembly.ram", "RAM");
    static final TextKey DISK = TextKey.of("jsc.assembly.disk", "DISK");
    static final TextKey GPU = TextKey.of("jsc.assembly.gpu", "GPU");
    static final TextKey PCIE = TextKey.of("jsc.assembly.pcie", "PCIE");

    // The tiles.
    static final TextKey CAPACITY = TextKey.of("jsc.assembly.capacity", "CAPACITY");
    static final TextKey RAM_BUFFER = TextKey.of("jsc.assembly.ram_buffer", "RAM BUFFER");
    static final TextKey QUEUES = TextKey.of("jsc.assembly.queues", "QUEUES");
    static final TextKey ITEMS_PER_TICK = TextKey.of("jsc.assembly.items_per_tick", "it/t");
    static final TextKey ITEMS = TextKey.of("jsc.assembly.items", "it");
    static final TextKey NETWORK = TextKey.of("jsc.assembly.network", "NETWORK");
    static final TextKey LINKED = TextKey.of("jsc.assembly.linked", "LINKED");
    static final TextKey ONE_SERVER = TextKey.of("jsc.assembly.one_server", "%s server");
    static final TextKey SERVERS = TextKey.of("jsc.assembly.servers", "%s servers");
    static final TextKey CRAFT = TextKey.of("jsc.assembly.craft", "CRAFT");
    /* The Crafting Card's two stats: how many stages it runs at once, and how much faster. */
    static final TextKey CRAFT_CARD = TextKey.of("jsc.assembly.craft_card", "CRAFT %sT x%s");
    static final TextKey NO_CARD = TextKey.of("jsc.assembly.no_card", "NO CARD");
    static final TextKey RECIPE_ROM = TextKey.of("jsc.assembly.recipe_rom", "RECIPE ROM");
    static final TextKey OF = TextKey.of("jsc.assembly.of", "%s / %s");

    // The power buttons.
    static final TextKey AUTO = TextKey.of("jsc.assembly.auto", "AUTO");
    static final TextKey TURN_OFF = TextKey.of("jsc.assembly.turn_off", "TURN OFF");
    static final TextKey TURN_ON = TextKey.of("jsc.assembly.turn_on", "TURN ON");
    static final TextKey AUTO_ON = TextKey.of("jsc.assembly.auto_on", "AUTO: ON");
    static final TextKey AUTO_OFF = TextKey.of("jsc.assembly.auto_off", "AUTO: OFF");

    private AssemblyTexts() {
    }
}
