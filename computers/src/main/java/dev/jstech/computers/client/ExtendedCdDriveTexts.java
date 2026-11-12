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

/** The words of an Extended CD Drive's window. */
@TextHolder
final class ExtendedCdDriveTexts {

    /** The tower's name over its window, the same in every era. */
    static final TextKey TITLE = TextKey.of("jsc.extended_cd_drive.title", "Extended CD Drive");
    static final TextKey READ = TextKey.of("jsc.extended_cd_drive.read", "READ");
    static final TextKey SPEED = TextKey.of("jsc.extended_cd_drive.speed", "SPEED");
    static final TextKey COPIES = TextKey.of("jsc.extended_cd_drive.copies", "COPIES");
    static final TextKey COPIES_OF = TextKey.of("jsc.extended_cd_drive.copies_of", "%s / %s");
    static final TextKey KB = TextKey.of("jsc.extended_cd_drive.kb", "%s KB/s");
    static final TextKey COPY = TextKey.of("jsc.extended_cd_drive.copy", "COPY");
    static final TextKey EJECT = TextKey.of("jsc.extended_cd_drive.eject", "EJECT");
    static final TextKey NOTE_1 = TextKey.of("jsc.extended_cd_drive.note_1", "Copies what Easy");
    static final TextKey NOTE_2 = TextKey.of("jsc.extended_cd_drive.note_2", "Disc Creator copies.");
    static final TextKey NO_DISC = TextKey.of("jsc.extended_cd_drive.no_disc", "Put the disc to");
    static final TextKey NO_DISC_2 = TextKey.of("jsc.extended_cd_drive.no_disc_2", "copy here.");
    /** The kind of disc read and its size, which is {@link #MB} or {@link #GB}. */
    static final TextKey SIZE = TextKey.of("jsc.extended_cd_drive.size", "%s, %s");
    static final TextKey MB = TextKey.of("jsc.extended_cd_drive.mb", "%s MB");
    static final TextKey GB = TextKey.of("jsc.extended_cd_drive.gb", "%s GB");
    /** Blu-ray by the mark its discs carry, where the formats a tower writes are listed. */
    static final TextKey BD = TextKey.of("jsc.extended_cd_drive.bd", "BD");
    static final TextKey TRAY = TextKey.of("jsc.extended_cd_drive.tray", "W%s");
    static final TextKey DONE = TextKey.of("jsc.extended_cd_drive.done", "DONE");
    static final TextKey EMPTY = TextKey.of("jsc.extended_cd_drive.empty", "EMPTY");
    static final TextKey READY = TextKey.of("jsc.extended_cd_drive.ready", "READY");
    static final TextKey PERCENT = TextKey.of("jsc.extended_cd_drive.percent", "%s%%");
    static final TextKey WRITES = TextKey.of("jsc.extended_cd_drive.writes", "WRITES");
    static final TextKey BURNED = TextKey.of("jsc.extended_cd_drive.burned", "BURNED");
    static final TextKey DISCS = TextKey.of("jsc.extended_cd_drive.discs", "%s discs");
    static final TextKey REFUSES = TextKey.of("jsc.extended_cd_drive.refuses", "REFUSES");
    static final TextKey STORAGE = TextKey.of("jsc.extended_cd_drive.storage", "storage");
    static final TextKey STATE_READING = TextKey.of("jsc.extended_cd_drive.state.reading", "READING");
    static final TextKey STATE_COPYING = TextKey.of("jsc.extended_cd_drive.state.copying", "COPYING %s OF %s");
    static final TextKey STATE_READY = TextKey.of("jsc.extended_cd_drive.state.ready", "READY");
    static final TextKey STATE_NOTHING = TextKey.of("jsc.extended_cd_drive.state.nothing", "NOTHING TO READ");
    static final TextKey STATE_STORAGE = TextKey.of("jsc.extended_cd_drive.state.storage", "HOLDS STORAGE");
    static final TextKey STATE_NO_BLANKS = TextKey.of("jsc.extended_cd_drive.state.no_blanks", "NO BLANKS");

    private ExtendedCdDriveTexts() {
    }
}
