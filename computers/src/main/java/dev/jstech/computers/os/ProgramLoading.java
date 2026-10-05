/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.os.fs.CopyTiming;
import dev.jstech.computers.os.install.SetupTiming;

/**
 * How long a program takes to come up when it is started: its weight read off the machine's disk at the pace its
 * processor unpacks it, so a light program on a quick machine is up in about a second and a heavy one on a slow
 * machine takes up to five. A desktop's window appears the moment it is asked for; this is how long the system shows
 * the start under way, with its working pointer, KDE's bouncing icon or CDE's busy light.
 *
 * <p>The pace is the same one a setup is read at, from the disk and the processor the player put in the machine.
 * Every figure here is an estimate to tune in play, like every other number.
 */
public final class ProgramLoading {

    /** The least a start takes, in milliseconds: even the lightest program on the quickest machine takes a second. */
    public static final long LEAST_MILLIS = 1_000L;
    /** The most a start takes, in milliseconds, however heavy the program and slow the machine. */
    public static final long MOST_MILLIS = 5_000L;
    /** The pace a machine is taken to load at before it has said its own: a mechanical disk, the reference processor. */
    public static final double REFERENCE_MB_PER_SECOND = CopyTiming.HDD_MB_PER_SECOND;

    private ProgramLoading() {
    }

    /**
     * What a machine loads a program at, in megabytes a second: its disk's speed times how much faster than the
     * reference its processor is.
     *
     * @param diskSpeedMultiplier the tier of the disk the program is on, where a mechanical disk is 1
     * @param cores               how many cores the machine has
     * @param mhz                 what one of those cores runs at
     */
    public static double loadRate(final int diskSpeedMultiplier, final int cores, final int mhz) {
        return CopyTiming.diskRate(diskSpeedMultiplier) * SetupTiming.cpuFactor(cores, mhz);
    }

    /**
     * How long a program of {@code programMb} takes to come up on a machine that loads at {@code mbPerSecond}, in
     * milliseconds: a second, and the time its weight takes to read, five seconds at most. A rate of nothing is read
     * as the reference pace rather than as a start that never ends.
     */
    public static long startMillis(final int programMb, final double mbPerSecond) {
        final double rate = mbPerSecond > 0.0 ? mbPerSecond : REFERENCE_MB_PER_SECOND;
        final double millis = LEAST_MILLIS + Math.max(0, programMb) / rate * 1_000.0;
        return Math.min(MOST_MILLIS, Math.round(millis));
    }
}
