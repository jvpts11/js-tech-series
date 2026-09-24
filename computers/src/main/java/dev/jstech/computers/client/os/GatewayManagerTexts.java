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

/**
 * What the Gateway Manager's window says, kept apart from the window so the language generator can read it on a
 * server too, where windows do not exist.
 */
@TextHolder
final class GatewayManagerTexts {

    // The tabs and the rail.
    static final TextKey STATUS_TAB = TextKey.of("jsc.gateway_manager.status_tab", "Status");
    static final TextKey PERMISSIONS_TAB = TextKey.of("jsc.gateway_manager.permissions_tab", "Permissions");
    static final TextKey COMPUTERS_TAB = TextKey.of("jsc.gateway_manager.computers_tab", "Computers");
    static final TextKey LOG_TAB = TextKey.of("jsc.gateway_manager.log_tab", "Log");
    static final TextKey ON_THIS_COMPUTER = TextKey.of("jsc.gateway_manager.on_this_computer", "ON THIS COMPUTER · %s");
    static final TextKey NONE_LINKED = TextKey.of("jsc.gateway_manager.none_linked", "none linked yet");
    static final TextKey RENAME = TextKey.of("jsc.gateway_manager.rename", "Rename");
    static final TextKey IDENTIFY = TextKey.of("jsc.gateway_manager.identify", "Identify");
    static final TextKey ASKING = TextKey.of("jsc.gateway_manager.asking", "Asking the computer ...");
    static final TextKey SELECT_ONE = TextKey.of("jsc.gateway_manager.select_one", "Select a Gateway on the left.");

    // The Status tab.
    static final TextKey THIS_SIDE = TextKey.of("jsc.gateway_manager.this_side", "THIS SIDE");
    static final TextKey COMPUTERCRAFT_SIDE = TextKey.of("jsc.gateway_manager.computercraft_side", "COMPUTERCRAFT SIDE");
    static final TextKey NOT_LINKED = TextKey.of("jsc.gateway_manager.not_linked", "Not linked");
    static final TextKey LINKED_TO = TextKey.of("jsc.gateway_manager.linked_to", "Linked to %s");
    static final TextKey TYPES_AND_SERVERS = TextKey.of("jsc.gateway_manager.types_and_servers", "%s, %s");
    static final TextKey ONE_TYPE = TextKey.of("jsc.gateway_manager.one_type", "%s type");
    static final TextKey TYPES = TextKey.of("jsc.gateway_manager.types", "%s types");
    static final TextKey ONE_SERVER = TextKey.of("jsc.gateway_manager.one_server", "%s server");
    static final TextKey SERVERS = TextKey.of("jsc.gateway_manager.servers", "%s servers");
    static final TextKey MAINFRAME_ONLINE = TextKey.of("jsc.gateway_manager.mainframe_online", "Mainframe online");
    static final TextKey MAINFRAME_OFFLINE = TextKey.of("jsc.gateway_manager.mainframe_offline", "Mainframe offline");
    static final TextKey BUDGET = TextKey.of("jsc.gateway_manager.budget", "Budget: %s%% of a tick");
    static final TextKey CC_VERSION = TextKey.of("jsc.gateway_manager.cc_version", "CC: Tweaked %s");
    static final TextKey CC_MISSING = TextKey.of("jsc.gateway_manager.cc_missing", "CC: Tweaked is not installed");
    static final TextKey COMPUTERS_AND_DEVICES = TextKey.of("jsc.gateway_manager.computers_and_devices", "%s, %s");
    static final TextKey ONE_COMPUTER = TextKey.of("jsc.gateway_manager.one_computer", "%s computer");
    static final TextKey COMPUTERS = TextKey.of("jsc.gateway_manager.computers", "%s computers");
    static final TextKey ONE_DEVICE = TextKey.of("jsc.gateway_manager.one_device", "%s device");
    static final TextKey DEVICES = TextKey.of("jsc.gateway_manager.devices", "%s devices");
    static final TextKey SERVED_ONE = TextKey.of("jsc.gateway_manager.served_one", "Served: %s call");
    static final TextKey SERVED = TextKey.of("jsc.gateway_manager.served", "Served: %s calls");
    static final TextKey ONE_OPERATION = TextKey.of("jsc.gateway_manager.one_operation", "%s operation this minute");
    static final TextKey OPERATIONS = TextKey.of("jsc.gateway_manager.operations", "%s operations this minute");
    static final TextKey NAMES = TextKey.of("jsc.gateway_manager.names", "On CC: %s · rednet: %s");
    static final TextKey NONE_YET = TextKey.of("jsc.gateway_manager.none_yet", "none yet");
    static final TextKey BUFFER = TextKey.of("jsc.gateway_manager.buffer", "BUFFER %s of %s");
    static final TextKey CLEAR_BUFFER = TextKey.of("jsc.gateway_manager.clear_buffer", "Clear buffer to network");
    static final TextKey RECENT = TextKey.of("jsc.gateway_manager.recent", "RECENT");
    static final TextKey NOTHING_SERVED = TextKey.of("jsc.gateway_manager.nothing_served", "nothing served yet");
    static final TextKey OPEN_LOG = TextKey.of("jsc.gateway_manager.open_log", "Open the log");
    static final TextKey OPEN_NETWORK = TextKey.of("jsc.gateway_manager.open_network", "Open Network");
    static final TextKey BUFFER_SLOT = TextKey.of("jsc.gateway_manager.buffer_slot", "%s x %s");

    // The Permissions tab.
    static final TextKey READ = TextKey.of("jsc.gateway_manager.read", "Read the network: types, totals, servers, watches");
    static final TextKey OPERATIONS_ALLOWED =
            TextKey.of("jsc.gateway_manager.operations_allowed", "Operations: pull, push, craft, cancel, run");
    static final TextKey CEILING = TextKey.of("jsc.gateway_manager.ceiling", "Priority ceiling for CC requests");
    static final TextKey CAP = TextKey.of("jsc.gateway_manager.cap", "Calls per tick from CC, paid from this budget");
    static final TextKey LOW = TextKey.of("jsc.gateway_manager.low", "LOW");
    static final TextKey MEDIUM = TextKey.of("jsc.gateway_manager.medium", "MEDIUM");
    static final TextKey HIGH = TextKey.of("jsc.gateway_manager.high", "HIGH");

    // The Computers tab.
    static final TextKey ID_COLUMN = TextKey.of("jsc.gateway_manager.id_column", "ID");
    static final TextKey LABEL_COLUMN = TextKey.of("jsc.gateway_manager.label_column", "LABEL");
    static final TextKey STATE_COLUMN = TextKey.of("jsc.gateway_manager.state_column", "STATE");
    static final TextKey AGENT_COLUMN = TextKey.of("jsc.gateway_manager.agent_column", "AGENT");
    static final TextKey LAST_SEEN_COLUMN = TextKey.of("jsc.gateway_manager.last_seen_column", "LAST SEEN");
    static final TextKey NO_COMPUTER = TextKey.of("jsc.gateway_manager.no_computer",
            "no ComputerCraft computer is attached to this Gateway");
    static final TextKey TURN_ON = TextKey.of("jsc.gateway_manager.turn_on", "Turn on");
    static final TextKey REBOOT = TextKey.of("jsc.gateway_manager.reboot", "Reboot");
    static final TextKey SHUTDOWN = TextKey.of("jsc.gateway_manager.shutdown", "Shutdown");
    static final TextKey TEST_EVENT = TextKey.of("jsc.gateway_manager.test_event", "Test event");
    static final TextKey NO_LABEL = TextKey.of("jsc.gateway_manager.no_label", "(no label)");
    static final TextKey ON = TextKey.of("jsc.gateway_manager.on", "on");
    static final TextKey OFF = TextKey.of("jsc.gateway_manager.off", "off");
    static final TextKey ANSWERING = TextKey.of("jsc.gateway_manager.answering", "answering");
    static final TextKey NONE = TextKey.of("jsc.gateway_manager.none", "none");
    /* The widest time a row can show, so the column is wide enough for it in any language. */
    static final TextKey WIDEST_AGO = TextKey.of("jsc.gateway_manager.widest_ago", "00 min ago");

    // The Log tab.
    static final TextKey WHEN_COLUMN = TextKey.of("jsc.gateway_manager.when_column", "WHEN");
    static final TextKey WHO_COLUMN = TextKey.of("jsc.gateway_manager.who_column", "WHO");
    static final TextKey RESULT_COLUMN = TextKey.of("jsc.gateway_manager.result_column", "RESULT");
    static final TextKey NOTHING_DONE = TextKey.of("jsc.gateway_manager.nothing_done", "this Gateway has done nothing yet");
    /* The longest result the Gateway writes, so the result column is wide enough for it in any language. */
    static final TextKey WIDEST_RESULT = TextKey.of("jsc.gateway_manager.widest_result", "40 items in 12 operations");

    // The rename dialog.
    static final TextKey RENAME_TITLE = TextKey.of("jsc.gateway_manager.rename_title", "RENAME GATEWAY");
    static final TextKey RENAME_HINT = TextKey.of("jsc.gateway_manager.rename_hint",
            "Letters, digits, dashes; empty goes back to the default.");
    static final TextKey APPLY = TextKey.of("jsc.gateway_manager.apply", "APPLY");
    static final TextKey CANCEL = TextKey.of("jsc.gateway_manager.cancel", "CANCEL");

    // The status bar.
    static final TextKey FLEET_ONE = TextKey.of("jsc.gateway_manager.fleet_one", "%s gateway, %s with CC reachable");
    static final TextKey FLEET = TextKey.of("jsc.gateway_manager.fleet", "%s gateways, %s with CC reachable");
    static final TextKey LOAD_ONE = TextKey.of("jsc.gateway_manager.load_one", "%s call this minute");
    static final TextKey LOAD = TextKey.of("jsc.gateway_manager.load", "%s calls this minute");
    static final TextKey NOTHING_SELECTED = TextKey.of("jsc.gateway_manager.nothing_selected", "nothing selected");
    static final TextKey SELECTED = TextKey.of("jsc.gateway_manager.selected", "%s selected");

    private GatewayManagerTexts() {
    }
}
