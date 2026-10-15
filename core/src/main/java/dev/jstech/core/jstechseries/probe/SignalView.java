/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

/**
 * The last reading the server sent this player's signal probe, as its display and the HUD show it, and the one HOLD
 * kept still. On a dedicated server it stays empty.
 */
public final class SignalView {

    private static volatile SignalReading latest = SignalReading.nothing();
    private static volatile SignalReading held = SignalReading.nothing();
    private static volatile boolean holding;

    private SignalView() {
    }

    /** Takes in what the server read. */
    public static void accept(final SignalReading reading) {
        latest = reading;
    }

    /** What to show: the reading HOLD kept while it is pressed, the latest otherwise. */
    public static SignalReading shown(final boolean hold) {
        if (hold && !holding) {
            held = latest;
            holding = true;
        } else if (!hold) {
            holding = false;
        }
        return hold ? held : latest;
    }
}
