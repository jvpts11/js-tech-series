/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * A portable's battery (computers/docs/design/wireless.md), in millijoules: a device draws a watt while it is on, so a
 * full charge lasts its kind's {@link PortableKind#batterySeconds() time}, and its era's Mobile Device Charging Dock
 * gives it back at the watts of that era's charger, the energy taken from the dock's line. The figures are estimates.
 *
 * <p>Pure: no part of the game.
 */
public final class PortableBattery {

    /** What a device on draws, in millijoules a tick: a watt. */
    public static final int DRAWN_A_TICK = 50;
    /** The width of an item's bar under its icon, which the game draws in that many pixels. */
    public static final int BAR_PIXELS = 13;

    private static final int MILLIJOULES_A_JOULE = 1_000;
    private static final int TICKS_A_SECOND = 20;
    private static final int GOOD_ABOVE = 50;
    private static final int LOW_ABOVE = 20;

    private PortableBattery() {
    }

    /** How charged a battery can be on a device of {@code model}, in millijoules. */
    public static int capacity(final PortableModel model) {
        return model.kind().batterySeconds() * MILLIJOULES_A_JOULE;
    }

    /** How full {@code stored} millijoules leave a battery of {@code capacity}, from 0 to 1. */
    public static float fraction(final int stored, final int capacity) {
        return capacity <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, (float) stored / capacity));
    }

    /** The share a battery shows, from 0 to 100: a battery not quite empty shows 1 rather than 0. */
    public static int percent(final int stored, final int capacity) {
        if (stored <= 0) {
            return 0;
        }
        return Math.max(1, Math.round(fraction(stored, capacity) * 100.0F));
    }

    /** How many of the bar's pixels a battery that full fills. */
    public static int barWidth(final float fraction) {
        return Math.round(BAR_PIXELS * Math.max(0.0F, Math.min(1.0F, fraction)));
    }

    /** How a battery showing {@code percent} reads: plenty above half, low above a fifth, nearly flat below. */
    public static Level level(final int percent) {
        return percent > GOOD_ABOVE ? Level.GOOD : percent > LOW_ABOVE ? Level.LOW : Level.CRITICAL;
    }

    /** How long {@code stored} millijoules keep a device on, in seconds of the server running. */
    public static int secondsLeft(final int stored) {
        return Math.max(0, stored) / (DRAWN_A_TICK * TICKS_A_SECOND);
    }

    /**
     * The watts the charger of a dock of {@code era} gives: the five of a cradle's and a USB charger's to the
     * Transition, ten in the Standard, fifteen on the Advanced's wireless stand (estimates).
     */
    public static int chargerWatts(final Era era) {
        if (era.isAtMost(HardwareEra.TRANSITION)) {
            return 5;
        }
        return era.isAtMost(HardwareEra.STANDARD) ? 10 : 15;
    }

    /** What a dock of {@code era} gives a battery a tick, in millijoules. */
    public static int chargedATick(final Era era) {
        return chargerWatts(era) * MILLIJOULES_A_JOULE / TICKS_A_SECOND;
    }

    /** How a battery reads, from the colour of its share on a tooltip. */
    public enum Level {
        GOOD,
        LOW,
        CRITICAL
    }
}
