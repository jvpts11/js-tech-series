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
 * What the Pattern Encoder's panel and the Patterns heading of a network machine's space say, kept apart from the
 * screens that draw it so the language generator can read it on a server too.
 */
@TextHolder
final class PatternTexts {

    // The encoder's panel.
    static final TextKey TITLE = TextKey.of("jsc.pattern_encoder.screen.title", "PATTERN ENCODER");
    static final TextKey EJECT = TextKey.of("jsc.pattern_encoder.screen.eject", "Eject");
    static final TextKey CANCEL_QUEUE = TextKey.of("jsc.pattern_encoder.screen.cancel_queue", "Cancel queue");
    static final TextKey LINKED_TO = TextKey.of("jsc.pattern_encoder.screen.linked_to", "Linked to %s");
    static final TextKey NOT_LINKED = TextKey.of("jsc.pattern_encoder.screen.not_linked", "Not linked");
    static final TextKey BAY_EMPTY = TextKey.of("jsc.pattern_encoder.screen.bay_empty", "Bay: empty");
    static final TextKey BAY = TextKey.of("jsc.pattern_encoder.screen.bay", "Bay: %s");
    static final TextKey REMOVABLE_MEDIUM =
            TextKey.of("jsc.pattern_encoder.screen.removable_medium", "Removable medium");
    static final TextKey CONNECT_CABLE =
            TextKey.of("jsc.pattern_encoder.screen.connect_cable", "Connect a peripheral cable");
    static final TextKey QUEUED = TextKey.of("jsc.pattern_encoder.screen.queued", "%s (%s queued)");
    static final TextKey VINTAGE_WRITES =
            TextKey.of("jsc.pattern_encoder.screen.vintage_writes", "Vintage encoder: floppy disks");
    static final TextKey LEGACY_WRITES = TextKey.of("jsc.pattern_encoder.screen.legacy_writes", "Legacy encoder: CD-RW");
    static final TextKey STANDARD_WRITES =
            TextKey.of("jsc.pattern_encoder.screen.standard_writes", "Standard encoder: DVD, CD, USB");

    // The Patterns heading.
    static final TextKey DRAFT = TextKey.of("jsc.terminal.patterns.draft", "Draft");
    static final TextKey MAKES = TextKey.of("jsc.terminal.patterns.makes", "makes");
    static final TextKey ASKING = TextKey.of("jsc.terminal.patterns.asking", "asking the machine ...");
    static final TextKey ENCODER_HEADING = TextKey.of("jsc.terminal.patterns.encoder", "Pattern Encoder");
    static final TextKey NONE_LINKED = TextKey.of("jsc.terminal.patterns.none_linked", "none linked to this machine");
    /* Two lines that read as one sentence. */
    static final TextKey BORN_AT_ENCODER =
            TextKey.of("jsc.terminal.patterns.born_at_encoder", "A pattern is born at an encoder;");
    static final TextKey TAUGHT_IN_ROM =
            TextKey.of("jsc.terminal.patterns.taught_in_rom", "the ROM is where it is taught.");
    static final TextKey LINKED = TextKey.of("jsc.terminal.patterns.linked", "linked");
    static final TextKey LINKED_ERA = TextKey.of("jsc.terminal.patterns.linked_era", "linked  %s");
    static final TextKey NO_MEDIUM_IN_BAY = TextKey.of("jsc.terminal.patterns.no_medium_in_bay", "no medium in its bay");
    static final TextKey SEND_DRAFT_TO = TextKey.of("jsc.terminal.patterns.send_draft_to", "Send this draft to");
    static final TextKey SEND_ENCODER = TextKey.of("jsc.terminal.patterns.send_encoder", "Encoder");
    static final TextKey SEND_DISK = TextKey.of("jsc.terminal.patterns.send_disk", "Disk");
    static final TextKey SEND_ROM = TextKey.of("jsc.terminal.patterns.send_rom", "ROM");
    static final TextKey NO_MEDIUM_IN_DRIVE =
            TextKey.of("jsc.terminal.patterns.no_medium_in_drive", "No medium in a linked drive");
    static final TextKey MEDIUM = TextKey.of("jsc.terminal.patterns.medium", "Medium: %s");
    static final TextKey NOTHING_TO_LOAD = TextKey.of("jsc.terminal.patterns.nothing_to_load", "nothing on it to load");
    static final TextKey LOAD_ALL = TextKey.of("jsc.terminal.patterns.load_all", "Load all");
    static final TextKey LOAD_ONE = TextKey.of("jsc.terminal.patterns.load_one", "Load one");
    static final TextKey RECIPE_ROM = TextKey.of("jsc.terminal.patterns.recipe_rom", "Recipe ROM");
    /* How full the ROM is: "3 of 64". */
    static final TextKey ROM_COUNT = TextKey.of("jsc.terminal.patterns.rom_count", "%s of %s");
    static final TextKey NOTHING_TAUGHT = TextKey.of("jsc.terminal.patterns.nothing_taught", "nothing taught yet");
    static final TextKey UNLOAD = TextKey.of("jsc.terminal.patterns.unload", "Unload");
    static final TextKey DOWNLOAD = TextKey.of("jsc.terminal.patterns.download", "Download");

    private PatternTexts() {
    }
}
