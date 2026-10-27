/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the Machine Monitor's window. */
@TextHolder
public final class MachineMonitorTexts {

    public static final TextKey MENU_VIEW = TextKey.of("jsindustrial.machine_monitor.menu.view", "View");
    public static final TextKey MENU_ALERTS = TextKey.of("jsindustrial.machine_monitor.menu.alerts", "Alerts");
    public static final TextKey ACKNOWLEDGE = TextKey.of("jsindustrial.machine_monitor.acknowledge", "Acknowledge");
    public static final TextKey ACKNOWLEDGE_ALL = TextKey.of("jsindustrial.machine_monitor.acknowledge_all",
            "Acknowledge all");
    public static final TextKey SHOW = TextKey.of("jsindustrial.machine_monitor.show", "Show:");
    public static final TextKey ALL_ALERTS = TextKey.of("jsindustrial.machine_monitor.all_alerts", "All alerts");
    public static final TextKey SEARCH = TextKey.of("jsindustrial.machine_monitor.search", "Search machines");
    public static final TextKey COUNTED = TextKey.of("jsindustrial.machine_monitor.counted", "%s %s");
    public static final TextKey TIME = TextKey.of("jsindustrial.machine_monitor.time", "Time");
    public static final TextKey MACHINE = TextKey.of("jsindustrial.machine_monitor.machine", "Machine");
    public static final TextKey WHERE = TextKey.of("jsindustrial.machine_monitor.where", "Where");
    public static final TextKey ALERT = TextKey.of("jsindustrial.machine_monitor.alert", "Alert");
    public static final TextKey READING = TextKey.of("jsindustrial.machine_monitor.reading", "Reading");
    public static final TextKey SINCE = TextKey.of("jsindustrial.machine_monitor.since", "%s since %s");
    public static final TextKey OVER = TextKey.of("jsindustrial.machine_monitor.over", "%s at %s, over");
    public static final TextKey AT = TextKey.of("jsindustrial.machine_monitor.at", "At %s");
    public static final TextKey TEMPERATURE = TextKey.of("jsindustrial.machine_monitor.temperature",
            "Temperature %s °C");
    public static final TextKey LIMIT = TextKey.of("jsindustrial.machine_monitor.limit", "Its limit %s °C");
    public static final TextKey STATE = TextKey.of("jsindustrial.machine_monitor.state", "State: %s");
    public static final TextKey POWER = TextKey.of("jsindustrial.machine_monitor.power", "Power %s %sW");
    public static final TextKey POWER_PORT = TextKey.of("jsindustrial.machine_monitor.power_port",
            "Power %s %sW, %s port");
    public static final TextKey READ_BY = TextKey.of("jsindustrial.machine_monitor.read_by", "Read by %s");
    public static final TextKey NO_ALERTS = TextKey.of("jsindustrial.machine_monitor.no_alerts",
            "No alerts: every machine read is well");
    public static final TextKey READING_NETWORK = TextKey.of("jsindustrial.machine_monitor.reading_network",
            "Reading the network...");
    public static final TextKey CHOOSE = TextKey.of("jsindustrial.machine_monitor.choose",
            "Choose an alert to see its machine");
    public static final TextKey ALERTS_COUNT = TextKey.of("jsindustrial.machine_monitor.alerts_count",
            "%s alerts, %s not acknowledged");
    public static final TextKey READ_COUNT = TextKey.of("jsindustrial.machine_monitor.read_count",
            "%s controllers, %s machines read");
    public static final TextKey READ_AT = TextKey.of("jsindustrial.machine_monitor.read_at", "Read %s");
    public static final TextKey STATUS_LINE = TextKey.of("jsindustrial.machine_monitor.status_line",
            "%s  |  %s  |  %s");

    private MachineMonitorTexts() {
    }
}
