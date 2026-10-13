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
 * What a network machine's space says about the things it holds: the Local, Storage and Network headings, and the
 * question that takes a thing out. Kept apart from the screens so the language generator can read it on a server
 * too, where screens do not exist.
 */
@TextHolder
final class TerminalGridTexts {

    // The toolbar over a grid.
    static final TextKey ONE_TYPE = TextKey.of("jsc.terminal.grid.one_type", "%s type");
    static final TextKey TYPES = TextKey.of("jsc.terminal.grid.types", "%s types");
    static final TextKey ONE_KIND = TextKey.of("jsc.terminal.grid.one_kind", "%s kind");
    static final TextKey KINDS = TextKey.of("jsc.terminal.grid.kinds", "%s kinds");
    /* The sort button, and the field a quantity is typed into. */
    static final TextKey QUANTITY = TextKey.of("jsc.terminal.grid.quantity", "Qty");
    static final TextKey NAME = TextKey.of("jsc.terminal.grid.name", "Name");
    static final TextKey MOD = TextKey.of("jsc.terminal.grid.mod", "Mod");
    static final TextKey STORE_ALL = TextKey.of("jsc.terminal.grid.store_all", "Store all");
    static final TextKey DEPOSIT_ALL = TextKey.of("jsc.terminal.grid.deposit_all", "Deposit all");

    // The Storage heading's panel.
    static final TextKey THIS_MACHINES_DISKS = TextKey.of("jsc.terminal.storage.disks", "This machine's disks");
    static final TextKey ALL_OFFERED = TextKey.of("jsc.terminal.storage.all_offered",
            "Every byte of it is the network's, and none of it is kept back.");
    static final TextKey NO_DISK = TextKey.of("jsc.terminal.storage.no_disk", "no disk");
    /* A disk by its letter. */
    static final TextKey DISK_LETTER = TextKey.of("jsc.terminal.storage.disk_letter", "Disk %s");
    static final TextKey EMPTY = TextKey.of("jsc.terminal.storage.empty", "empty");
    static final TextKey OFFERED = TextKey.of("jsc.terminal.storage.offered", "%s%% offered");
    static final TextKey DRAG_HINT = TextKey.of("jsc.terminal.storage.drag_hint",
            "Drag to say how much of a disk the network may have. Hold Shift for fine steps.");

    // The Network heading's panel.
    static final TextKey NOTHING_PICKED = TextKey.of("jsc.terminal.network.nothing_picked", "Nothing picked out.");
    static final TextKey PICK_HINT = TextKey.of("jsc.terminal.network.pick_hint",
            "Click a thing in the grid to see where it is kept.");
    static final TextKey HELD_BY = TextKey.of("jsc.terminal.network.held_by", "Held by");
    static final TextKey ASKING = TextKey.of("jsc.terminal.network.asking", "asking the network ...");
    static final TextKey MADE_FROM = TextKey.of("jsc.terminal.network.made_from", "Made from");
    static final TextKey NO_PATTERN = TextKey.of("jsc.terminal.network.no_pattern", "no pattern for it");
    static final TextKey HAS_PATTERN = TextKey.of("jsc.terminal.network.has_pattern", "a pattern this network holds");
    static final TextKey GET = TextKey.of("jsc.terminal.network.get", "Get");
    static final TextKey CRAFT = TextKey.of("jsc.terminal.network.craft", "Craft");

    // The Local heading.
    static final TextKey THIS_COMPUTER = TextKey.of("jsc.terminal.local.this_computer", "THIS COMPUTER");
    static final TextKey HARDWARE = TextKey.of("jsc.terminal.local.hardware", "HARDWARE");
    static final TextKey DISK = TextKey.of("jsc.terminal.local.disk", "Disk");
    static final TextKey STORAGE = TextKey.of("jsc.terminal.local.storage", "Storage");
    static final TextKey THE_NETWORK = TextKey.of("jsc.terminal.local.the_network", "THE NETWORK");
    static final TextKey TWO_ORCHESTRATORS = TextKey.of("jsc.terminal.local.two_orchestrators", "two orchestrators");
    static final TextKey JOINED = TextKey.of("jsc.terminal.local.joined", "joined");
    static final TextKey NOT_ON_ONE = TextKey.of("jsc.terminal.local.not_on_one", "not on one");
    static final TextKey CABLE_HINT = TextKey.of("jsc.terminal.local.cable_hint",
            "A data cable to a Mainframe is what puts it on one.");
    static final TextKey SERVERS = TextKey.of("jsc.terminal.local.servers", "Servers");
    static final TextKey COMPUTERS = TextKey.of("jsc.terminal.local.computers", "Computers");
    static final TextKey SUBFRAMES = TextKey.of("jsc.terminal.local.subframes", "Subframes");
    static final TextKey HELD = TextKey.of("jsc.terminal.local.held", "Held");
    static final TextKey ROOM = TextKey.of("jsc.terminal.local.room", "Room");
    static final TextKey IN_FLIGHT = TextKey.of("jsc.terminal.local.in_flight", "In flight");

    // The question that takes a thing out.
    static final TextKey IN_LOCAL = TextKey.of("jsc.terminal.request.in_local", "%s in local");
    static final TextKey AVAILABLE = TextKey.of("jsc.terminal.request.available", "%s available");
    /* The toggle that opens the question out, short enough for its small button. */
    static final TextKey ADVANCED = TextKey.of("jsc.terminal.request.advanced", "ADV");
    static final TextKey MAX = TextKey.of("jsc.terminal.request.max", "MAX");
    static final TextKey LANDS_HERE = TextKey.of("jsc.terminal.request.lands_here", "Lands in this computer's storage");
    static final TextKey NEEDS_STORAGE =
            TextKey.of("jsc.terminal.request.needs_storage", "Needs internal storage (no disk)");
    static final TextKey REQUEST = TextKey.of("jsc.terminal.request.request", "REQUEST %s");
    static final TextKey SEND_LOCAL_TO = TextKey.of("jsc.terminal.request.send_local_to", "Send local items to:");
    static final TextKey TO_INVENTORY = TextKey.of("jsc.terminal.request.to_inventory", "TO INVENTORY");
    static final TextKey TO_NETWORK = TextKey.of("jsc.terminal.request.to_network", "TO NETWORK");
    static final TextKey PULL_FROM = TextKey.of("jsc.terminal.request.pull_from", "PULL FROM");
    static final TextKey ALL_SERVERS = TextKey.of("jsc.terminal.request.all_servers", "all servers");
    static final TextKey MORE_INCLUDED = TextKey.of("jsc.terminal.request.more_included", "+%s more (included)");
    static final TextKey SEND_TO = TextKey.of("jsc.terminal.request.send_to", "SEND TO");
    static final TextKey NO_COMPUTERS = TextKey.of("jsc.terminal.request.no_computers", "No computers available");
    /* A computer to send to, and how much room it has. */
    static final TextKey WITH_FREE = TextKey.of("jsc.terminal.request.with_free", "%s  (%s free)");
    static final TextKey SEND = TextKey.of("jsc.terminal.request.send", "SEND %s");

    private TerminalGridTexts() {
    }
}
