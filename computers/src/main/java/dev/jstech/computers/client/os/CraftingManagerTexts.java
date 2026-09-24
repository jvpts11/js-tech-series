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
    static final TextKey MACHINES_TAB = TextKey.of("jsc.crafting_manager.machines_tab", "Machines");
    static final TextKey CARD_REQUIRED_TO_MANAGE =
            TextKey.of("jsc.crafting_manager.card_required_to_manage", "A Crafting Card is required to manage recipes.");
    static final TextKey NO_REMOVABLE_MEDIA = TextKey.of("jsc.crafting_manager.no_removable_media", "Removable media: none");
    static final TextKey MEDIA = TextKey.of("jsc.crafting_manager.media", "Media: %s");
    static final TextKey THIS_COMPUTER = TextKey.of("jsc.crafting_manager.this_computer", "This computer  (%s/%s)");
    static final TextKey INSERT_A_DISC =
            TextKey.of("jsc.crafting_manager.insert_a_disc", "Insert a disc into a linked drive");
    static final TextKey NO_CRAFT_FILES = TextKey.of("jsc.crafting_manager.no_craft_files", "No .craft files");
    static final TextKey NO_RECIPES = TextKey.of("jsc.crafting_manager.no_recipes", "No recipes loaded");
    static final TextKey LOAD = TextKey.of("jsc.crafting_manager.load", "Load →");
    static final TextKey LOAD_ALL = TextKey.of("jsc.crafting_manager.load_all", "Load all");
    static final TextKey DOWNLOAD = TextKey.of("jsc.crafting_manager.download", "← Download");
    static final TextKey REMOVE = TextKey.of("jsc.crafting_manager.remove", "Remove");
    static final TextKey CARD_REQUIRED = TextKey.of("jsc.crafting_manager.card_required", "A Crafting Card is required.");
    static final TextKey NO_MACHINES =
            TextKey.of("jsc.crafting_manager.no_machines", "No machines on the crafting network.");
    static final TextKey MACHINE_COLUMN = TextKey.of("jsc.crafting_manager.machine_column", "MACHINE");
    static final TextKey STATE_COLUMN = TextKey.of("jsc.crafting_manager.state_column", "STATE");
    static final TextKey FEED_COLUMN = TextKey.of("jsc.crafting_manager.feed_column", "FEED");
    static final TextKey ONE_MACHINE = TextKey.of("jsc.crafting_manager.one_machine", "%s  ·  %s machine");
    static final TextKey MACHINES = TextKey.of("jsc.crafting_manager.machines", "%s  ·  %s machines");
    static final TextKey JOBS = TextKey.of("jsc.crafting_manager.jobs", "Jobs %s");
    static final TextKey JOBS_AUTO = TextKey.of("jsc.crafting_manager.jobs_auto", "Jobs Auto");
    static final TextKey PAUSED = TextKey.of("jsc.crafting_manager.paused", "Paused");
    static final TextKey RUNNING = TextKey.of("jsc.crafting_manager.running", "Running");
    static final TextKey FILL = TextKey.of("jsc.crafting_manager.fill", "Fill");
    static final TextKey ONE_LOT = TextKey.of("jsc.crafting_manager.one_lot", "1 lot");

    private CraftingManagerTexts() {
    }
}
