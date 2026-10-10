/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a bus's window tells the player when it refuses a change. */
@TextHolder
final class BusMenuTexts {

    static final TextKey ONE_HOURS_WINDOW = TextKey.of("jsc.bus.one_hours_window",
            "A bus holds only one hours window. Remove it first to set another.");

    private BusMenuTexts() {
    }
}
