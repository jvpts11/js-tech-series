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

/** The words of the copy windows, each system's in its own phrasing. */
@TextHolder
final class CopyTexts {

    // Frames 95 and XP.
    static final TextKey COPYING = TextKey.of("jsc.copy.copying", "Copying...");
    static final TextKey MOVING = TextKey.of("jsc.copy.moving", "Moving...");
    static final TextKey DELETING = TextKey.of("jsc.copy.deleting", "Deleting...");
    static final TextKey FROM_TO = TextKey.of("jsc.copy.from_to", "From '%s' to '%s'");
    static final TextKey FROM = TextKey.of("jsc.copy.from", "From '%s'");
    static final TextKey SECONDS_REMAINING = TextKey.of("jsc.copy.seconds_remaining", "%s Seconds Remaining");
    static final TextKey CANCEL = TextKey.of("jsc.copy.cancel", "Cancel");

    // Frames 11.
    static final TextKey PERCENT_COMPLETE = TextKey.of("jsc.copy.percent_complete", "%s%% complete");
    static final TextKey PAUSED_PERCENT = TextKey.of("jsc.copy.paused_percent", "Paused - %s%% complete");
    static final TextKey COPYING_ITEMS = TextKey.of("jsc.copy.copying_items", "Copying %s items from %s to %s");
    static final TextKey NAME = TextKey.of("jsc.copy.name", "Name:");
    static final TextKey TIME_REMAINING = TextKey.of("jsc.copy.time_remaining", "Time remaining:");
    static final TextKey ABOUT_SECONDS = TextKey.of("jsc.copy.about_seconds", "About %s seconds");
    static final TextKey ITEMS_REMAINING = TextKey.of("jsc.copy.items_remaining", "Items remaining:");
    static final TextKey ITEMS_LEFT = TextKey.of("jsc.copy.items_left", "%s (%s MB)");
    static final TextKey FEWER_DETAILS = TextKey.of("jsc.copy.fewer_details", "Fewer details");

    // KDE 2 and 3, KIO's progress dialog.
    static final TextKey PROGRESS_DIALOG = TextKey.of("jsc.copy.progress_dialog", "Progress Dialog - Konqueror");
    static final TextKey KIO_COPYING = TextKey.of("jsc.copy.kio_copying", "Copying");
    static final TextKey SOURCE = TextKey.of("jsc.copy.source", "Source:");
    static final TextKey DESTINATION = TextKey.of("jsc.copy.destination", "Destination:");
    static final TextKey FILE_URL = TextKey.of("jsc.copy.file_url", "file:/%s");
    static final TextKey FILES_OF = TextKey.of("jsc.copy.files_of", "%s of %s files");
    static final TextKey MB_OF = TextKey.of("jsc.copy.mb_of", "%s MB of %s MB");
    static final TextKey RATE_REMAINING = TextKey.of("jsc.copy.rate_remaining", "%s MB/s ( %s remaining )");
    static final TextKey KEEP_OPEN = TextKey.of("jsc.copy.keep_open",
            "Keep this window open after transfer is complete");
    static final TextKey OPEN_FILE = TextKey.of("jsc.copy.open_file", "Open File");
    static final TextKey OPEN_DESTINATION = TextKey.of("jsc.copy.open_destination", "Open Destination");

    // Plasma's notification.
    static final TextKey FILE_TO = TextKey.of("jsc.copy.file_to", "%s to %s");
    static final TextKey MIB_PER_SECOND = TextKey.of("jsc.copy.mib_per_second", "%s MiB/s");
    static final TextKey PAUSED = TextKey.of("jsc.copy.paused", "Paused");

    // GNOME 1's gmc.
    static final TextKey COPYING_FILES = TextKey.of("jsc.copy.copying_files", "Copying files");
    static final TextKey COPYING_FROM = TextKey.of("jsc.copy.copying_from", "Copying from:");
    static final TextKey TO = TextKey.of("jsc.copy.to", "To:");

    // GNOME's Files and Cinnamon's Nemo.
    static final TextKey COPYING_QUOTED = TextKey.of("jsc.copy.copying_quoted", "Copying \"%s\" to \"%s\"");
    static final TextKey DELETING_QUOTED = TextKey.of("jsc.copy.deleting_quoted", "Trashing \"%s\"");
    static final TextKey PROGRESS_LINE = TextKey.of("jsc.copy.progress_line",
            "%s MB of %s MB - %s seconds left (%s MB/sec)");
    static final TextKey FILE_OPERATIONS = TextKey.of("jsc.copy.file_operations", "File Operations");

    private CopyTexts() {
    }
}
