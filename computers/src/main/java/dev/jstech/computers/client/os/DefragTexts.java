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

/** The words of the Defrag on every system: DEFRAG's screen, the Frames windows and their legends. */
@TextHolder
final class DefragTexts {

    /* Frames 95. */
    static final TextKey DEFRAGMENTING_DRIVE = TextKey.of("jsc.defrag.defragmenting_drive", "Defragmenting Drive %s");
    static final TextKey READY_DRIVE = TextKey.of("jsc.defrag.ready_drive", "Ready to defragment Drive %s");
    static final TextKey COMPLETE = TextKey.of("jsc.defrag.complete", "%s%% complete");
    static final TextKey COUNTS = TextKey.of("jsc.defrag.counts", "Corrupted items: %s, recovered %s, lost %s");
    static final TextKey START = TextKey.of("jsc.defrag.start", "Start");
    static final TextKey DRIVE = TextKey.of("jsc.defrag.drive", "Drive %s");
    static final TextKey STOP = TextKey.of("jsc.defrag.stop", "Stop");
    static final TextKey PAUSE = TextKey.of("jsc.defrag.pause", "Pause");
    static final TextKey RESUME = TextKey.of("jsc.defrag.resume", "Resume");
    static final TextKey LEGEND = TextKey.of("jsc.defrag.legend", "Legend");
    static final TextKey HIDE_DETAILS = TextKey.of("jsc.defrag.hide_details", "Hide Details");
    static final TextKey SHOW_DETAILS = TextKey.of("jsc.defrag.show_details", "Show Details");
    static final TextKey OPTIMIZED = TextKey.of("jsc.defrag.optimized", "Optimized data");
    static final TextKey UNOPTIMIZED = TextKey.of("jsc.defrag.unoptimized", "Data still to optimize");
    static final TextKey FREE_SPACE = TextKey.of("jsc.defrag.free_space", "Free space");
    static final TextKey READING = TextKey.of("jsc.defrag.reading", "Being read");
    static final TextKey CORRUPTED = TextKey.of("jsc.defrag.corrupted", "Corrupted items");
    static final TextKey BAD_SECTORS = TextKey.of("jsc.defrag.bad_sectors", "Bad sectors");

    /* Frames XP. */
    static final TextKey MENU = TextKey.of("jsc.defrag.menu", "File   Action   View   Help");
    static final TextKey VOLUME = TextKey.of("jsc.defrag.volume", "Volume");
    static final TextKey SESSION = TextKey.of("jsc.defrag.session", "Session Status");
    static final TextKey FILE_SYSTEM = TextKey.of("jsc.defrag.file_system", "File System");
    static final TextKey CAPACITY = TextKey.of("jsc.defrag.capacity", "Capacity");
    static final TextKey FREE = TextKey.of("jsc.defrag.free", "Free");
    static final TextKey DEFRAGMENTING = TextKey.of("jsc.defrag.defragmenting", "Defragmenting...");
    static final TextKey PAUSED = TextKey.of("jsc.defrag.paused", "Paused");
    static final TextKey BEFORE = TextKey.of("jsc.defrag.before", "Estimated disk usage before defragmentation:");
    static final TextKey AFTER = TextKey.of("jsc.defrag.after", "Estimated disk usage after defragmentation:");
    static final TextKey ANALYZE = TextKey.of("jsc.defrag.analyze", "Analyze");
    static final TextKey DEFRAGMENT = TextKey.of("jsc.defrag.defragment", "Defragment");
    static final TextKey VIEW_REPORT = TextKey.of("jsc.defrag.view_report", "View Report");
    static final TextKey FRAGMENTED = TextKey.of("jsc.defrag.fragmented", "Fragmented");
    static final TextKey CONTIGUOUS = TextKey.of("jsc.defrag.contiguous", "Contiguous");
    static final TextKey UNMOVABLE = TextKey.of("jsc.defrag.unmovable", "Unmovable");
    static final TextKey CORRUPTED_SHORT = TextKey.of("jsc.defrag.corrupted_short", "Corrupted");
    static final TextKey REPAIRING = TextKey.of("jsc.defrag.repairing", "Repairing %s: %s corrupted items found,");
    static final TextKey REPAIRED = TextKey.of("jsc.defrag.repaired",
            "%s recovered, %s lost. Bad sectors: %s, now %s.");
    static final TextKey ANALYSIS = TextKey.of("jsc.defrag.analysis",
            "Analysis of %s: %s corrupted items, bad sectors %s.");
    static final TextKey REPORT = TextKey.of("jsc.defrag.report", "%s: %s%% corrupted, %s%% lost to bad sectors.");
    static final TextKey STATUS_RUNNING = TextKey.of("jsc.defrag.status_running", "%s Defragmenting... %s%%");
    static final TextKey STATUS_READY = TextKey.of("jsc.defrag.status_ready", "%s Ready");

    /* Frames 7. */
    static final TextKey INTRO_7_A = TextKey.of("jsc.defrag.intro_7_a",
            "Disk Defragmenter puts the files on your disk back");
    static final TextKey INTRO_7_B = TextKey.of("jsc.defrag.intro_7_b",
            "together and repairs the corrupted items it finds.");
    static final TextKey SCHEDULE = TextKey.of("jsc.defrag.schedule", "Schedule:");
    static final TextKey SCHEDULE_ON = TextKey.of("jsc.defrag.schedule_on", "Scheduled defragmentation is on");
    static final TextKey SCHEDULE_OFF = TextKey.of("jsc.defrag.schedule_off", "Scheduled defragmentation is off");
    static final TextKey RUN_AT = TextKey.of("jsc.defrag.run_at", "Run at 01:00 every 7 days");
    static final TextKey CONFIGURE = TextKey.of("jsc.defrag.configure", "Configure schedule...");
    static final TextKey CURRENT = TextKey.of("jsc.defrag.current", "Current status:");
    static final TextKey DISK = TextKey.of("jsc.defrag.disk", "Disk");
    static final TextKey LAST_RUN = TextKey.of("jsc.defrag.last_run", "Last Run");
    static final TextKey PROGRESS = TextKey.of("jsc.defrag.progress", "Progress");
    static final TextKey RUNNING = TextKey.of("jsc.defrag.running", "Running...");
    static final TextKey PASS = TextKey.of("jsc.defrag.pass", "Pass 1: %s%%");
    static final TextKey NEVER = TextKey.of("jsc.defrag.never", "Never run");
    static final TextKey NOT_FRAGMENTED = TextKey.of("jsc.defrag.not_fragmented", "(0% fragmented)");
    static final TextKey HAS_CORRUPTED = TextKey.of("jsc.defrag.has_corrupted", "%s corrupted items");
    static final TextKey RESULT = TextKey.of("jsc.defrag.result", "%s: %s corrupted items, %s recovered, %s lost.");
    static final TextKey RESULT_BAD = TextKey.of("jsc.defrag.result_bad", "Bad sectors: %s, now %s.");
    static final TextKey ANALYZE_DISK = TextKey.of("jsc.defrag.analyze_disk", "Analyze disk");
    static final TextKey DEFRAGMENT_DISK = TextKey.of("jsc.defrag.defragment_disk", "Defragment disk");
    static final TextKey STOP_OPERATION = TextKey.of("jsc.defrag.stop_operation", "Stop operation");
    static final TextKey CLOSE = TextKey.of("jsc.defrag.close", "Close");
    static final TextKey FAILED = TextKey.of("jsc.defrag.failed", "This disk has failed");
    static final TextKey STORAGE_DRIVE = TextKey.of("jsc.defrag.storage_drive", "Storage %s");

    /* Frames 10 and 11. */
    static final TextKey INTRO_10_A = TextKey.of("jsc.defrag.intro_10_a",
            "You can optimize your drives to help your computer");
    static final TextKey INTRO_10_B = TextKey.of("jsc.defrag.intro_10_b",
            "run better, or analyze them to see if they need it.");
    static final TextKey INTRO_10_C = TextKey.of("jsc.defrag.intro_10_c",
            "It also repairs the corrupted items it finds.");
    static final TextKey STATUS = TextKey.of("jsc.defrag.status", "Status");
    static final TextKey DRIVE_COLUMN = TextKey.of("jsc.defrag.drive_column", "Drive");
    static final TextKey MEDIA_TYPE = TextKey.of("jsc.defrag.media_type", "Media type");
    static final TextKey LAST_ANALYZED = TextKey.of("jsc.defrag.last_analyzed", "Last analyzed");
    static final TextKey CURRENT_STATUS = TextKey.of("jsc.defrag.current_status", "Current status");
    static final TextKey HARD_DISK = TextKey.of("jsc.defrag.hard_disk", "Hard disk");
    static final TextKey SOLID_STATE = TextKey.of("jsc.defrag.solid_state", "Solid state");
    static final TextKey REPAIRING_PERCENT = TextKey.of("jsc.defrag.repairing_percent", "Repairing: %s%%");
    static final TextKey OK = TextKey.of("jsc.defrag.ok", "OK");
    static final TextKey NEEDS_REPAIR = TextKey.of("jsc.defrag.needs_repair", "Needs repair");
    static final TextKey OPTIMIZE = TextKey.of("jsc.defrag.optimize", "Optimize");
    static final TextKey SCHEDULED = TextKey.of("jsc.defrag.scheduled", "Scheduled optimization");
    static final TextKey CHANGE_SETTINGS = TextKey.of("jsc.defrag.change_settings", "Change settings");
    static final TextKey ON = TextKey.of("jsc.defrag.on", "On");
    static final TextKey OFF = TextKey.of("jsc.defrag.off", "Off");
    static final TextKey AUTOMATIC = TextKey.of("jsc.defrag.automatic",
            "Drives are analyzed and optimized automatically.");
    static final TextKey FREQUENCY = TextKey.of("jsc.defrag.frequency", "Frequency: Weekly");

    /* MC-DOS's DEFRAG. */
    static final TextKey DOS_BAR = TextKey.of("jsc.defrag.dos_bar", "Optimize  Esc=Exit");
    static final TextKey DOS_TITLE = TextKey.of("jsc.defrag.dos_title", "MC-DOS Defrag");
    static final TextKey DOS_STATUS = TextKey.of("jsc.defrag.dos_status", "Status");
    static final TextKey DOS_CLUSTER = TextKey.of("jsc.defrag.dos_cluster", "Cluster %s");
    static final TextKey DOS_CORRUPTED = TextKey.of("jsc.defrag.dos_corrupted", "Corrupted items: %s");
    static final TextKey DOS_RECOVERED = TextKey.of("jsc.defrag.dos_recovered", "Recovered %s, lost %s");
    static final TextKey DOS_READY = TextKey.of("jsc.defrag.dos_ready", "Enter to begin, P to pause");
    static final TextKey DOS_FINISHED = TextKey.of("jsc.defrag.dos_finished", "Finished condensing %s");
    static final TextKey DOS_USED = TextKey.of("jsc.defrag.dos_used", "Used");
    static final TextKey DOS_UNUSED = TextKey.of("jsc.defrag.dos_unused", "Unused");
    static final TextKey DOS_READING = TextKey.of("jsc.defrag.dos_reading", "Reading");
    static final TextKey DOS_CORRUPT = TextKey.of("jsc.defrag.dos_corrupt", "Corrupted");
    static final TextKey DOS_BAD = TextKey.of("jsc.defrag.dos_bad", "Bad sector");
    static final TextKey DOS_UNMOVABLE = TextKey.of("jsc.defrag.dos_unmovable", "Unmovable");
    static final TextKey DOS_FAILED = TextKey.of("jsc.defrag.dos_failed", "Drive %s has failed");

    private DefragTexts() {
    }
}
