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
 * What the Network Manager's Services tab says: the engine's card, the lists under it, the dialog that replaces or
 * starts an engine and the steps a replacement goes through. Engine, vendor and machine names and versions are data.
 * Kept apart from the window so the language generator can read it on a server too, where windows do not exist.
 */
@TextHolder
final class NetworkServicesTexts {

    static final TextKey TAB = TextKey.of("jsc.network_services.tab", "Services");

    // The engine's card.
    static final TextKey RUNNING = TextKey.of("jsc.network_services.running", "Running");
    static final TextKey STOPPED = TextKey.of("jsc.network_services.stopped", "Stopped");
    static final TextKey REPLACING = TextKey.of("jsc.network_services.replacing", "Replacing");
    static final TextKey ON_HOST = TextKey.of("jsc.network_services.on_host", "on %s");
    static final TextKey CONFIGURE = TextKey.of("jsc.network_services.configure", "Configure...");
    static final TextKey STOP = TextKey.of("jsc.network_services.stop", "Stop");
    static final TextKey START = TextKey.of("jsc.network_services.start", "Start...");
    static final TextKey REPLACE = TextKey.of("jsc.network_services.replace", "Replace...");
    static final TextKey DIALECT = TextKey.of("jsc.network_services.dialect", "Dialect");
    static final TextKey MEMORY = TextKey.of("jsc.network_services.memory", "Memory");
    static final TextKey MEGABYTES = TextKey.of("jsc.network_services.megabytes", "%s MB");
    static final TextKey UP = TextKey.of("jsc.network_services.up", "Up");
    static final TextKey PLANS_TODAY = TextKey.of("jsc.network_services.plans_today", "Plans today");
    static final TextKey INDEXES = TextKey.of("jsc.network_services.indexes", "Indexes");
    static final TextKey INDEXES_VALUE =
            TextKey.of("jsc.network_services.indexes_value", "%s item types on %s server(s)");
    static final TextKey CAPABILITIES = TextKey.of("jsc.network_services.capabilities", "Capabilities");
    /** What every engine does, before the extras it offers. */
    static final TextKey EVERY_ENGINE =
            TextKey.of("jsc.network_services.every_engine", "transfers, crafts, queries");
    static final TextKey PROCEDURES_AND_VIEWS =
            TextKey.of("jsc.network_services.capability.procedures_and_views", "procedures, views");
    static final TextKey DECLARATIVE_STATE =
            TextKey.of("jsc.network_services.capability.declarative_state", "declared states");
    static final TextKey SUBSCRIPTIONS =
            TextKey.of("jsc.network_services.capability.subscriptions", "subscriptions");
    static final TextKey EXPLAIN = TextKey.of("jsc.network_services.capability.explain", "explain");
    static final TextKey PLANNER_HINTS =
            TextKey.of("jsc.network_services.capability.planner_hints", "planner hints");
    static final TextKey EXTENSIONS = TextKey.of("jsc.network_services.capability.extensions", "extensions");
    static final TextKey DAYS_HOURS = TextKey.of("jsc.network_services.days_hours", "%s d %s h");
    static final TextKey HOURS_MINUTES = TextKey.of("jsc.network_services.hours_minutes", "%s h %s min");
    static final TextKey MINUTES = TextKey.of("jsc.network_services.minutes", "%s min");

    // With no engine running.
    static final TextKey NONE_RUNNING =
            TextKey.of("jsc.network_services.none_running", "No Network Operations Engine is running");
    static final TextKey NONE_NOTE = TextKey.of("jsc.network_services.none_note",
            "Storage stays, Operations in flight carry on, reads and direct transfers work: browse, deposit, "
                    + "withdraw. Crafting, planned moves and queries are unavailable until an engine runs.");
    static final TextKey NONE_INSTALLED = TextKey.of("jsc.network_services.none_installed",
            "No engine is installed on this Mainframe. Install one from its disc to plan the network's work.");

    // The lists.
    static final TextKey INSTALLED_ON = TextKey.of("jsc.network_services.installed_on", "INSTALLED ON %s");
    static final TextKey ENGINE_COLUMN = TextKey.of("jsc.network_services.engine_column", "ENGINE");
    static final TextKey VENDOR_COLUMN = TextKey.of("jsc.network_services.vendor_column", "VENDOR");
    static final TextKey VERSION_COLUMN = TextKey.of("jsc.network_services.version_column", "VERSION");
    static final TextKey STATE_COLUMN = TextKey.of("jsc.network_services.state_column", "STATE");
    static final TextKey ROW_ACTIVE = TextKey.of("jsc.network_services.row_active", "active");
    static final TextKey ROW_STOPPED = TextKey.of("jsc.network_services.row_stopped", "stopped");
    static final TextKey ROW_INSTALLED = TextKey.of("jsc.network_services.row_installed", "installed");
    static final TextKey ROW_STARTING = TextKey.of("jsc.network_services.row_starting", "starting");
    static final TextKey SUBFRAMES = TextKey.of("jsc.network_services.subframes", "SUBFRAMES");
    static final TextKey NO_SUBFRAMES =
            TextKey.of("jsc.network_services.no_subframes", "No Subframes on this network.");
    static final TextKey FOLLOWS_MAINFRAME =
            TextKey.of("jsc.network_services.follows_mainframe", "whatever %s runs");
    static final TextKey MATCHES = TextKey.of("jsc.network_services.matches", "matches");
    static final TextKey TAKES_NO_WORK = TextKey.of("jsc.network_services.takes_no_work", "takes no work");
    static final TextKey MISMATCH_NOTE = TextKey.of("jsc.network_services.mismatch_note",
            "%s runs another engine than %s: it takes no SubOperations until it runs %s, or %s switches to %s.");
    static final TextKey MISMATCH_NOTE_NONE = TextKey.of("jsc.network_services.mismatch_note_none",
            "%s runs %s, and %s runs no engine: it takes no SubOperations until the two run the same one.");
    static final TextKey OTHER_SERVICES = TextKey.of("jsc.network_services.other_services", "OTHER SERVICES");
    static final TextKey SERVICE_RUNNING = TextKey.of("jsc.network_services.service_running", "running");
    static final TextKey SERVICE_ABSENT = TextKey.of("jsc.network_services.service_absent", "not installed");

    // The dialog that replaces or starts an engine.
    static final TextKey REPLACE_TITLE =
            TextKey.of("jsc.network_services.replace_title", "Replace the Network Operations Engine");
    static final TextKey START_TITLE =
            TextKey.of("jsc.network_services.start_title", "Start a Network Operations Engine");
    static final TextKey OPTION_ACTIVE = TextKey.of("jsc.network_services.option_active", "(active)");
    static final TextKey OPTION_INSTALLED = TextKey.of("jsc.network_services.option_installed", "- installed");
    static final TextKey OPTION_STOPPED = TextKey.of("jsc.network_services.option_stopped", "- stopped");
    static final TextKey REPLACE_NEW_REQUESTS =
            TextKey.of("jsc.network_services.replace_new_requests", "New requests go to %s.");
    static final TextKey REPLACE_IN_FLIGHT = TextKey.of("jsc.network_services.replace_in_flight",
            "The %s Operations in flight finish on the plans they started with.");
    static final TextKey REPLACE_SCRIPTS = TextKey.of("jsc.network_services.replace_scripts",
            "Scripts written in %s's own statements may stop working; the IQL core works on every engine.");
    static final TextKey REPLACE_SUBFRAMES = TextKey.of("jsc.network_services.replace_subframes",
            "%s will take no work until it runs %s too.");
    static final TextKey REPLACE_NOTHING_ELSE = TextKey.of("jsc.network_services.replace_nothing_else",
            "No other engine is installed on this Mainframe. Install one from its disc to replace this one.");
    static final TextKey START_NOTE = TextKey.of("jsc.network_services.start_note",
            "The engine starts at once and plans every new request.");
    static final TextKey REPLACE_BUTTON = TextKey.of("jsc.network_services.replace_button", "Replace");
    static final TextKey START_BUTTON = TextKey.of("jsc.network_services.start_button", "Start");
    static final TextKey CANCEL = TextKey.of("jsc.network_services.cancel", "Cancel");

    // A replacement under way.
    static final TextKey REPLACING_TITLE = TextKey.of("jsc.network_services.replacing_title", "Replacing: %s -> %s");
    static final TextKey REPLACING_FROM_NONE =
            TextKey.of("jsc.network_services.replacing_from_none", "Starting: %s");
    static final TextKey STEP_STOP_NEW_PLANS =
            TextKey.of("jsc.network_services.step.stop_new_plans", "Stop taking new plans");
    static final TextKey STEP_KEEP_IN_FLIGHT = TextKey.of("jsc.network_services.step.keep_in_flight",
            "Keep %s Operations in flight on their plans");
    static final TextKey STEP_STOP_OLD = TextKey.of("jsc.network_services.step.stop_old", "Stop %s");
    static final TextKey STEP_STOP_NONE =
            TextKey.of("jsc.network_services.step.stop_none", "Nothing to stop");
    static final TextKey STEP_START_NEW = TextKey.of("jsc.network_services.step.start_new", "Start %s");
    static final TextKey STEP_DISCOVER =
            TextKey.of("jsc.network_services.step.discover", "Discover storage and machines");
    static final TextKey STEP_BUILD_INDEXES =
            TextKey.of("jsc.network_services.step.build_indexes", "Build indexes");
    static final TextKey STEP_PUBLISH = TextKey.of("jsc.network_services.step.publish", "Publish capabilities");
    static final TextKey STEP_READY = TextKey.of("jsc.network_services.step.ready", "Ready");
    static final TextKey ABOUT_SECONDS = TextKey.of("jsc.network_services.about_seconds",
            "About %s seconds: %s item types to index");

    private NetworkServicesTexts() {
    }
}
