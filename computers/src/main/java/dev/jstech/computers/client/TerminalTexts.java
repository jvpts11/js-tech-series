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
