/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.prophet;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What Prophet YourIQL says: of the states it keeps, its watches and what it did for them. */
@TextHolder
public final class ProphetTexts {

    // What a statement answers.
    public static final TextKey KEPT = TextKey.of("jsc.prophet.said.kept", "%s kept at %s or more");
    public static final TextKey KEPT_BAND = TextKey.of("jsc.prophet.said.kept_band", "%s kept between %s and %s");
    public static final TextKey WATCHING = TextKey.of("jsc.prophet.said.watching", "watch %s set: %s");
    public static final TextKey FORGOTTEN = TextKey.of("jsc.prophet.said.forgotten", "%s is no longer kept");
    public static final TextKey NOT_KEPT = TextKey.of("jsc.prophet.said.not_kept", "%s is not kept");
    public static final TextKey WATCH_FORGOTTEN = TextKey.of("jsc.prophet.said.watch_forgotten", "watch %s let go");
    public static final TextKey NO_WATCH = TextKey.of("jsc.prophet.said.no_watch", "no watch is numbered %s");
    public static final TextKey LISTED = TextKey.of("jsc.prophet.said.listed", "%s states and %s watches");
    public static final TextKey UNKNOWN_ITEM = TextKey.of("jsc.prophet.said.unknown_item", "no item is called %s");
    public static final TextKey USAGE_KEEP = TextKey.of("jsc.prophet.said.usage_keep",
            "syntax: KEEP item >= n, or KEEP item BETWEEN a AND b");
    public static final TextKey USAGE_WATCH = TextKey.of("jsc.prophet.said.usage_watch",
            "syntax: WATCH item < n DO statement (the comparison one of <, <=, >, >=)");
    public static final TextKey USAGE_FORGET = TextKey.of("jsc.prophet.said.usage_forget",
            "syntax: FORGET item, or FORGET WATCH n");
    public static final TextKey READS_WELL = TextKey.of("jsc.prophet.said.reads_well", "The statement reads well.");

    // Where a state stands.
    public static final TextKey STATUS_NEW = TextKey.of("jsc.prophet.status.new", "New");
    public static final TextKey STATUS_HOLDING = TextKey.of("jsc.prophet.status.holding", "Holding");
    public static final TextKey STATUS_WORKING = TextKey.of("jsc.prophet.status.working", "Working");
    public static final TextKey STATUS_CANNOT_HOLD = TextKey.of("jsc.prophet.status.cannot_hold", "Cannot hold");
    public static final TextKey STATUS_OVER = TextKey.of("jsc.prophet.status.over", "Over");
    public static final TextKey STATUS_ARMED = TextKey.of("jsc.prophet.status.armed", "Armed");
    public static final TextKey STATUS_FIRED = TextKey.of("jsc.prophet.status.fired", "Fired");

    // What it did.
    public static final TextKey ASKED_FOR = TextKey.of("jsc.prophet.reaction.asked_for",
            "asked for %s %s, Operation %s");
    public static final TextKey NOTHING_MAKES = TextKey.of("jsc.prophet.reaction.nothing_makes",
            "nothing on the network makes %s");
    public static final TextKey WATCH_FIRED = TextKey.of("jsc.prophet.reaction.watch_fired", "watch %s fired: %s");
    public static final TextKey WATCH_REFUSED = TextKey.of("jsc.prophet.reaction.watch_refused",
            "watch %s fired, and was refused: %s");
    public static final TextKey NOTHING_TO_DO = TextKey.of("jsc.prophet.reaction.nothing_to_do",
            "watch %s fired with nothing to make");

    private ProphetTexts() {
    }

    /** The word for where a state or a watch stands. */
    public static Text status(final ProphetStates.Status status) {
        return switch (status) {
            case NEW -> STATUS_NEW.text();
            case HOLDING -> STATUS_HOLDING.text();
            case WORKING -> STATUS_WORKING.text();
            case CANNOT_HOLD -> STATUS_CANNOT_HOLD.text();
            case OVER -> STATUS_OVER.text();
            case ARMED -> STATUS_ARMED.text();
            case FIRED -> STATUS_FIRED.text();
        };
    }
}
