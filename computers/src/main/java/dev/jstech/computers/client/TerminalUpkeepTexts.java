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
 * What a network machine's space says about the work it does: the Ops, Tasks, Upkeep, Programs and Console
 * headings, and the questions about one Operation and about dropping data. Kept apart from the screens so the
 * language generator can read it on a server too, where screens do not exist.
 */
@TextHolder
final class TerminalUpkeepTexts {

    // The Upkeep heading.
    static final TextKey STORAGE_INDEX = TextKey.of("jsc.terminal.upkeep.storage_index", "STORAGE INDEX");
    static final TextKey TYPES = TextKey.of("jsc.terminal.upkeep.types", "TYPES");
    static final TextKey SERVERS = TextKey.of("jsc.terminal.upkeep.servers", "SERVERS");
    static final TextKey LOCKS = TextKey.of("jsc.terminal.upkeep.locks", "LOCKS");
    static final TextKey STORAGE = TextKey.of("jsc.terminal.upkeep.storage", "STORAGE");
    static final TextKey ACTIONS = TextKey.of("jsc.terminal.upkeep.actions", "ACTIONS");
    /* How the index stands when it is not all right. */
    static final TextKey STALE = TextKey.of("jsc.terminal.upkeep.stale", "STALE");
    static final TextKey FRAGMENTED = TextKey.of("jsc.terminal.upkeep.fragmented", "FRAGMENTED");
    /* That state, and how many item types are in doubt. */
    static final TextKey ONE_AFFECTED = TextKey.of("jsc.terminal.upkeep.one_affected", "%s - %s item type affected");
    static final TextKey AFFECTED = TextKey.of("jsc.terminal.upkeep.affected", "%s - %s item types affected");
    /* The run that settles it, which is data. */
    static final TextKey RUN = TextKey.of("jsc.terminal.upkeep.run", "run %s");
    static final TextKey DROP_DATA_BUTTON = TextKey.of("jsc.terminal.upkeep.drop_data", "DROP DATA...");

    // The Tasks heading.
    static final TextKey PROCESSES = TextKey.of("jsc.terminal.tasks.processes", "Processes");
    static final TextKey HARDWARE = TextKey.of("jsc.terminal.tasks.hardware", "Hardware");
    static final TextKey DEVICES = TextKey.of("jsc.terminal.tasks.devices", "Devices");
    static final TextKey IN_FLIGHT = TextKey.of("jsc.terminal.tasks.in_flight", "IN FLIGHT");
    static final TextKey PENDING = TextKey.of("jsc.terminal.tasks.pending", "PENDING");
    static final TextKey DONE = TextKey.of("jsc.terminal.tasks.done", "DONE");
    static final TextKey IN_PROGRESS = TextKey.of("jsc.terminal.tasks.in_progress", "IN PROGRESS");
    static final TextKey IDLE = TextKey.of("jsc.terminal.tasks.idle", "Idle - no Operations running.");
    static final TextKey MAINFRAME = TextKey.of("jsc.terminal.tasks.mainframe", "Mainframe");
    static final TextKey DEVICE_SERVERS = TextKey.of("jsc.terminal.tasks.servers", "Servers");
    static final TextKey PERSONAL_COMPUTERS = TextKey.of("jsc.terminal.tasks.personal_computers", "Personal Computers");
    static final TextKey SUBFRAMES = TextKey.of("jsc.terminal.tasks.subframes", "Subframes");
    static final TextKey NETWORK_STORAGE = TextKey.of("jsc.terminal.tasks.network_storage", "Network storage");

    // The Ops heading.
    static final TextKey OPERATIONS = TextKey.of("jsc.terminal.ops.operations", "OPERATIONS");
    static final TextKey LIVE_OF = TextKey.of("jsc.terminal.ops.live_of", "%s live / %s");
    static final TextKey NO_OPERATIONS = TextKey.of("jsc.terminal.ops.none", "No operations yet.");
    /* An uncapped request, which asks for all there is. */
    static final TextKey ALL = TextKey.of("jsc.terminal.ops.all", "all");
    /* How far an Operation has got, and how it stands. */
    static final TextKey MOVED_OF = TextKey.of("jsc.terminal.ops.moved_of", "%s of %s  %s");
    static final TextKey MORE_SOURCES = TextKey.of("jsc.terminal.ops.more_sources", "+%s more sources");
    static final TextKey MORE_STAGES = TextKey.of("jsc.terminal.ops.more_stages", "+%s more stages");

    // One Operation, opened.
    static final TextKey SUBOPERATIONS = TextKey.of("jsc.terminal.operation.suboperations", "SUBOPERATIONS");
    static final TextKey NO_MOVEMENT = TextKey.of("jsc.terminal.operation.no_movement", "No movement yet.");
    static final TextKey RIGHT_CLICK_TO_CLOSE =
            TextKey.of("jsc.terminal.operation.right_click_to_close", "right-click to close");
    static final TextKey READING = TextKey.of("jsc.terminal.operation.reading", "READING");
    static final TextKey STREAMING = TextKey.of("jsc.terminal.operation.streaming", "STREAMING");
    static final TextKey SUB_DONE = TextKey.of("jsc.terminal.operation.done", "DONE");
    static final TextKey QUEUED = TextKey.of("jsc.terminal.operation.queued", "QUEUED");

    // The Programs heading.
    static final TextKey SERVICES_AND_JOBS = TextKey.of("jsc.terminal.programs.services_and_jobs", "SERVICES & JOBS");
    static final TextKey NONE = TextKey.of("jsc.terminal.programs.none", "None");
    static final TextKey STOP = TextKey.of("jsc.terminal.programs.stop", "Stop");
    static final TextKey START = TextKey.of("jsc.terminal.programs.start", "Start");
    static final TextKey END = TextKey.of("jsc.terminal.programs.end", "End");
    static final TextKey RESTART = TextKey.of("jsc.terminal.programs.restart", "Restart");
    static final TextKey NO_PROCESSES = TextKey.of("jsc.terminal.programs.no_processes", "no processes running");

    // The Console heading; the command it names is data.
    static final TextKey START_HINT =
            TextKey.of("jsc.terminal.console.start_hint", "%s lists what this machine can run.");

    // The question about dropping data.
    static final TextKey DROP_DATA = TextKey.of("jsc.terminal.drop.title", "DROP DATA");
    static final TextKey IRREVERSIBLE = TextKey.of("jsc.terminal.drop.irreversible", "Irreversible data loss");
    static final TextKey SCOPE_NETWORK = TextKey.of("jsc.terminal.drop.scope_network", "NETWORK");
    static final TextKey SCOPE_SERVER = TextKey.of("jsc.terminal.drop.scope_server", "SERVER");
    static final TextKey SCOPE_TYPES = TextKey.of("jsc.terminal.drop.scope_types", "TYPES");
    static final TextKey DESTROYS_ALL = TextKey.of("jsc.terminal.drop.destroys_all",
            "Destroys ALL public storage on the entire network.");
    static final TextKey TYPES_OVER = TextKey.of("jsc.terminal.drop.types_over", "%s types over %s servers");
    static final TextKey CANCEL = TextKey.of("jsc.terminal.drop.cancel", "CANCEL");
    static final TextKey CONFIRM_DROP = TextKey.of("jsc.terminal.drop.confirm", "CONFIRM DROP");
    static final TextKey NO_SERVERS = TextKey.of("jsc.terminal.drop.no_servers", "No Servers on the network.");
    static final TextKey WIPES_SERVER = TextKey.of("jsc.terminal.drop.wipes_server", "Wipes this server's storage.");
    static final TextKey FREE_NOW = TextKey.of("jsc.terminal.drop.free_now", "%s free now");
    static final TextKey NO_TYPES = TextKey.of("jsc.terminal.drop.no_types", "No data types on the network.");
    static final TextKey SELECTED = TextKey.of("jsc.terminal.drop.selected", "%s of %s selected");

    private TerminalUpkeepTexts() {
    }
}
