/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Pattern Studio's server side answers an edit with, kept apart from the handlers that open its window: the
 * language generator loads every class that declares sentences, on a server as well, where windows do not exist.
 */
@TextHolder
final class PatternStudioTexts {

    static final TextKey BENCH_STAGE_ADDED = TextKey.of("jsc.pattern_studio.bench_stage_added", "Bench stage added");
    static final TextKey BENCH_NOT_RECIPE =
            TextKey.of("jsc.pattern_studio.bench_not_recipe", "The bench draft is not a recipe");
    static final TextKey MACHINE_STAGE_ADDED =
            TextKey.of("jsc.pattern_studio.machine_stage_added", "Machine stage added");
    static final TextKey MACHINE_INCOMPLETE = TextKey.of("jsc.pattern_studio.machine_incomplete",
            "The machine draft needs a machine, an input and an output");
    static final TextKey QUEUE_CLEARED = TextKey.of("jsc.pattern_studio.queue_cleared", "Encoder queue cleared");
    static final TextKey NO_ENCODER = TextKey.of("jsc.pattern_studio.no_encoder", "No encoder linked");
    static final TextKey ENCODER_WRITING = TextKey.of("jsc.pattern_studio.encoder_writing", "The encoder is writing");
    static final TextKey BAY_EMPTY = TextKey.of("jsc.pattern_studio.bay_empty", "The bay is empty");
    static final TextKey EJECTED = TextKey.of("jsc.pattern_studio.ejected", "Ejected");
    static final TextKey CANNOT_READ = TextKey.of("jsc.pattern_studio.cannot_read", "Cannot read %s");
    static final TextKey NOT_MACHINE_RECIPE =
            TextKey.of("jsc.pattern_studio.not_machine_recipe", "Not a machine recipe: %s");
    static final TextKey NOT_PIPELINE = TextKey.of("jsc.pattern_studio.not_pipeline", "Not a pipeline: %s");
    static final TextKey NOT_RECIPE_FILE = TextKey.of("jsc.pattern_studio.not_recipe_file", "Not a recipe file: %s");
    static final TextKey OPENED = TextKey.of("jsc.pattern_studio.opened", "Opened %s");
    static final TextKey STAGE_ADDED = TextKey.of("jsc.pattern_studio.stage_added", "Stage added: %s");
    static final TextKey COULD_NOT_ADD = TextKey.of("jsc.pattern_studio.could_not_add", "Could not add %s");
    static final TextKey PIPELINE_IN_PIPELINE = TextKey.of("jsc.pattern_studio.pipeline_in_pipeline",
            "A pipeline cannot hold another pipeline");
    static final TextKey DRAFT_INCOMPLETE =
            TextKey.of("jsc.pattern_studio.draft_incomplete", "The draft is not complete");
    static final TextKey NO_SYSTEM_DISK = TextKey.of("jsc.pattern_studio.no_system_disk", "No system disk to save to");
    static final TextKey SAVE_FAILED = TextKey.of("jsc.pattern_studio.save_failed", "Save failed: %s");
    static final TextKey SAVED = TextKey.of("jsc.pattern_studio.saved", "Saved %s");
    static final TextKey ONLY_CRAFTING_COMPUTER = TextKey.of("jsc.pattern_studio.only_crafting_computer",
            "Only a Crafting Computer holds a Recipe ROM");
    static final TextKey CARD_REQUIRED = TextKey.of("jsc.pattern_studio.card_required", "A Crafting Card is required");
    static final TextKey PIPELINE_EMPTY = TextKey.of("jsc.pattern_studio.pipeline_empty", "The pipeline has no stages");
    static final TextKey NOT_LOADED = TextKey.of("jsc.pattern_studio.not_loaded",
            "Not loaded: already in the ROM, or the ROM is full");
    static final TextKey LOADED = TextKey.of("jsc.pattern_studio.loaded", "Loaded into the ROM: %s");
    static final TextKey NO_PATTERN_ENCODER = TextKey.of("jsc.pattern_studio.no_pattern_encoder",
            "No Pattern Encoder is linked to this computer");
    static final TextKey ENCODER_BAY_EMPTY =
            TextKey.of("jsc.pattern_studio.encoder_bay_empty", "The encoder's bay is empty");
    static final TextKey ENCODER_QUEUE_FULL =
            TextKey.of("jsc.pattern_studio.encoder_queue_full", "The encoder's queue is full");
    static final TextKey SENT = TextKey.of("jsc.pattern_studio.sent", "Sent to the encoder: %s.craft");
    /* A drive and the medium in it: "CD Drive: Blank CD-RW". */
    static final TextKey DRIVE_MEDIUM = TextKey.of("jsc.pattern_studio.drive_medium", "%s: %s");
    static final TextKey SYSTEM_DISK = TextKey.of("jsc.pattern_studio.system_disk", "System disk");
    /* A drive and the folder on it the Studio keeps its files in. */
    static final TextKey IN_FOLDER = TextKey.of("jsc.pattern_studio.in_folder", "%s (%s)");

    private PatternStudioTexts() {
    }
}
