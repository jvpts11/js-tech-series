/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

/**
 * The last reading the server sent this player's clamp meter, as its display and the HUD show it, and the one HOLD
 * kept still. On a dedicated server it stays empty.
 */
public final class MeterView {

    private static volatile MeterReading latest = MeterReading.nothing();
    private static volatile MeterReading held = MeterReading.nothing();
    private static volatile boolean holding;

    private MeterView() {
    }

    /** Takes in what the server read. */
    public static void accept(final MeterReading reading) {
        latest = reading;
    }

    /** What to show: the reading HOLD kept while it is pressed, the latest otherwise. */
    public static MeterReading shown(final boolean hold) {
        if (hold && !holding) {
            held = latest;
            holding = true;
        } else if (!hold) {
            holding = false;
        }
        return hold ? held : latest;
    }
}
