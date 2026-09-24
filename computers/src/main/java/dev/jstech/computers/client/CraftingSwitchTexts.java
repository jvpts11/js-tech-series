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
 * What the Crafting Switch's screen says, kept apart from the screen so the language generator can read it on a
 * server too, where screens do not exist.
 */
@TextHolder
final class CraftingSwitchTexts {

    static final TextKey TITLE = TextKey.of("jsc.crafting_switch.title", "CRAFTING SWITCH");
    static final TextKey LINKED = TextKey.of("jsc.crafting_switch.linked", "LINKED");
    static final TextKey UNLINKED = TextKey.of("jsc.crafting_switch.unlinked", "UNLINKED");
    static final TextKey ACTIVE = TextKey.of("jsc.crafting_switch.active", "Active");
    static final TextKey ACCEPTING = TextKey.of("jsc.crafting_switch.accepting", "Active: accepting");
    static final TextKey INACTIVE = TextKey.of("jsc.crafting_switch.inactive", "Inactive");
    static final TextKey CATEGORY = TextKey.of("jsc.crafting_switch.category", "Category");
    static final TextKey NO_CATEGORY = TextKey.of("jsc.crafting_switch.no_category", "Category: none");
    static final TextKey SHORT_CATEGORY = TextKey.of("jsc.crafting_switch.short_category", "Cat: %s");
    static final TextKey NAME = TextKey.of("jsc.crafting_switch.name", "NAME");
    static final TextKey AT = TextKey.of("jsc.crafting_switch.at", "AT %s, %s, %s");
    static final TextKey MORE = TextKey.of("jsc.crafting_switch.more", "+%s more");
    static final TextKey MACHINE_CATEGORY = TextKey.of("jsc.crafting_switch.machine_category", "MACHINE CATEGORY");
    static final TextKey NONE = TextKey.of("jsc.crafting_switch.none", "none");
    /* A face row: the face, then what hangs off it. */
    static final TextKey TO_COMPUTER = TextKey.of("jsc.crafting_switch.to_computer", "%s → computer");
    static final TextKey FACE_MACHINE = TextKey.of("jsc.crafting_switch.face_machine", "%s  %s");
    static final TextKey MACHINE = TextKey.of("jsc.crafting_switch.machine", "machine");
    static final TextKey FACE_NONE = TextKey.of("jsc.crafting_switch.face_none", "%s  none");
    static final TextKey VIA_BUS = TextKey.of("jsc.crafting_switch.via_bus", "%s > %s");
    static final TextKey VIA_BUS_MORE = TextKey.of("jsc.crafting_switch.via_bus_more", "%s > %s +%s");
    /* The detail panel's heading for a face. */
    static final TextKey CRAFTING_CABLE = TextKey.of("jsc.crafting_switch.crafting_cable", "%s · crafting cable");
    static final TextKey HAS_MACHINE = TextKey.of("jsc.crafting_switch.has_machine", "%s · machine");
    static final TextKey NO_MACHINE = TextKey.of("jsc.crafting_switch.no_machine", "%s · no machine");
    static final TextKey DOWN = TextKey.of("jsc.crafting_switch.down", "DOWN");
    static final TextKey UP = TextKey.of("jsc.crafting_switch.up", "UP");
    static final TextKey NORTH = TextKey.of("jsc.crafting_switch.north", "NORTH");
    static final TextKey SOUTH = TextKey.of("jsc.crafting_switch.south", "SOUTH");
    static final TextKey WEST = TextKey.of("jsc.crafting_switch.west", "WEST");
    static final TextKey EAST = TextKey.of("jsc.crafting_switch.east", "EAST");

    private CraftingSwitchTexts() {
    }
}
