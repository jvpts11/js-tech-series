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
 * What a server rack's screen says, kept apart from the screen so the language generator can read it on a server
 * too, where screens do not exist.
 */
@TextHolder
final class ServerRackTexts {

    /* The rack units in use out of the cabinet's, then its state. */
    static final TextKey THROTTLED = TextKey.of("jsc.rack.throttled", "%s/%sU  THROTTLED %s%%");
    static final TextKey LINKED = TextKey.of("jsc.rack.linked", "%s/%sU  LINKED");
    static final TextKey OFFLINE = TextKey.of("jsc.rack.offline", "%s/%sU  OFFLINE");
    static final TextKey UNIT = TextKey.of("jsc.rack.unit", "%sU");
    /* A row taken by the chassis mounted above it. */
    static final TextKey COVERED = TextKey.of("jsc.rack.covered", "^ %sU");
    static final TextKey INCOMPLETE = TextKey.of("jsc.rack.incomplete", "INCOMPLETE");
    static final TextKey OFF = TextKey.of("jsc.rack.off", "OFF");
    static final TextKey ONLINE = TextKey.of("jsc.rack.online", "ONLINE");
    static final TextKey READY = TextKey.of("jsc.rack.ready", "READY");
    static final TextKey REBUILD = TextKey.of("jsc.rack.rebuild", "REBUILD %s%%");
    static final TextKey POWER = TextKey.of("jsc.rack.power", "PWR");

    // The tooltips.
    static final TextKey BELONGS_IN_SERVER_RACK =
            TextKey.of("jsc.rack.belongs_in_server_rack", "This chassis belongs in a Server Rack");
    static final TextKey BELONGS_IN_SUPERCOMPUTER_RACK =
            TextKey.of("jsc.rack.belongs_in_supercomputer_rack", "This chassis belongs in a Supercomputer Rack");
    static final TextKey BELONGS_IN_AI_RACK = TextKey.of("jsc.rack.belongs_in_ai_rack", "This chassis belongs in an AI Rack");
    static final TextKey ARRAY_MEMBER = TextKey.of("jsc.rack.array_member", "Array member slot - %s");
    static final TextKey GADGET_CONFIGURED = TextKey.of("jsc.rack.gadget_configured",
            "Gadget bay - a RAID Controller is configured in the machine's firmware (STORAGE)");
    static final TextKey DRIVE_BAY = TextKey.of("jsc.rack.drive_bay", "Drive bay - hotswap a disk here");
    static final TextKey GADGET_BAY = TextKey.of("jsc.rack.gadget_bay", "Gadget bay - RAID controller or cache card");
    static final TextKey BLOCKED_NO_UNIT =
            TextKey.of("jsc.rack.blocked_no_unit", "Blocked - no unit in this row cables these slots");
    static final TextKey BLOCKED_BUDGET =
            TextKey.of("jsc.rack.blocked_budget", "Blocked - the chassis does not cable this slot");
    static final TextKey NODE = TextKey.of("jsc.rack.node", "Node %s");
    static final TextKey UNASSIGNED_NODE = TextKey.of("jsc.rack.unassigned_node", "Unassigned node");
    static final TextKey CHASSIS = TextKey.of("jsc.rack.chassis", "%sU - %s drive + %s gadget bays");
    static final TextKey NEEDS_BOARD = TextKey.of("jsc.rack.needs_board", "Incomplete - needs a board + PSU");
    static final TextKey NOT_ON_NETWORK = TextKey.of("jsc.rack.not_on_network", "Rack cable not on a network");

    private ServerRackTexts() {
    }
}
