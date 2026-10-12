/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.core.network.DataLine;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import org.jetbrains.annotations.Nullable;

/**
 * What a cable's item says starts a new run of it, under how far one run reaches: the device a player lays in a run
 * that would be too long. A Repeater carries every line but the long distance one on, a Router the access and backbone
 * lines, and a Hub the peripheral line.
 */
@TextHolder
public final class CableRunTexts {

    public static final TextKey REPEATER_OR_ROUTER = TextKey.of("jsc.cable.renewed.repeater_or_router",
            "A Repeater or a Router starts a new run");
    public static final TextKey REPEATER = TextKey.of("jsc.cable.renewed.repeater", "A Repeater starts a new run");
    public static final TextKey HUB = TextKey.of("jsc.cable.renewed.hub", "A Hub starts a new run");

    private CableRunTexts() {
    }

    /** What starts a new run of a data cable of that line, or null for the long distance line, which has no run. */
    @Nullable
    public static TextKey forLine(final DataLine line) {
        return switch (line) {
            case ACCESS, BACKBONE -> REPEATER_OR_ROUTER;
            case HPC, CRAFTING -> REPEATER;
            case LONG_DISTANCE -> null;
        };
    }
}
