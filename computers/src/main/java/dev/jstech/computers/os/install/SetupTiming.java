/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.core.tier.HardwareEra;

/**
 * How long installing a program takes.
 *
 * <p>The program's size, read at the speed of whatever it comes from: a floppy is slow because it is a
 * floppy, and a fibre link is not. The result is clamped so the window is always seen and nobody ever
 * waits for a game, and removing is a fifth of installing, since throwing files away is quicker than
 * copying them. Every figure here is an estimate to tune in play, like every other number.
 */
public final class SetupTiming {

    /** Ticks per second, the world's clock. */
    public static final int TICKS_PER_SECOND = 20;

    /** The least a setup takes, so the window is seen even for a tiny program on a fast medium. */
    public static final int MIN_SECONDS = 3;

    /** The most a setup takes, so a huge program on a floppy does not become a chore. */
    public static final int MAX_SECONDS = 90;

    /** The share of the install time that removing the same program takes. */
    public static final int REMOVE_DIVISOR = 5;

    /** What the network reads at when a program comes from the Mirror rather than a disc. */
    public static final double NETWORK_MB_PER_SECOND = 8.0;

    /**
     * The processor a system install is measured against: one core at two gigahertz.
     *
     * <p>A machine with exactly this much gets no help and no penalty; everything else is read against it, so
     * a two-core machine at that clock copies twice as fast and a single slow core takes longer.
     */
    public static final double CPU_REFERENCE_MHZ_CORES = 2_000.0;

    /**
     * The least the processor can count for, however little of it there is.
     *
     * <p>Without this the earliest machines would be measured at a tenth of the reference and installing
     * anything on them would be nothing but waiting; a floppy is already slow enough to say what era it is.
     */
    public static final double CPU_MIN_FACTOR = 0.5;

    private SetupTiming() {
    }

    /**
     * What a system install reads and writes at, in megabytes per second, on this machine from that medium.
     *
     * <p>Three things decide it, and they are all parts a player chose: the medium it is read from, the disk
     * it is written to, and the processor that unpacks it in between. The machine's generation is not in here
     * on its own any more, because it was the only thing in here and that made every machine of an age take
     * exactly as long as every other. It is in here now through the parts, which is where a generation
     * actually lives: a Legacy board takes a CD and an IDE disk, and putting a solid-state disk in that same
     * machine really does cut the wait.
     *
     * @param format              the medium the system is read from
     * @param diskSpeedMultiplier the tier of the disk it is written to, where a mechanical disk is 1
     * @param cores               how many cores the processor has
     * @param mhz                 what one of those cores runs at
     */
    public static double installRate(final MediaFormat format, final int diskSpeedMultiplier,
                                     final int cores, final int mhz) {
        return rateOf(format) * Math.max(1, diskSpeedMultiplier) * cpuFactor(cores, mhz);
    }

    /** How much faster than the reference processor this one is, never counting for less than the floor. */
    public static double cpuFactor(final int cores, final int mhz) {
        final double power = (double) Math.max(0, cores) * Math.max(0, mhz);
        return Math.max(CPU_MIN_FACTOR, power / CPU_REFERENCE_MHZ_CORES);
    }

    /**
     * The ticks a system of {@code sizeMb} takes to be installed on this machine from that medium.
     *
     * <p>Clamped like every other setup, so the smallest system is still watched for a moment and the largest
     * one on the worst hardware there is stays something a player waits out rather than leaves the game over.
     */
    public static int installTicks(final int sizeMb, final MediaFormat format, final int diskSpeedMultiplier,
                                   final int cores, final int mhz) {
        return ticks(sizeMb, installRate(format, diskSpeedMultiplier, cores, mhz), false);
    }

    /** What a medium of that format reads at, in megabytes per second. */
    public static double rateOf(final MediaFormat format) {
        return switch (format) {
            case FLOPPY -> 1.0;
            case CD -> 4.0;
            case DVD -> 16.0;
            case USB -> 32.0;
        };
    }

    /**
     * The ticks a setup takes for a program of {@code sizeMb} read at {@code mbPerSecond}.
     *
     * <p>A program that takes no room on the disk is still a setup that is seen, which is what the
     * lower clamp is for; a rate of nothing would take forever, so it is treated as the slowest medium.
     */
    public static int ticks(final int sizeMb, final double mbPerSecond, final boolean removing) {
        final double rate = mbPerSecond > 0 ? mbPerSecond : rateOf(MediaFormat.FLOPPY);
        double seconds = Math.max(0, sizeMb) / rate;
        seconds = Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
        if (removing) {
            seconds = Math.max(1.0, seconds / REMOVE_DIVISOR);
        }
        return (int) Math.round(seconds * TICKS_PER_SECOND);
    }

    /** The same, for a program that comes from a disc of that format. */
    public static int ticks(final int sizeMb, final MediaFormat format, final boolean removing) {
        return ticks(sizeMb, rateOf(format), removing);
    }

    /** The same, for a program that comes over the network from the Mirror. */
    public static int networkTicks(final int sizeMb, final boolean removing) {
        return ticks(sizeMb, NETWORK_MB_PER_SECOND, removing);
    }

    /**
     * How much faster a machine of that generation sets a program up than a Vintage one.
     *
     * <p>A floppy is a floppy, but what unpacks and writes what it carries is the computer, and each
     * generation does that twice as fast as the one before: Vintage 1, Legacy 2, Standard 4, and so on.
     * A machine with no generation to speak of counts as Vintage.
     */
    public static int eraFactor(final HardwareEra era) {
        return era == null ? 1 : 1 << era.level();
    }

    /** A disc's time on a machine that works {@code factor} times as fast as a Vintage one. */
    public static int ticks(final int sizeMb, final MediaFormat format, final boolean removing, final int factor) {
        return ticks(sizeMb, rateOf(format) * Math.max(1, factor), removing);
    }

    /** The Mirror's time on a machine that works {@code factor} times as fast as a Vintage one. */
    public static int networkTicks(final int sizeMb, final boolean removing, final int factor) {
        return ticks(sizeMb, NETWORK_MB_PER_SECOND * Math.max(1, factor), removing);
    }
}
