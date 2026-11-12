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

/** What the Backup Console says, in its four tabs and its status bar. */
@TextHolder
final class BackupConsoleTexts {

    static final TextKey TITLE = TextKey.of("jsc.backup_console.title", "Backup Console");
    static final TextKey JOBS_TAB = TextKey.of("jsc.backup_console.tab.jobs", "Jobs");
    static final TextKey BACK_UP_TAB = TextKey.of("jsc.backup_console.tab.back_up", "Back up");
    static final TextKey RESTORE_TAB = TextKey.of("jsc.backup_console.tab.restore", "Restore");
    static final TextKey TAPES_TAB = TextKey.of("jsc.backup_console.tab.tapes", "Tapes");

    // Jobs
    static final TextKey JOBS_INTRO = TextKey.of("jsc.backup_console.jobs.intro",
            "The jobs the Backup Service runs. The timed ones are Automation Engine jobs.");
    static final TextKey JOB_COLUMN = TextKey.of("jsc.backup_console.column.job", "Job");
    static final TextKey WHAT_COLUMN = TextKey.of("jsc.backup_console.column.what", "What");
    static final TextKey WHERE_COLUMN = TextKey.of("jsc.backup_console.column.where", "Where");
    static final TextKey WHEN_COLUMN = TextKey.of("jsc.backup_console.column.when", "When");
    static final TextKey LAST_RUN_COLUMN = TextKey.of("jsc.backup_console.column.last_run", "Last run");
    static final TextKey NO_JOBS = TextKey.of("jsc.backup_console.jobs.none", "No jobs yet: make one with New job.");
    static final TextKey NEW_JOB = TextKey.of("jsc.backup_console.new_job", "New job...");
    static final TextKey RUN_NOW = TextKey.of("jsc.backup_console.run_now", "Run now");
    static final TextKey EDIT = TextKey.of("jsc.backup_console.edit", "Edit...");
    static final TextKey DELETE = TextKey.of("jsc.backup_console.delete", "Delete");
    static final TextKey PART_ITEMS = TextKey.of("jsc.backup_console.part.items", "items");
    static final TextKey PART_FILES = TextKey.of("jsc.backup_console.part.files", "files");
    static final TextKey PART_SETTINGS = TextKey.of("jsc.backup_console.part.settings", "settings");
    static final TextKey PART_PATTERNS = TextKey.of("jsc.backup_console.part.patterns", ".craft patterns");
    static final TextKey PART_SCRIPTS = TextKey.of("jsc.backup_console.part.scripts", "scripts");
    static final TextKey PART_IMAGES = TextKey.of("jsc.backup_console.part.images", "system images");
    static final TextKey PART_WALLETS = TextKey.of("jsc.backup_console.part.wallets", "Teracoin wallets");
    static final TextKey IMAGES_OF = TextKey.of("jsc.backup_console.images_of", "System images: %s");
    static final TextKey TAPE_LIBRARY = TextKey.of("jsc.backup_console.tape_library", "Tape Library");
    static final TextKey DRIVE_OF = TextKey.of("jsc.backup_console.drive_of", "%s, disk %s:");
    static final TextKey BY_HAND = TextKey.of("jsc.backup_console.by_hand", "By hand");
    static final TextKey EVERY_NIGHT = TextKey.of("jsc.backup_console.every_night", "Every night");
    static final TextKey EVERY_DAY = TextKey.of("jsc.backup_console.every_day", "Every day");
    static final TextKey WHEN_AT = TextKey.of("jsc.backup_console.when_at", "%s, %s");
    static final TextKey MONDAYS = TextKey.of("jsc.backup_console.mondays", "Mondays");
    static final TextKey TUESDAYS = TextKey.of("jsc.backup_console.tuesdays", "Tuesdays");
    static final TextKey WEDNESDAYS = TextKey.of("jsc.backup_console.wednesdays", "Wednesdays");
    static final TextKey THURSDAYS = TextKey.of("jsc.backup_console.thursdays", "Thursdays");
    static final TextKey FRIDAYS = TextKey.of("jsc.backup_console.fridays", "Fridays");
    static final TextKey SATURDAYS = TextKey.of("jsc.backup_console.saturdays", "Saturdays");
    static final TextKey SUNDAYS = TextKey.of("jsc.backup_console.sundays", "Sundays");
    /** The days of the week a weekly job runs on, in the order the world's calendar counts them. */
    static final TextKey[] WEEKDAYS = {MONDAYS, TUESDAYS, WEDNESDAYS, THURSDAYS, FRIDAYS, SATURDAYS, SUNDAYS};
    static final TextKey DAY_CLOCK = TextKey.of("jsc.backup_console.day_clock", "Day %s, %s");
    static final TextKey LAST_RUN = TextKey.of("jsc.backup_console.last_run", "%s: %s");
    static final TextKey NEVER = TextKey.of("jsc.backup_console.never", "never");
    static final TextKey DONE = TextKey.of("jsc.backup_console.result.done", "done");
    static final TextKey NO_ROOM = TextKey.of("jsc.backup_console.result.no_room", "no room");
    static final TextKey NO_TARGET = TextKey.of("jsc.backup_console.result.no_target", "nowhere to go");
    static final TextKey INTERRUPTED = TextKey.of("jsc.backup_console.result.interrupted", "cut short");
    static final TextKey RUNNING = TextKey.of("jsc.backup_console.result.running", "running");

    // Back up
    static final TextKey WHAT_HEADING = TextKey.of("jsc.backup_console.what_heading", "What to back up");
    static final TextKey NAME = TextKey.of("jsc.backup_console.name", "Name");
    static final TextKey ITEMS = TextKey.of("jsc.backup_console.items", "Items");
    static final TextKey FILES = TextKey.of("jsc.backup_console.files", "Files");
    static final TextKey SETTINGS = TextKey.of("jsc.backup_console.settings", "Settings");
    static final TextKey PATTERNS = TextKey.of("jsc.backup_console.patterns", ".craft patterns");
    static final TextKey SCRIPTS = TextKey.of("jsc.backup_console.scripts", "IQL and Sigma# scripts");
    static final TextKey IMAGES = TextKey.of("jsc.backup_console.images", "System images");
    static final TextKey WALLETS = TextKey.of("jsc.backup_console.wallets", "Teracoin wallets");
    static final TextKey NO_WALLETS = TextKey.of("jsc.backup_console.no_wallets", "no computer keeps one");
    static final TextKey ITEMS_DETAIL = TextKey.of("jsc.backup_console.items_detail",
            "the network's storage: %s items, %s");
    static final TextKey NO_HOMES = TextKey.of("jsc.backup_console.no_homes", "no home folders");
    static final TextKey EVERY_COMPUTER = TextKey.of("jsc.backup_console.every_computer", "every computer");
    static final TextKey THE_NETWORKS = TextKey.of("jsc.backup_console.the_networks", "the network's");
    static final TextKey NONE_CHOSEN = TextKey.of("jsc.backup_console.none_chosen", "none chosen");
    static final TextKey ITEMS_NOTE = TextKey.of("jsc.backup_console.items_note",
            "Items come back only when they stopped existing: lost to a failure, a virus or a DROP.");
    static final TextKey WHERE_HEADING = TextKey.of("jsc.backup_console.where_heading", "Where");
    static final TextKey TO_LIBRARY = TextKey.of("jsc.backup_console.to_library",
            "The Tape Library: %s (%s free)");
    static final TextKey TO_DRIVE = TextKey.of("jsc.backup_console.to_drive", "Another server's disk: %s (%s free)");
    static final TextKey NO_LIBRARY = TextKey.of("jsc.backup_console.no_library", "The Tape Library: none here");
    static final TextKey NO_DRIVE = TextKey.of("jsc.backup_console.no_drive", "Another server's disk: none here");
    static final TextKey NEXT = TextKey.of("jsc.backup_console.next", "Next");
    static final TextKey WHEN_HEADING = TextKey.of("jsc.backup_console.when_heading", "When");
    static final TextKey AS_JOB = TextKey.of("jsc.backup_console.as_job", "As an Automation Engine job:");
    static final TextKey CREATE = TextKey.of("jsc.backup_console.create", "Create");
    static final TextKey SAVE = TextKey.of("jsc.backup_console.save", "Save");
    static final TextKey CANCEL = TextKey.of("jsc.backup_console.cancel", "Cancel");

    // Restore
    static final TextKey MADE_COLUMN = TextKey.of("jsc.backup_console.column.made", "Made");
    static final TextKey ON_COLUMN = TextKey.of("jsc.backup_console.column.on", "On");
    static final TextKey SIZE_COLUMN = TextKey.of("jsc.backup_console.column.size", "Size");
    static final TextKey ON_TAPE = TextKey.of("jsc.backup_console.on_tape", "Tape Library, tape %s");
    static final TextKey NO_BACKUPS = TextKey.of("jsc.backup_console.no_backups", "No backups on this network yet.");
    static final TextKey IN_BACKUP = TextKey.of("jsc.backup_console.in_backup", "In this backup");
    static final TextKey IMAGE_ROW = TextKey.of("jsc.backup_console.image_row", "System image of %s: %s, %s");
    static final TextKey IMAGE_NOTE = TextKey.of("jsc.backup_console.image_note",
            "Restoring an image puts its computer's system back as it was: it fixes a corrupted system.");
    static final TextKey ALSO = TextKey.of("jsc.backup_console.also", "Also puts back: %s");
    static final TextKey LOST_HEADING = TextKey.of("jsc.backup_console.lost_heading",
            "Lost since this backup, from the network's loss book");
    static final TextKey ITEM_COLUMN = TextKey.of("jsc.backup_console.column.item", "Item");
    static final TextKey LOST_COLUMN = TextKey.of("jsc.backup_console.column.lost", "Lost");
    static final TextKey HOW_COLUMN = TextKey.of("jsc.backup_console.column.how", "How");
    static final TextKey IN_BACKUP_COLUMN = TextKey.of("jsc.backup_console.column.in_backup", "In backup");
    static final TextKey BACK_COLUMN = TextKey.of("jsc.backup_console.column.back", "Back");
    static final TextKey NONE = TextKey.of("jsc.backup_console.none", "none");
    static final TextKey HOW_DEFRAG = TextKey.of("jsc.backup_console.how.defrag", "Corrupted, Defrag gave up");
    static final TextKey HOW_DEAD_DISK = TextKey.of("jsc.backup_console.how.dead_disk", "%s disk %s died");
    static final TextKey HOW_RAID0 = TextKey.of("jsc.backup_console.how.raid0", "%s lost a RAID 0 drive");
    static final TextKey HOW_DROP = TextKey.of("jsc.backup_console.how.drop", "DROP from %s");
    static final TextKey ONCE_NOTE = TextKey.of("jsc.backup_console.once_note",
            "Each loss comes back once, from one backup.");
    static final TextKey STILL_EXIST_NOTE = TextKey.of("jsc.backup_console.still_exist_note",
            "Items taken out, exported or crafted still exist, so they are not in the book.");
    static final TextKey RESTORING = TextKey.of("jsc.backup_console.restoring", "Restoring %s items: %s");
    static final TextKey NOTHING_LOST = TextKey.of("jsc.backup_console.nothing_lost",
            "Nothing this backup keeps was lost since it was made.");
    static final TextKey WINDING = TextKey.of("jsc.backup_console.winding",
            "Winding tape %s to the backup: about %s seconds");
    static final TextKey RESTORE = TextKey.of("jsc.backup_console.restore", "Restore");
    static final TextKey CLOSE = TextKey.of("jsc.backup_console.close", "Close");

    // Tapes
    static final TextKey TAPES_INTRO = TextKey.of("jsc.backup_console.tapes.intro",
            "The tapes in the Tape Library (%s).");
    static final TextKey DRIVE_IDLE = TextKey.of("jsc.backup_console.drive.idle", "The drive is idle.");
    static final TextKey DRIVE_BUSY = TextKey.of("jsc.backup_console.drive.busy", "Tape %s is in the drive, %s.");
    static final TextKey IDLE = TextKey.of("jsc.backup_console.drive.state.idle", "idle");
    static final TextKey WINDING_STATE = TextKey.of("jsc.backup_console.drive.state.winding", "winding");
    static final TextKey READING = TextKey.of("jsc.backup_console.drive.state.reading", "reading");
    static final TextKey WRITING = TextKey.of("jsc.backup_console.drive.state.writing", "writing");
    static final TextKey NO_LIBRARY_HERE = TextKey.of("jsc.backup_console.no_library_here",
            "There is no Tape Library on this network.");
    static final TextKey SLOT_COLUMN = TextKey.of("jsc.backup_console.column.slot", "Slot");
    static final TextKey LABEL_COLUMN = TextKey.of("jsc.backup_console.column.label", "Label");
    static final TextKey TAPE_COLUMN = TextKey.of("jsc.backup_console.column.tape", "Tape");
    static final TextKey USED_COLUMN = TextKey.of("jsc.backup_console.column.used", "Used");
    static final TextKey WRITTEN_COLUMN = TextKey.of("jsc.backup_console.column.written", "Last written");
    static final TextKey BLANK = TextKey.of("jsc.backup_console.blank", "blank");
    static final TextKey FORMAT = TextKey.of("jsc.backup_console.format", "%s, %s");
    static final TextKey LABEL = TextKey.of("jsc.backup_console.label", "Label...");
    static final TextKey ERASE = TextKey.of("jsc.backup_console.erase", "Erase...");
    static final TextKey EJECT = TextKey.of("jsc.backup_console.eject", "Eject");
    static final TextKey NEXT_LIBRARY = TextKey.of("jsc.backup_console.next_library", "Next library");
    static final TextKey LABEL_TITLE = TextKey.of("jsc.backup_console.label_title", "Label tape %s");
    static final TextKey LABEL_HINT = TextKey.of("jsc.backup_console.label_hint", "What to write on the tape:");
    static final TextKey ERASE_TITLE = TextKey.of("jsc.backup_console.erase_title", "Erase tape %s");
    static final TextKey ERASE_HINT = TextKey.of("jsc.backup_console.erase_hint",
            "Its %s backups go with it, for good.");
    static final TextKey APPLY = TextKey.of("jsc.backup_console.apply", "Apply");

    // Status bar
    static final TextKey SERVICE_ON = TextKey.of("jsc.backup_console.service_on", "Backup Service on %s: running");
    static final TextKey NO_ENGINE = TextKey.of("jsc.backup_console.no_engine",
            "no Automation Engine: timed jobs wait");
    static final TextKey LIBRARY_STATUS = TextKey.of("jsc.backup_console.library_status",
            "Tape Library, %s: %s tapes, %s of %s");
    static final TextKey RACK_ROW = TextKey.of("jsc.backup_console.rack_row", "rack %s, %sU");

    private BackupConsoleTexts() {
    }
}
