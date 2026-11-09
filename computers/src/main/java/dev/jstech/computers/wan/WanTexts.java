/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The words of an Operation that crosses a WAN link, as its record's reason says where it has got to, and of the four
 * things one network may do on another.
 */
@TextHolder
public final class WanTexts {

    public static final TextKey IN_TRANSIT = TextKey.of("jsc.wan.op.in_transit", "In transit on the %s: %s of %s here");
    public static final TextKey THROUGH = TextKey.of("jsc.wan.op.through", "Through %s, which relays it: %s lines");
    public static final TextKey NOT_LOADED = TextKey.of("jsc.wan.op.not_loaded",
            "%s is not loaded: waits for its chunks");
    public static final TextKey LINE_DOWN = TextKey.of("jsc.wan.op.line_down", "The line to %s is down: waits for it");
    public static final TextKey LINE_FELL = TextKey.of("jsc.wan.op.line_fell",
            "The line fell: %s items in transit were lost");
    public static final TextKey NOT_ALLOWED = TextKey.of("jsc.wan.op.not_allowed",
            "%s does not let this network %s");
    public static final TextKey FAR_REFUSED = TextKey.of("jsc.wan.op.far_refused",
            "%s could not start it: its Mainframe runs no engine");
    public static final TextKey ALL_HERE = TextKey.of("jsc.wan.op.all_here", "All %s here");
    public static final TextKey SOME_HERE = TextKey.of("jsc.wan.op.some_here", "%s of %s here");
    public static final TextKey TOOK = TextKey.of("jsc.wan.op.took", "%s took %s of %s");
    public static final TextKey CRAFTING_THERE = TextKey.of("jsc.wan.op.crafting_there", "Being crafted on %s");
    public static final TextKey CRAFTED_THERE = TextKey.of("jsc.wan.op.crafted_there", "%s crafted %s");
    public static final TextKey CANCELLED_BOTH = TextKey.of("jsc.wan.op.cancelled_both",
            "Cancelled here and on %s");
    public static final TextKey RELAYED = TextKey.of("jsc.wan.op.relayed", "Relayed from %s to %s");
    private static final TextKey FROM = TextKey.of("jsc.wan.op.from", "WAN: %s");
    private static final TextKey SEE = TextKey.of("jsc.wan.right.see", "see its items");
    private static final TextKey TAKE = TextKey.of("jsc.wan.right.take", "take items");
    private static final TextKey SEND = TextKey.of("jsc.wan.right.send", "send items");
    private static final TextKey CRAFT = TextKey.of("jsc.wan.right.craft", "craft there");

    private WanTexts() {
    }

    /** What doing {@code right} is called, in a sentence. */
    public static TextKey rightName(final WanRight right) {
        return switch (right) {
            case SEE -> SEE;
            case TAKE -> TAKE;
            case SEND -> SEND;
            case CRAFT -> CRAFT;
        };
    }

    /** The label the far network's half of a crossing goes by: who asked for it. */
    public static String from(final String asker) {
        return GameText.resolve(FROM.with(asker));
    }
}
