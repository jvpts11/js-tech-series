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

/** The words of Easy Disc Creator's window. */
@TextHolder
final class EasyDiscCreatorTexts {

    static final TextKey TITLE = TextKey.of("jsc.edc.title", "Easy Disc Creator - Disc Copier");
    static final TextKey BRAND = TextKey.of("jsc.edc.brand", "roxa");
    static final TextKey NAME = TextKey.of("jsc.edc.name", "Easy Disc Creator");
    static final TextKey SUBTITLE = TextKey.of("jsc.edc.subtitle", "Disc Copier");
    static final TextKey TAB = TextKey.of("jsc.edc.tab", "Copy");
    static final TextKey COPY_FROM = TextKey.of("jsc.edc.copy_from", "Copy from:");
    static final TextKey RECORD_TO = TextKey.of("jsc.edc.record_to", "Record to:");
    static final TextKey SPEED = TextKey.of("jsc.edc.speed", "Speed:");
    static final TextKey COPIES = TextKey.of("jsc.edc.copies", "Copies:");
    static final TextKey PLACE = TextKey.of("jsc.edc.place", "%s: %s");
    static final TextKey NO_DRIVES = TextKey.of("jsc.edc.no_drives", "No drive is linked to this computer");
    /* What a drive holds, in the second column of its list. */
    static final TextKey EMPTY = TextKey.of("jsc.edc.empty", "empty");
    static final TextKey BLANK = TextKey.of("jsc.edc.blank", "blank %s");
    static final TextKey BLANK_VINYL = TextKey.of("jsc.edc.blank_vinyl", "a blank vinyl");
    static final TextKey STORAGE = TextKey.of("jsc.edc.storage", "network storage");
    static final TextKey NAMED = TextKey.of("jsc.edc.named", "%s, %s");
    /* What the chosen drive holds, on the line under its field. */
    static final TextKey INFO_EMPTY = TextKey.of("jsc.edc.info_empty", "Nothing in it");
    static final TextKey INFO_STORAGE = TextKey.of("jsc.edc.info_storage", "A storage disk of this network");
    static final TextKey INFO_SONG = TextKey.of("jsc.edc.info_song", "Music disc: %s, %s");
    static final TextKey INFO_BLANK_VINYL = TextKey.of("jsc.edc.info_blank_vinyl", "A blank vinyl");
    static final TextKey INFO_BLANK = TextKey.of("jsc.edc.info_blank", "A blank %s, %s");
    static final TextKey INFO_ROOM = TextKey.of("jsc.edc.info_room", "A blank %s, %s: room for the copy");
    static final TextKey INFO_TOO_SMALL = TextKey.of("jsc.edc.info_too_small",
            "A blank %s, %s: too small for the copy");
    static final TextKey INFO_NAMED = TextKey.of("jsc.edc.info_named", "%s: a %s, %s of %s");
    static final TextKey INFO_UNNAMED = TextKey.of("jsc.edc.info_unnamed", "A %s, %s of %s");
    static final TextKey SAME_VINYL = TextKey.of("jsc.edc.same_vinyl", "The same drive: it records the disc first");
    static final TextKey ONE_DRIVE = TextKey.of("jsc.edc.one_drive",
            "One drive: the source is read first, then the blank is asked for.");
    static final TextKey OWN_SPEED = TextKey.of("jsc.edc.own_speed", "The blank's own speed.");
    static final TextKey SPEED_KB = TextKey.of("jsc.edc.speed_kb", "%s, the blank's own speed.");
    static final TextKey SPEED_ABOUT = TextKey.of("jsc.edc.speed_about", "%s, the blank's own speed. About %s.");
    static final TextKey SPEED_SONG = TextKey.of("jsc.edc.speed_song", "The song's own time: %s to read, %s to cut.");
    static final TextKey SPEED_AT_ONCE = TextKey.of("jsc.edc.speed_at_once",
            "At once: a USB stick has no head to move.");
    static final TextKey RATE = TextKey.of("jsc.edc.rate", "%s KB/s");
    static final TextKey AT_ONCE = TextKey.of("jsc.edc.at_once", "at once");
    static final TextKey COPY = TextKey.of("jsc.edc.copy", "Copy");
    static final TextKey CLOSE = TextKey.of("jsc.edc.close", "Close");
    static final TextKey CANCEL = TextKey.of("jsc.edc.cancel", "Cancel");
    static final TextKey OK = TextKey.of("jsc.edc.ok", "OK");
    static final TextKey COPYING = TextKey.of("jsc.edc.copying", "Copying %s (%s:) onto the blank %s in %s:");
    static final TextKey COPYING_VINYL = TextKey.of("jsc.edc.copying_vinyl", "Copying %s (%s:) onto a blank vinyl");
    static final TextKey THE_DISC = TextKey.of("jsc.edc.the_disc", "the disc");
    static final TextKey READING = TextKey.of("jsc.edc.reading", "Reading the source");
    static final TextKey WRITING = TextKey.of("jsc.edc.writing", "Writing to %s: at %s");
    static final TextKey CHECKING = TextKey.of("jsc.edc.checking", "Checking the copy");
    static final TextKey RECORDING = TextKey.of("jsc.edc.recording", "Recording the disc in the song's own time");
    static final TextKey CUTTING = TextKey.of("jsc.edc.cutting", "Cutting the copy onto the blank vinyl");
    static final TextKey DONE = TextKey.of("jsc.edc.step_done", "done");
    static final TextKey WAITING = TextKey.of("jsc.edc.step_waiting", "waiting");
    static final TextKey PERCENT = TextKey.of("jsc.edc.percent", "%s%%");
    static final TextKey SIZE_OF = TextKey.of("jsc.edc.size_of", "%s of %s");
    static final TextKey TIME_LEFT = TextKey.of("jsc.edc.time_left", "Time left: about %s");
    static final TextKey SECOND = TextKey.of("jsc.edc.second", "1 second");
    static final TextKey SECONDS = TextKey.of("jsc.edc.seconds", "%s seconds");
    static final TextKey MINUTE = TextKey.of("jsc.edc.minute", "1 minute");
    static final TextKey MINUTES = TextKey.of("jsc.edc.minutes", "%s minutes");
    static final TextKey NOTE = TextKey.of("jsc.edc.note", "Install discs copy like any other. A disk that holds the "
            + "network's storage, or a .dat file, never copies.");
    static final TextKey FINISHED = TextKey.of("jsc.edc.finished", "The copy is over.");

    private EasyDiscCreatorTexts() {
    }
}
