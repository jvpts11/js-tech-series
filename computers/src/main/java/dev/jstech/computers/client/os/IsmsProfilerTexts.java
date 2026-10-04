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

/** What the IQL Server Profiler's window says. */
@TextHolder
final class IsmsProfilerTexts {

    static final TextKey TITLE = TextKey.of("jsc.isms_profiler.title", "%s - [%s (%s)]");
    static final TextKey UNTITLED = TextKey.of("jsc.isms_profiler.untitled", "Untitled - %s");
    static final TextKey REPLAY = TextKey.of("jsc.isms_profiler.replay", "Replay");
    static final TextKey REPLAY_STATEMENT = TextKey.of("jsc.isms_profiler.replay_statement", "Replay Statement");
    static final TextKey NEW_TRACE = TextKey.of("jsc.isms_profiler.new_trace", "New Trace");
    static final TextKey PAUSE = TextKey.of("jsc.isms_profiler.pause", "Pause");
    static final TextKey CLEAR = TextKey.of("jsc.isms_profiler.clear", "Clear");
    static final TextKey CLEAR_WINDOW = TextKey.of("jsc.isms_profiler.clear_window", "Clear Trace Window");
    static final TextKey FIND = TextKey.of("jsc.isms_profiler.find", "Find");
    static final TextKey FIND_NEXT = TextKey.of("jsc.isms_profiler.find_next", "Find Next");
    static final TextKey EVENTS = TextKey.of("jsc.isms_profiler.events", "Events: %s");
    static final TextKey EVENTS_SELECTION = TextKey.of("jsc.isms_profiler.events_selection", "Events Selection");
    static final TextKey NO_GROUPS = TextKey.of("jsc.isms_profiler.no_groups", "none");
    static final TextKey GROUP_ON = TextKey.of("jsc.isms_profiler.group_on", "%s (on)");
    static final TextKey GROUP_OFF = TextKey.of("jsc.isms_profiler.group_off", "%s (off)");
    static final TextKey STATEMENTS = TextKey.of("jsc.isms_profiler.statements", "Statements");
    static final TextKey OPERATIONS = TextKey.of("jsc.isms_profiler.operations", "Operations");
    static final TextKey LOCKS = TextKey.of("jsc.isms_profiler.locks", "Locks");
    static final TextKey PLANS = TextKey.of("jsc.isms_profiler.plans", "Plans");
    static final TextKey BUSES = TextKey.of("jsc.isms_profiler.buses", "Buses");
    static final TextKey EVENT_CLASS = TextKey.of("jsc.isms_profiler.event_class", "EventClass");
    static final TextKey TEXT_DATA = TextKey.of("jsc.isms_profiler.text_data", "TextData");
    static final TextKey REQUESTER = TextKey.of("jsc.isms_profiler.requester", "Requester");
    static final TextKey COMPUTER = TextKey.of("jsc.isms_profiler.computer", "Computer");
    static final TextKey ITEMS = TextKey.of("jsc.isms_profiler.items", "Items");
    static final TextKey DURATION = TextKey.of("jsc.isms_profiler.duration", "Duration");
    static final TextKey START_TIME = TextKey.of("jsc.isms_profiler.start_time", "StartTime");
    static final TextKey RUNNING = TextKey.of("jsc.isms_profiler.running", "Trace is running.");
    static final TextKey PAUSED = TextKey.of("jsc.isms_profiler.paused", "Trace is paused.");
    static final TextKey STOPPED = TextKey.of("jsc.isms_profiler.stopped", "Trace is stopped.");
    static final TextKey ROW_COUNT = TextKey.of("jsc.isms_profiler.row_count", "Rows: %s");
    static final TextKey LINE = TextKey.of("jsc.isms_profiler.line", "Ln %s, Col %s");
    static final TextKey TICKS = TextKey.of("jsc.isms_profiler.ticks", "%s t");
    static final TextKey ASKED_BY = TextKey.of("jsc.isms_profiler.asked_by", "asked by: %s on %s");
    static final TextKey TOOK = TextKey.of("jsc.isms_profiler.took", "took %s ticks (%s s)");
    static final TextKey NO_EVENTS = TextKey.of("jsc.isms_profiler.no_events",
            "No events yet. Start the trace to record what the network does.");
    static final TextKey SAVED = TextKey.of("jsc.isms_profiler.saved", "The trace was saved to %s.");
    static final TextKey NOT_FOUND = TextKey.of("jsc.isms_profiler.not_found", "Nothing more holds %s.");

    private IsmsProfilerTexts() {
    }
}
