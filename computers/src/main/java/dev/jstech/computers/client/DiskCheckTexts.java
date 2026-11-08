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
 * What the Frames family says as it checks its disks after a sudden shutdown, in the player's language as the real
 * ones were in theirs. A Unix's fsck is not here: no fsck ever printed in anything but its own words, so those lines
 * come from the machine as they are.
 */
@TextHolder
final class DiskCheckTexts {

    // Frames 95: ScanDisk.
    static final TextKey NOT_SHUT_DOWN = TextKey.of("jsc.disk_check.not_shut_down",
            "Because Frames was not properly shut down, one or more of your disk drives may have errors on it.");
    static final TextKey CHECKING_AREAS = TextKey.of("jsc.disk_check.checking_areas",
            "ScanDisk is now checking the following areas of drive %s:");
    static final TextKey MEDIA_DESCRIPTOR = TextKey.of("jsc.disk_check.media_descriptor", "Media descriptor");
    static final TextKey ALLOCATION_TABLES = TextKey.of("jsc.disk_check.allocation_tables", "File allocation tables");
    static final TextKey DIRECTORY_STRUCTURE = TextKey.of("jsc.disk_check.directory_structure",
            "Directory structure");
    static final TextKey FILE_SYSTEM = TextKey.of("jsc.disk_check.file_system", "File system");
    static final TextKey FREE_SPACE = TextKey.of("jsc.disk_check.free_space", "Free space");
    static final TextKey SURFACE_SCAN = TextKey.of("jsc.disk_check.surface_scan", "Surface scan");
    static final TextKey AREA_COMPLETE = TextKey.of("jsc.disk_check.area_complete", "%s: %s complete");
    static final TextKey PAUSE = TextKey.of("jsc.disk_check.pause", "Pause");
    static final TextKey MORE_INFO = TextKey.of("jsc.disk_check.more_info", "More Info");
    static final TextKey EXIT = TextKey.of("jsc.disk_check.exit", "Exit");

    // Frames XP and 7: chkdsk as the system comes up.
    static final TextKey CHECKING_ON = TextKey.of("jsc.disk_check.checking_on", "Checking file system on %s");
    static final TextKey TYPE_IS = TextKey.of("jsc.disk_check.type_is", "The type of the file system is %s.");
    static final TextKey NEEDS_CHECKING = TextKey.of("jsc.disk_check.needs_checking",
            "One of your disks needs to be checked for consistency. You may cancel the disk check, but it is strongly"
                    + " recommended that you continue.");
    static final TextKey TO_SKIP = TextKey.of("jsc.disk_check.to_skip",
            "To skip disk checking, press any key within %s second(s).");
    static final TextKey VERIFYING_FILES = TextKey.of("jsc.disk_check.verifying_files",
            "CHKDSK is verifying files (stage 1 of 3)...");
    static final TextKey FILES_DONE = TextKey.of("jsc.disk_check.files_done", "File verification completed.");
    static final TextKey VERIFYING_INDEXES = TextKey.of("jsc.disk_check.verifying_indexes",
            "CHKDSK is verifying indexes (stage 2 of 3)...");
    static final TextKey INDEXES_DONE = TextKey.of("jsc.disk_check.indexes_done", "Index verification completed.");
    static final TextKey VERIFYING_SECURITY = TextKey.of("jsc.disk_check.verifying_security",
            "CHKDSK is verifying security descriptors (stage 3 of 3)...");
    static final TextKey SECURITY_DONE = TextKey.of("jsc.disk_check.security_done",
            "Security descriptor verification completed.");
    static final TextKey VERIFYING_JOURNAL = TextKey.of("jsc.disk_check.verifying_journal",
            "CHKDSK is verifying Usn Journal...");
    static final TextKey JOURNAL_DONE = TextKey.of("jsc.disk_check.journal_done",
            "Usn Journal verification completed.");
    static final TextKey NO_PROBLEMS = TextKey.of("jsc.disk_check.no_problems",
            "Frames has checked the file system and found no problems.");
    static final TextKey TOTAL_SPACE = TextKey.of("jsc.disk_check.total_space", "%s KB total disk space.");
    static final TextKey IN_FILES = TextKey.of("jsc.disk_check.in_files", "%s KB in %s files.");
    static final TextKey AVAILABLE = TextKey.of("jsc.disk_check.available", "%s KB available on disk.");

    // Frames 10 and 11: the line under the mark.
    static final TextKey SCANNING = TextKey.of("jsc.disk_check.scanning",
            "Scanning and repairing drive (%s): %s complete");

    private DiskCheckTexts() {
    }
}
