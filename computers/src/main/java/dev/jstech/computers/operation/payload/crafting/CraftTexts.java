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
            TextKey.of("jsc.craft_manager.loaded_rom_full", "Loaded %s of %s - no room left where they go");
    static final TextKey LOADED_ONE = TextKey.of("jsc.craft_manager.loaded_one", "Loaded %s craft");
    static final TextKey LOADED_MANY = TextKey.of("jsc.craft_manager.loaded_many", "Loaded %s crafts");
    static final TextKey ALREADY_LOADED = TextKey.of("jsc.craft_manager.already_loaded", "Already loaded");
    static final TextKey NOTHING_TO_LOAD = TextKey.of("jsc.craft_manager.nothing_to_load", "Nothing to load");
    static final TextKey REMOVABLE_DRIVE = TextKey.of("jsc.craft_manager.removable_drive", "Removable Drive");
    /* A card's place: its name and the slot it is in. */
    static final TextKey CARD_PLACE = TextKey.of("jsc.craft_manager.card_place", "%s (slot %s)");
    /* An interface nobody named, by its kind and where it is. */
    static final TextKey INTERFACE_AT = TextKey.of("jsc.craft_manager.interface_at", "%s (%s, %s, %s)");
    static final TextKey MOVED = TextKey.of("jsc.craft_manager.moved", "%s moved to %s");
    static final TextKey NOT_MOVED = TextKey.of("jsc.craft_manager.not_moved",
            "Not moved: %s has no room for it, holds it already, or takes another kind of recipe");
    static final TextKey RUNNING_ONE = TextKey.of("jsc.craft_manager.running_one", "Running · %s");
    static final TextKey RUNNING_MANY = TextKey.of("jsc.craft_manager.running_many", "Running · %s jobs");
    static final TextKey DRAINING = TextKey.of("jsc.craft_manager.draining", "Draining");
    /* A job: what it makes and how many. */
    static final TextKey JOB = TextKey.of("jsc.craft_manager.job", "%s x%s");

    private CraftTexts() {
    }
}
