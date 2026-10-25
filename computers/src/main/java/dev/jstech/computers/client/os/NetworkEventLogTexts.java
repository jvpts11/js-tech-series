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

/** The words of the Network Manager's event log: the switch, the filters, the columns, the count and Export. */
@TextHolder
final class NetworkEventLogTexts {

    static final TextKey OPERATIONS = TextKey.of("jsc.event_log.view.operations", "Operations");
    static final TextKey EVENTS = TextKey.of("jsc.event_log.view.events", "Events");
    static final TextKey ALL_LEVELS = TextKey.of("jsc.event_log.filter.all_levels", "All levels");
    static final TextKey ALL_MACHINES = TextKey.of("jsc.event_log.filter.all_machines", "All machines");
    static final TextKey LAST_HOUR = TextKey.of("jsc.event_log.filter.last_hour", "Last hour");
    static final TextKey LAST_DAY = TextKey.of("jsc.event_log.filter.last_day", "Last day");
    static final TextKey LAST_WEEK = TextKey.of("jsc.event_log.filter.last_week", "Last 7 days");
    static final TextKey ALL_KEPT = TextKey.of("jsc.event_log.filter.all_kept", "All kept");
    static final TextKey TIME = TextKey.of("jsc.event_log.column.time", "TIME");
    static final TextKey LEVEL = TextKey.of("jsc.event_log.column.level", "LEVEL");
    static final TextKey MACHINE = TextKey.of("jsc.event_log.column.machine", "MACHINE");
    static final TextKey EVENT = TextKey.of("jsc.event_log.column.event", "EVENT");
    static final TextKey DAY = TextKey.of("jsc.event_log.day", "Day %s");
    static final TextKey COUNT = TextKey.of("jsc.event_log.count", "%s events");
    static final TextKey ONE = TextKey.of("jsc.event_log.count.one", "1 event");
    static final TextKey NONE = TextKey.of("jsc.event_log.none", "Nothing written down in this period");
    static final TextKey EXPORT = TextKey.of("jsc.event_log.export", "Export");
    static final TextKey EXPORT_TITLE = TextKey.of("jsc.event_log.export.title", "Export the event log");
    static final TextKey EXPORTED = TextKey.of("jsc.event_log.exported", "Exported to %s");
    static final TextKey TEXT_FILES = TextKey.of("jsc.event_log.text_files", "Text files");
    static final TextKey PRINT_TITLE = TextKey.of("jsc.event_log.print.title", "Network event log");

    private NetworkEventLogTexts() {
    }
}
