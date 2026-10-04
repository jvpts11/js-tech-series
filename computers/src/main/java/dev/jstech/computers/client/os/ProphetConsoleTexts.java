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

/** What the Prophet Reactive Console's window says. */
@TextHolder
public final class ProphetConsoleTexts {

    public static final TextKey TITLE = TextKey.of("jsc.prophet_console.title", "Prophet Reactive Console");

    // The tabs.
    public static final TextKey STATES_TAB = TextKey.of("jsc.prophet_console.tab.states", "States");
    public static final TextKey SUBSCRIPTIONS_TAB = TextKey.of("jsc.prophet_console.tab.subscriptions",
            "Subscriptions");
    public static final TextKey REACTIONS_TAB = TextKey.of("jsc.prophet_console.tab.reactions", "Reactions");
    public static final TextKey SETTINGS_TAB = TextKey.of("jsc.prophet_console.tab.settings", "Settings");

    // The States tab.
    public static final TextKey DESIRED_STATE = TextKey.of("jsc.prophet_console.desired_state", "DESIRED STATE");
    public static final TextKey STATE = TextKey.of("jsc.prophet_console.state", "STATE");
    public static final TextKey NOW = TextKey.of("jsc.prophet_console.now", "NOW");
    public static final TextKey WORK = TextKey.of("jsc.prophet_console.work", "WORK");
    public static final TextKey LAST_REACTION = TextKey.of("jsc.prophet_console.last_reaction", "LAST REACTION");
    public static final TextKey IN_FLIGHT = TextKey.of("jsc.prophet_console.in_flight", "%s on the way");
    public static final TextKey NO_STATES = TextKey.of("jsc.prophet_console.no_states",
            "No states yet. Declare one below: KEEP item >= n");
    public static final TextKey WITH_WORK = TextKey.of("jsc.prophet_console.with_work", "with work on its way");
    public static final TextKey COUNTING = TextKey.of("jsc.prophet_console.counting",
            "Counting %s on the way: %s + %s = %s.");
    public static final TextKey NO_OPERATIONS = TextKey.of("jsc.prophet_console.no_operations",
            "Nothing set going for it yet.");
    public static final TextKey CHECK = TextKey.of("jsc.prophet_console.check", "Check");
    public static final TextKey APPLY = TextKey.of("jsc.prophet_console.apply", "Apply");

    // The Subscriptions tab.
    public static final TextKey FIRED_TIMES = TextKey.of("jsc.prophet_console.fired_times", "fired %s times");
    public static final TextKey NO_WATCHES = TextKey.of("jsc.prophet_console.no_watches",
            "No watches yet. Set one below: WATCH item < n DO CRAFT item TO m");
    public static final TextKey FORGET = TextKey.of("jsc.prophet_console.forget", "Forget");

    // The Reactions tab.
    public static final TextKey SECONDS_AGO = TextKey.of("jsc.prophet_console.seconds_ago", "%s s ago");
    public static final TextKey MINUTES_AGO = TextKey.of("jsc.prophet_console.minutes_ago", "%s min ago");
    public static final TextKey NO_REACTIONS = TextKey.of("jsc.prophet_console.no_reactions",
            "Nothing done yet: every state is where it should be.");

    // The Settings tab.
    public static final TextKey LOOK_EVERY = TextKey.of("jsc.prophet_console.look_every", "Look at the network every");
    public static final TextKey SECONDS = TextKey.of("jsc.prophet_console.seconds", "%s s");
    public static final TextKey AT_MOST = TextKey.of("jsc.prophet_console.at_most", "Ask for at most, in one craft");
    public static final TextKey REACT = TextKey.of("jsc.prophet_console.react", "React to changes");
    public static final TextKey REACTING = TextKey.of("jsc.prophet_console.reacting", "On");
    public static final TextKey PAUSED = TextKey.of("jsc.prophet_console.paused", "Paused");

    // The status bar.
    public static final TextKey STATES_COUNT = TextKey.of("jsc.prophet_console.states_count", "%s states");
    public static final TextKey REACTING_STATUS = TextKey.of("jsc.prophet_console.reacting_status",
            "reacting to changes");
    public static final TextKey PAUSED_STATUS = TextKey.of("jsc.prophet_console.paused_status", "reactions paused");
    public static final TextKey ASKING = TextKey.of("jsc.prophet_console.asking", "Asking the network");

    private ProphetConsoleTexts() {
    }
}
