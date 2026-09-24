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
 * What the headings and the questions of a network machine's space say, kept apart from the screen that draws them
 * so the language generator can read them on a server too, where screens do not exist.
 */
@TextHolder
final class TerminalTexts {

    // The rail's headings.
    static final TextKey TAB_LOCAL = TextKey.of("jsc.terminal.tab.local", "Local");
    static final TextKey TAB_STORAGE = TextKey.of("jsc.terminal.tab.storage", "Storage");
    static final TextKey TAB_NETWORK = TextKey.of("jsc.terminal.tab.network", "Network");
    static final TextKey TAB_OPS = TextKey.of("jsc.terminal.tab.ops", "Ops");
    static final TextKey TAB_TASKS = TextKey.of("jsc.terminal.tab.tasks", "Tasks");
    static final TextKey TAB_UPKEEP = TextKey.of("jsc.terminal.tab.upkeep", "Upkeep");
    static final TextKey TAB_CRAFT = TextKey.of("jsc.terminal.tab.craft", "Craft");
    static final TextKey TAB_PROGRAMS = TextKey.of("jsc.terminal.tab.programs", "Programs");
    static final TextKey TAB_CONSOLE = TextKey.of("jsc.terminal.tab.console", "Console");
    static final TextKey TAB_PATTERNS = TextKey.of("jsc.terminal.tab.patterns", "Patterns");

    // The screen round the headings.
    static final TextKey SEARCH = TextKey.of("jsc.terminal.search", "Search");
    static final TextKey SEARCH_HINT = TextKey.of("jsc.terminal.search_hint", "Search items...");
    static final TextKey NOT_AVAILABLE = TextKey.of("jsc.terminal.not_available", "Not available yet");
    static final TextKey YOUR_INVENTORY = TextKey.of("jsc.terminal.your_inventory", "Your inventory");
    static final TextKey NETWORK_CONFLICT = TextKey.of("jsc.terminal.network_conflict", "network conflict");
    static final TextKey HELD = TextKey.of("jsc.terminal.held", "%s held");
    static final TextKey NO_NETWORK = TextKey.of("jsc.terminal.no_network", "no network");
    static final TextKey BUILD_INVALID = TextKey.of("jsc.terminal.build_invalid", "build invalid");
    static final TextKey HALTED = TextKey.of("jsc.terminal.halted", "halted");
    static final TextKey IDLE = TextKey.of("jsc.terminal.idle", "idle");
    static final TextKey ONE_OP = TextKey.of("jsc.terminal.one_op", "%s op");
    static final TextKey OPS = TextKey.of("jsc.terminal.ops", "%s ops");
    /* A maintenance run the player asked for, which the Operations list now carries; the run is data. */
    static final TextKey LOGGED = TextKey.of("jsc.terminal.logged", "%s logged");

    // How an Operation stands.
    static final TextKey STATUS_COMPLETED = TextKey.of("jsc.terminal.status.completed", "COMPLETED");
    static final TextKey STATUS_PARTIAL = TextKey.of("jsc.terminal.status.partial", "PARTIAL");
    static final TextKey STATUS_PROCESSING = TextKey.of("jsc.terminal.status.processing", "PROCESSING");
    static final TextKey STATUS_WAITING = TextKey.of("jsc.terminal.status.waiting", "WAITING");
    static final TextKey STATUS_RESOURCE_LOCKED =
            TextKey.of("jsc.terminal.status.resource_locked", "RESOURCE LOCKED");
    static final TextKey STATUS_PENDING = TextKey.of("jsc.terminal.status.pending", "PENDING");
    static final TextKey STATUS_FAILED = TextKey.of("jsc.terminal.status.failed", "FAILED");

    // The grid's tooltips.
    static final TextKey IN_STORAGE = TextKey.of("jsc.terminal.tip.in_storage", "%s in storage");
    static final TextKey ON_THE_NETWORK = TextKey.of("jsc.terminal.tip.on_the_network", "%s on the network");
    static final TextKey DEPOSIT_LOCAL = TextKey.of("jsc.terminal.tip.deposit_local", "Deposit into local storage");
    static final TextKey DEPOSIT_NETWORK = TextKey.of("jsc.terminal.tip.deposit_network", "Deposit into the network");
    static final TextKey DEPOSIT_CLICK = TextKey.of("jsc.terminal.tip.deposit_click", "Click: deposit held stack");
    static final TextKey DEPOSIT_RIGHT_CLICK =
            TextKey.of("jsc.terminal.tip.deposit_right_click", "Right-click: deposit one");
    static final TextKey DEPOSIT_SHIFT_CLICK =
            TextKey.of("jsc.terminal.tip.deposit_shift_click", "Shift-click an inventory item");
    static final TextKey TAKE_CLICK = TextKey.of("jsc.terminal.tip.take_click", "Click: take a stack");
    static final TextKey TAKE_SHIFT_CLICK =
            TextKey.of("jsc.terminal.tip.take_shift_click", "Shift-click: take all  -  Right-click: take one");

    // The Craft heading.
    static final TextKey CRAFTABLE = TextKey.of("jsc.terminal.craft.craftable", "CRAFTABLE");
    static final TextKey ONE_PATTERN = TextKey.of("jsc.terminal.craft.one_pattern", "%s pattern");
    static final TextKey PATTERNS = TextKey.of("jsc.terminal.craft.patterns", "%s patterns");
    static final TextKey NO_PATTERNS = TextKey.of("jsc.terminal.craft.no_patterns", "no patterns loaded");
    static final TextKey LOAD_UNDER_PATTERNS =
            TextKey.of("jsc.terminal.craft.load_under_patterns", "load one from a medium under Patterns");
    static final TextKey RUNNING = TextKey.of("jsc.terminal.craft.running", "RUNNING");
    static final TextKey NOTHING_BEING_MADE = TextKey.of("jsc.terminal.craft.nothing_being_made", "nothing being made");
    static final TextKey JUST_MADE = TextKey.of("jsc.terminal.craft.just_made", "JUST MADE");
    static final TextKey NOTHING_YET = TextKey.of("jsc.terminal.craft.nothing_yet", "nothing yet");
    static final TextKey DONE = TextKey.of("jsc.terminal.craft.done", "done");
    static final TextKey PARTIAL = TextKey.of("jsc.terminal.craft.partial", "partial");
    static final TextKey LOCKED = TextKey.of("jsc.terminal.craft.locked", "locked");
    static final TextKey DROPPED = TextKey.of("jsc.terminal.craft.dropped", "dropped");
    static final TextKey FAILED = TextKey.of("jsc.terminal.craft.failed", "failed");
    /* A finished craft: what it made, and how many. */
    static final TextKey MADE = TextKey.of("jsc.terminal.craft.made", "%s x%s");

    // The craft question.
    static final TextKey CRAFT_TITLE = TextKey.of("jsc.terminal.craft_popup.title", "CRAFT  %s");
    static final TextKey PLAN_RAW = TextKey.of("jsc.terminal.craft_popup.plan_raw", "PLAN - raw ingredients");
    static final TextKey PLANNING = TextKey.of("jsc.terminal.craft_popup.planning", "planning...");
    static final TextKey MORE = TextKey.of("jsc.terminal.craft_popup.more", "+%s more");
    static final TextKey ESTIMATE = TextKey.of("jsc.terminal.craft_popup.estimate", "EST ~%ss");
    static final TextKey NO_ESTIMATE = TextKey.of("jsc.terminal.craft_popup.no_estimate", "EST --");
    static final TextKey MAX_NOW = TextKey.of("jsc.terminal.craft_popup.max_now", "max now: %s");
    static final TextKey CRAFT = TextKey.of("jsc.terminal.craft_popup.craft", "CRAFT");
    static final TextKey PARTIAL_BUTTON = TextKey.of("jsc.terminal.craft_popup.partial", "PARTIAL");
    static final TextKey CLOSE = TextKey.of("jsc.terminal.craft_popup.close", "CLOSE");

    private TerminalTexts() {
    }
}
