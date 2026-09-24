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
 * What the Pattern Studio's window says, kept apart from the window so the language generator can read it on a
 * server too, where windows do not exist.
 */
@TextHolder
final class PatternStudioScreenTexts {

    // The tabs and the drafts.
    static final TextKey BENCH_TAB = TextKey.of("jsc.pattern_studio.screen.bench_tab", "Bench");
    static final TextKey MACHINE_TAB = TextKey.of("jsc.pattern_studio.screen.machine_tab", "Machine");
    static final TextKey MULTI_STAGE_TAB = TextKey.of("jsc.pattern_studio.screen.multi_stage_tab", "Multi-stage");
    static final TextKey IN_RECIPE_ROM = TextKey.of("jsc.pattern_studio.screen.in_recipe_rom", "In the Recipe ROM");
    static final TextKey IN_THE_ROM = TextKey.of("jsc.pattern_studio.screen.in_the_rom", "In the ROM");
    static final TextKey IN_ROM = TextKey.of("jsc.pattern_studio.screen.in_rom", "In ROM");
    static final TextKey CLEAR = TextKey.of("jsc.pattern_studio.screen.clear", "Clear");
    static final TextKey TIMEOUT = TextKey.of("jsc.pattern_studio.screen.timeout", "Timeout");
    static final TextKey NO_STAGES = TextKey.of("jsc.pattern_studio.screen.no_stages", "No stages yet");
    static final TextKey ADD_BENCH = TextKey.of("jsc.pattern_studio.screen.add_bench", "+ Bench");
    static final TextKey ADD_MACHINE = TextKey.of("jsc.pattern_studio.screen.add_machine", "+ Machine");
    static final TextKey REMOVE = TextKey.of("jsc.pattern_studio.screen.remove", "Remove");
    static final TextKey NAME = TextKey.of("jsc.pattern_studio.screen.name", "Name");
    static final TextKey NOTE = TextKey.of("jsc.pattern_studio.screen.note", "Note");
    static final TextKey LOADING = TextKey.of("jsc.pattern_studio.screen.loading", "Loading...");
    static final TextKey INVENTORY = TextKey.of("jsc.pattern_studio.screen.inventory", "Inventory");
    static final TextKey NO_RECIPE = TextKey.of("jsc.pattern_studio.screen.no_recipe", "No recipe");
    /* How many a recipe makes, and of what: "4 x Oak Planks". */
    static final TextKey RESULT = TextKey.of("jsc.pattern_studio.screen.result", "%s x %s");
    static final TextKey FILE = TextKey.of("jsc.pattern_studio.screen.file", "File: %s");
    static final TextKey PICK_MACHINE_BUTTON =
            TextKey.of("jsc.pattern_studio.screen.pick_machine_button", "Machine...");
    static final TextKey STAGE_BENCH = TextKey.of("jsc.pattern_studio.screen.stage_bench", "%s. [bench] %s");
    static final TextKey STAGE_MACHINE = TextKey.of("jsc.pattern_studio.screen.stage_machine", "%s. [machine] %s");

    // What the bar under the editor hints at.
    static final TextKey HINT_BENCH =
            TextKey.of("jsc.pattern_studio.screen.hint_bench", "Right-click a cell: the tag it accepts");
    static final TextKey HINT_MACHINE =
            TextKey.of("jsc.pattern_studio.screen.hint_machine", "Right-click: chance. Shift-click: amount");
    static final TextKey HINT_PIPELINE =
            TextKey.of("jsc.pattern_studio.screen.hint_pipeline", "Add a draft or a file as a stage");

    // The rail.
    static final TextKey FILES_TAB = TextKey.of("jsc.pattern_studio.screen.files_tab", "Files");
    static final TextKey ENCODER_TAB = TextKey.of("jsc.pattern_studio.screen.encoder_tab", "Encoder");
    static final TextKey NO_DRIVES = TextKey.of("jsc.pattern_studio.screen.no_drives", "No drives");
    static final TextKey CLICK_ADDS_STAGE =
            TextKey.of("jsc.pattern_studio.screen.click_adds_stage", "Click adds a stage");
    static final TextKey CLICK_OPENS_FILE =
            TextKey.of("jsc.pattern_studio.screen.click_opens_file", "Click opens the file");
    static final TextKey ENCODER_OF_ERA = TextKey.of("jsc.pattern_studio.screen.encoder_of_era", "%s encoder");
    static final TextKey NO_ENCODER = TextKey.of("jsc.pattern_studio.screen.no_encoder", "No encoder linked");
    /* Two lines that read as one sentence under "No encoder linked". */
    static final TextKey RUN_CABLE = TextKey.of("jsc.pattern_studio.screen.run_cable", "Run a peripheral cable");
    static final TextKey TO_ENCODER = TextKey.of("jsc.pattern_studio.screen.to_encoder", "to a Pattern Encoder.");
    static final TextKey BAY_EMPTY = TextKey.of("jsc.pattern_studio.screen.bay_empty", "Bay: empty");
    static final TextKey BAY = TextKey.of("jsc.pattern_studio.screen.bay", "Bay: %s");
    static final TextKey QUEUED = TextKey.of("jsc.pattern_studio.screen.queued", "Queued: %s");
    static final TextKey CANCEL = TextKey.of("jsc.pattern_studio.screen.cancel", "Cancel");
    static final TextKey EJECT = TextKey.of("jsc.pattern_studio.screen.eject", "Eject");

    // The action bar.
    static final TextKey BURN = TextKey.of("jsc.pattern_studio.screen.burn", "Burn");
    static final TextKey SAVE_TO_DISK = TextKey.of("jsc.pattern_studio.screen.save_to_disk", "Save to disk");
    static final TextKey LOAD_ROM = TextKey.of("jsc.pattern_studio.screen.load_rom", "Load ROM");

    // The popups.
    static final TextKey PICK_MACHINE = TextKey.of("jsc.pattern_studio.screen.pick_machine", "Pick a machine");
    static final TextKey CLOSE = TextKey.of("jsc.pattern_studio.screen.close", "Close");
    static final TextKey DONE = TextKey.of("jsc.pattern_studio.screen.done", "Done");
    /* A recipe category as a machine choice: any machine that runs it. */
    static final TextKey ANY_CATEGORY = TextKey.of("jsc.pattern_studio.screen.any_category", "Any %s");
    static final TextKey OUTPUT_AMOUNT =
            TextKey.of("jsc.pattern_studio.screen.output_amount", "Output amount per run");
    static final TextKey INPUT_AMOUNT = TextKey.of("jsc.pattern_studio.screen.input_amount", "Input amount per run");

    // A cell's tooltip.
    static final TextKey ANY_TAG = TextKey.of("jsc.pattern_studio.screen.any_tag", "Any #%s");
    static final TextKey NETWORK_WOULD_USE =
            TextKey.of("jsc.pattern_studio.screen.network_would_use", "Network would use: %s");
    static final TextKey IN_STOCK = TextKey.of("jsc.pattern_studio.screen.in_stock", "In stock: %s");
    static final TextKey PER_RUN = TextKey.of("jsc.pattern_studio.screen.per_run", "%s per run");
    static final TextKey PER_RUN_ESTIMATED =
            TextKey.of("jsc.pattern_studio.screen.per_run_estimated", "%s per run (estimated)");
    static final TextKey CHANCE = TextKey.of("jsc.pattern_studio.screen.chance", "Chance: %s");
    static final TextKey ITEMS = TextKey.of("jsc.pattern_studio.screen.items", "%s items");
    static final TextKey MILLIBUCKETS = TextKey.of("jsc.pattern_studio.screen.millibuckets", "%s mB");

    private PatternStudioScreenTexts() {
    }
}
