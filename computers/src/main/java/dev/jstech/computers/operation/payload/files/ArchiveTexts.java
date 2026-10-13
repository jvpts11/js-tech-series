/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the machine answers when files are packed into an archive or taken back out: what was done, or why it was
 * not. File names and counts are data. Kept apart from the handlers, which reach the client's windows, so the
 * language generator can read it on a server.
 */
@TextHolder
final class ArchiveTexts {

    // Packing.
    static final TextKey NOTHING_TO_ARCHIVE = TextKey.of("jsc.archive.nothing_to_archive", "Nothing to archive");
    static final TextKey NOTHING_IN = TextKey.of("jsc.archive.nothing_in", "Nothing in %s");
    static final TextKey HOLDS_ITSELF = TextKey.of("jsc.archive.holds_itself", "An archive cannot hold itself");
    static final TextKey MISSING = TextKey.of("jsc.archive.missing", "Missing %s");
    static final TextKey TOO_MUCH = TextKey.of("jsc.archive.too_much", "Too much to pack into one archive");
    static final TextKey INVALID_NAME = TextKey.of("jsc.archive.invalid_name", "Invalid archive name");
    static final TextKey PACKED = TextKey.of("jsc.archive.packed", "Packed %s into %s, %s bytes became %s");
    static final TextKey PACKED_REMOVED =
            TextKey.of("jsc.archive.packed_removed", "Packed %s into %s, %s bytes became %s, %s removed");

    // Taking out.
    static final TextKey NO_SUCH_ARCHIVE = TextKey.of("jsc.archive.no_such_archive", "No such archive");
    static final TextKey NOT_AN_ARCHIVE = TextKey.of("jsc.archive.not_an_archive", "%s is not an archive");
    static final TextKey DAMAGED = TextKey.of("jsc.archive.damaged", "The archive is damaged");
    static final TextKey NOT_INSIDE = TextKey.of("jsc.archive.not_inside", "No %s in the archive");
    static final TextKey ONE_ALREADY_THERE = TextKey.of("jsc.archive.one_already_there", "That one is already there");
    static final TextKey ALL_ALREADY_THERE = TextKey.of("jsc.archive.all_already_there", "All %s are already there");
    static final TextKey NO_ROOM_FOR_ONE =
            TextKey.of("jsc.archive.no_room_for_one", "Not enough free space for %s file");
    static final TextKey NO_ROOM_FOR = TextKey.of("jsc.archive.no_room_for", "Not enough free space for %s files");
    static final TextKey NOTHING_WRITTEN = TextKey.of("jsc.archive.nothing_written", "Nothing could be written");
    static final TextKey TOOK_OUT_ONE_SKIPPED =
            TextKey.of("jsc.archive.took_out_one_skipped", "Took out %s file, %s already there");
    static final TextKey TOOK_OUT_SKIPPED =
            TextKey.of("jsc.archive.took_out_skipped", "Took out %s files, %s already there");
    static final TextKey TOOK_OUT_ONE = TextKey.of("jsc.archive.took_out_one", "Took out %s file");
    static final TextKey TOOK_OUT = TextKey.of("jsc.archive.took_out", "Took out %s files");

    private ArchiveTexts() {
    }
}
