/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a craft plan says about what it is short of, kept apart from the handlers that hand plans to windows: the
 * language generator loads every class that declares sentences, on a server as well, where windows do not exist.
 */
@TextHolder
final class CraftTexts {

    static final TextKey CRAFTED_BEFORE_STAGES = TextKey.of("jsc.craft.cover.crafted_before_stages",
            "Missing %s %s · will be crafted from %s (%s pattern) before the stages start");
    static final TextKey CRAFT_IT_FIRST = TextKey.of("jsc.craft.cover.craft_it_first",
            "Missing %s %s · a pipeline runs on stock, craft it first (%s pattern)");
    static final TextKey NOTHING_MAKES_IT = TextKey.of("jsc.craft.cover.nothing_makes_it",
            "Missing %s %s · nothing on the network makes it");
    /* What a plan crafts from when it consumes nothing named: what is already in stock. */
    static final TextKey STOCK = TextKey.of("jsc.craft.cover.stock", "stock");
    /* How much of a thing, then the thing: "4 Logs". */
    static final TextKey AMOUNT_OF = TextKey.of("jsc.craft.cover.amount_of", "%s %s");

    // The Crafting Manager.
    static final TextKey LOADED_ROM_FULL =
            TextKey.of("jsc.craft_manager.loaded_rom_full", "Loaded %s of %s - ROM full (%s/%s)");
    static final TextKey LOADED_ONE = TextKey.of("jsc.craft_manager.loaded_one", "Loaded %s craft");
    static final TextKey LOADED_MANY = TextKey.of("jsc.craft_manager.loaded_many", "Loaded %s crafts");
    static final TextKey ALREADY_LOADED = TextKey.of("jsc.craft_manager.already_loaded", "Already loaded");
    static final TextKey NOTHING_TO_LOAD = TextKey.of("jsc.craft_manager.nothing_to_load", "Nothing to load");
    static final TextKey REMOVABLE_DRIVE = TextKey.of("jsc.craft_manager.removable_drive", "Removable Drive");
    static final TextKey ROM_MULTI = TextKey.of("jsc.craft_manager.rom_multi", "%s [multi]");
    static final TextKey ROM_MACHINE = TextKey.of("jsc.craft_manager.rom_machine", "%s [machine]");
    /* Where an unnamed machine stands: the switch face it is on, by its initial, and its coordinates. */
    static final TextKey MACHINE_AT_FACE = TextKey.of("jsc.craft_manager.machine_at_face", "%s (%s, %s, %s)");
    static final TextKey MACHINE_AT = TextKey.of("jsc.craft_manager.machine_at", "(%s, %s, %s)");
    static final TextKey FACE_DOWN = TextKey.of("jsc.craft_manager.face_down", "D");
    static final TextKey FACE_UP = TextKey.of("jsc.craft_manager.face_up", "U");
    static final TextKey FACE_NORTH = TextKey.of("jsc.craft_manager.face_north", "N");
    static final TextKey FACE_SOUTH = TextKey.of("jsc.craft_manager.face_south", "S");
    static final TextKey FACE_WEST = TextKey.of("jsc.craft_manager.face_west", "W");
    static final TextKey FACE_EAST = TextKey.of("jsc.craft_manager.face_east", "E");

    private CraftTexts() {
    }
}
