/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Backup Service answers, at the network's prompt and in the Backup Console alike, so the two never word the
 * same outcome differently.
 */
@TextHolder
public final class BackupTexts {

    public static final TextKey NO_SERVICE = TextKey.of("jsc.backup.no_service",
            "No Backup Service is running on this network.");
    public static final TextKey NO_JOB = TextKey.of("jsc.backup.no_job",
            "The Backup Service has no job called %s.");
    public static final TextKey QUEUED = TextKey.of("jsc.backup.queued",
            "Backup %s is under way: it waits for the Tape Library's drive.");
    public static final TextKey WRITTEN = TextKey.of("jsc.backup.written", "Backup %s written.");
    public static final TextKey NO_ROOM = TextKey.of("jsc.backup.no_room",
            "No room for backup %s where its job writes it.");
    public static final TextKey NO_TARGET = TextKey.of("jsc.backup.no_target",
            "Backup %s has nowhere to go: its Tape Library, tapes or drive are not on this network.");
    public static final TextKey QUEUE_FULL = TextKey.of("jsc.backup.queue_full",
            "The Tape Library's drive has too much queued already.");
    public static final TextKey BUSY = TextKey.of("jsc.backup.busy", "Backup %s is already under way.");
    public static final TextKey MISSING = TextKey.of("jsc.backup.missing",
            "Nothing on this network holds that backup any more.");
    public static final TextKey WINDING = TextKey.of("jsc.backup.winding",
            "Winding tape %s to the backup.");
    public static final TextKey RESTORED = TextKey.of("jsc.backup.restored",
            "Restored: %s items back, %s computers written.");
    public static final TextKey TAPE_IN_DRIVE = TextKey.of("jsc.backup.tape_in_drive",
            "Tape %s is in the drive: wait for it to finish.");
    public static final TextKey ERASED = TextKey.of("jsc.backup.erased", "Tape %s erased.");
    public static final TextKey LABELLED = TextKey.of("jsc.backup.labelled", "Tape %s labelled %s.");
    public static final TextKey EJECTED = TextKey.of("jsc.backup.ejected", "Tape %s ejected.");
    public static final TextKey NO_TAPE = TextKey.of("jsc.backup.no_tape", "There is no tape in slot %s.");
    public static final TextKey BAD_NAME = TextKey.of("jsc.backup.bad_name",
            "A job's name is one word of letters, digits and underscores, up to %s of them.");
    public static final TextKey FULL = TextKey.of("jsc.backup.full", "The Backup Service keeps %s jobs at most.");
    public static final TextKey NAME_TAKEN = TextKey.of("jsc.backup.name_taken", "A job called %s already exists.");
    public static final TextKey NOTHING_CHOSEN = TextKey.of("jsc.backup.nothing_chosen",
            "Choose something to back up.");
    public static final TextKey SAVED = TextKey.of("jsc.backup.saved", "Job %s saved.");
    public static final TextKey DELETED = TextKey.of("jsc.backup.deleted", "Job %s deleted.");
    public static final TextKey NO_ENGINE = TextKey.of("jsc.backup.no_engine",
            "Job %s saved, but no Automation Engine runs on this network to run it on time.");

    private BackupTexts() {
    }
}
