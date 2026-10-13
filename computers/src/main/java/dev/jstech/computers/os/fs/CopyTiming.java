/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.os.media.MediaFormat;

/**
 * How long a file copy takes: its size read at the speed of the slower of the two volumes it goes between, or over the
 * network at the slowest cable on the way when that is slower still. A copy on a fast disk is over before anyone
 * sees it; the same file onto a floppy takes a while, which is when a copy window comes up.
 *
 * <p>A file's size is counted in the megabytes its disk says it weighs. Every figure here is an estimate to tune in
 * play, like every other number.
 */
public final class CopyTiming {

    /** What a mechanical disk reads and writes at, in megabytes a second; a faster disk is so many times this. */
    public static final double HDD_MB_PER_SECOND = 20.0;
    /** How much a network cable that carries one item a tick moves, in megabytes a second: four megabytes an item. */
    public static final double MB_PER_SECOND_PER_ITEM_A_TICK = 4.0 * SetupTiming.TICKS_PER_SECOND;
    /** How long a copy may take before its window comes up, in ticks: the second the old systems waited. */
    public static final int MOMENT_TICKS = SetupTiming.TICKS_PER_SECOND;

    private CopyTiming() {
    }

    /** What a disk of that speed reads and writes at, in megabytes a second. */
    public static double diskRate(final int speedMultiplier) {
        return HDD_MB_PER_SECOND * Math.max(1, speedMultiplier);
    }

    /** What a medium of that format reads and writes at, in megabytes a second. */
    public static double mediaRate(final MediaFormat format) {
        return SetupTiming.rateOf(format);
    }

    /** What a network cable carrying that many items a tick moves, in megabytes a second. */
    public static double cableRate(final long itemsPerTick) {
        return Math.max(0L, itemsPerTick) * MB_PER_SECOND_PER_ITEM_A_TICK;
    }

    /** The slowest of the rates a copy goes through, which is the one it goes at. */
    public static double slowest(final double first, final double... more) {
        double slowest = first;
        for (final double rate : more) {
            slowest = Math.min(slowest, rate);
        }
        return slowest;
    }

    /**
     * The ticks a copy of {@code sizeMb} megabytes at {@code mbPerSecond} takes, rounded up: none for a file that
     * weighs nothing, and none for a rate of nothing, which is a road that does not exist rather than an endless one.
     */
    public static int ticks(final long sizeMb, final double mbPerSecond) {
        if (sizeMb <= 0 || mbPerSecond <= 0.0) {
            return 0;
        }
        final double ticks = Math.ceil(sizeMb / mbPerSecond * SetupTiming.TICKS_PER_SECOND);
        return (int) Math.min(Integer.MAX_VALUE, ticks);
    }

    /** Whether a copy that takes that many ticks lasts long enough for its window to come up. */
    public static boolean showsWindow(final int ticks) {
        return ticks > MOMENT_TICKS;
    }
}
