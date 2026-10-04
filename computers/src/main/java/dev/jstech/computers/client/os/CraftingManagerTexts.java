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
 * What the Crafting Manager's window says, kept apart from the window so the language generator can read it on a
 * server too, where windows do not exist.
 */
@TextHolder
final class CraftingManagerTexts {

    static final TextKey RECIPES_TAB = TextKey.of("jsc.crafting_manager.recipes_tab", "Recipes");
    static final TextKey INTERFACES_TAB = TextKey.of("jsc.crafting_manager.interfaces_tab", "Interfaces");
    static final TextKey CARD_REQUIRED_TO_MANAGE = TextKey.of("jsc.crafting_manager.card_required_to_manage",
            "A Crafting Card is required to manage recipes.");
    static final TextKey NO_REMOVABLE_MEDIA =
            TextKey.of("jsc.crafting_manager.no_removable_media", "Removable media: none");
    static final TextKey MEDIA = TextKey.of("jsc.crafting_manager.media", "Media: %s");
    static final TextKey THIS_COMPUTER = TextKey.of("jsc.crafting_manager.this_computer", "This computer");
    static final TextKey INSERT_A_DISC =
            TextKey.of("jsc.crafting_manager.insert_a_disc", "Insert a disc into a linked drive");
    static final TextKey NO_CRAFT_FILES = TextKey.of("jsc.crafting_manager.no_craft_files", "No .craft files");
    static final TextKey NO_RECIPES = TextKey.of("jsc.crafting_manager.no_recipes", "No cards and no interfaces");
    static final TextKey LOAD = TextKey.of("jsc.crafting_manager.load", "Load →");
    static final TextKey LOAD_ALL = TextKey.of("jsc.crafting_manager.load_all", "Load all");
    static final TextKey DOWNLOAD = TextKey.of("jsc.crafting_manager.download", "← Download");
    static final TextKey REMOVE = TextKey.of("jsc.crafting_manager.remove", "Remove");
    /* Where Load puts what it loads: the place chosen, or the first with room. */
    static final TextKey INTO = TextKey.of("jsc.crafting_manager.into", "into %s ▾");
    static final TextKey FIRST_WITH_ROOM = TextKey.of("jsc.crafting_manager.first_with_room", "the first with room");
    static final TextKey MOVE_TO = TextKey.of("jsc.crafting_manager.move_to", "Move to ▾");
    static final TextKey PICK_PLACE = TextKey.of("jsc.crafting_manager.pick_place", "Load into");
    static final TextKey PICK_MOVE = TextKey.of("jsc.crafting_manager.pick_move", "Move to");
    /* A place's header: its title, then how full it is. */
    static final TextKey PLACE_COUNT = TextKey.of("jsc.crafting_manager.place_count", "%s of %s");
    static final TextKey BENCH_RECIPES = TextKey.of("jsc.crafting_manager.bench_recipes", "%s · bench recipes");
    static final TextKey KIND_NOTE = TextKey.of("jsc.crafting_manager.kind_note",
            "Bench recipes go into a card's ROM; machine and pipeline recipes into an interface.");
    static final TextKey CARD_REQUIRED =
            TextKey.of("jsc.crafting_manager.card_required", "A Crafting Card is required.");
    static final TextKey NO_INTERFACES = TextKey.of("jsc.crafting_manager.no_interfaces",
            "No Crafting Interface on this computer's crafting cable.");
    static final TextKey SUMMARY = TextKey.of("jsc.crafting_manager.summary",
            "Interfaces %s of %s · %s Crafting Cards");
    static final TextKey SUMMARY_ONE = TextKey.of("jsc.crafting_manager.summary_one",
            "Interfaces %s of %s · 1 Crafting Card");
    static final TextKey INTERFACE_COLUMN = TextKey.of("jsc.crafting_manager.interface_column", "INTERFACE");
    static final TextKey MACHINE_COLUMN = TextKey.of("jsc.crafting_manager.machine_column", "MACHINE");
    static final TextKey PATTERNS_COLUMN = TextKey.of("jsc.crafting_manager.patterns_column", "PATTERNS");
    static final TextKey MODE_COLUMN = TextKey.of("jsc.crafting_manager.mode_column", "MODE");
    static final TextKey STATE_COLUMN = TextKey.of("jsc.crafting_manager.state_column", "STATE");
    static final TextKey JOBS_COLUMN = TextKey.of("jsc.crafting_manager.jobs_column", "JOBS");
    static final TextKey PATTERNS = TextKey.of("jsc.crafting_manager.patterns", "%s / %s");
    static final TextKey EXCLUSIVE = TextKey.of("jsc.crafting_manager.exclusive", "Exclusive");
    static final TextKey NOT_EXCLUSIVE = TextKey.of("jsc.crafting_manager.not_exclusive", "Not exclusive");
    static final TextKey IDLE = TextKey.of("jsc.crafting_manager.idle", "Idle");
    static final TextKey PAUSED = TextKey.of("jsc.crafting_manager.paused", "Paused");
    static final TextKey NO_MACHINE = TextKey.of("jsc.crafting_manager.no_machine", "Touches no machine");
    static final TextKey JOBS_AUTO = TextKey.of("jsc.crafting_manager.jobs_auto", "Auto");
    static final TextKey PAUSE = TextKey.of("jsc.crafting_manager.pause", "Pause");
    static final TextKey RESUME = TextKey.of("jsc.crafting_manager.resume", "Resume");
    static final TextKey CARDS = TextKey.of("jsc.crafting_manager.cards", "Crafting Cards");
    static final TextKey CARD_LINE = TextKey.of("jsc.crafting_manager.card_line",
            "%s · drives %s interfaces · ROM %s of %s");
    static final TextKey WAITING = TextKey.of("jsc.crafting_manager.waiting",
            "%s more on the crafting cable wait, driven by none, until another card goes in.");
    static final TextKey HINT = TextKey.of("jsc.crafting_manager.hint",
            "Click the mode to switch it, the jobs to raise them (right-click lowers).");

    private CraftingManagerTexts() {
    }
}
