/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.core.tier.HardwareEra;

/**
 * How long a machine takes to come up: the self-test, the system booting, and shutting down again.
 *
 * <p>None of it is a fixed number, because none of it is fixed in life. The self-test grows with what is seated in
 * the machine, since every part of it has to be found and counted, and shrinks with the generation, since a newer
 * firmware does the same work faster. A system comes up at a speed set by how big it is, how fast the disk it sits
 * on is and how new the machine is, so the same system is quick on one computer and a wait on another, and choosing
 * a disk becomes a choice about something.
 *
 * <p>Every figure here is an estimate to tune in play, like the ones in {@link SetupTiming}, whose era factor and
 * clock this shares so that the mod has one way of thinking about time rather than three.
 */
public final class BootTiming {

    /** What the self-test costs before anything is counted: the firmware waking and putting its name up. */
    public static final double POST_BASE_SECONDS = 3.0;

    /** What counting one memory module costs. On screen this is the time the memory count spends climbing. */
    public static final double POST_SECONDS_PER_MODULE = 0.4;

    /** What finding one device costs: a disk, a graphics card, an expansion card, a drive with something in it. */
    public static final double POST_SECONDS_PER_DEVICE = 0.25;

    /** The least a self-test takes, so it is always seen; a modern machine sits here. */
    public static final double POST_MIN_SECONDS = 1.0;

    /** The most it takes, so a machine stuffed with parts does not become a chore. */
    public static final double POST_MAX_SECONDS = 12.0;

    /** What a boot costs before the system is read at all: the logo appearing. */
    public static final double BOOT_BASE_SECONDS = 1.0;

    /**
     * How much of a system comes up per second on the slowest machine there is, measured against the system's own
     * size. It is not a disk's speed: the disk's tier and the machine's generation multiply it.
     */
    public static final double BOOT_FOOTPRINT_MB_PER_SECOND = 160.0;

    /** How many times its own memory a system wants before it stops being pressed for room. */
    public static final int BOOT_RAM_HEADROOM = 2;

    /** What a boot costs when the machine has no memory to spare for the system on it. */
    public static final double BOOT_TIGHT_RAM_FACTOR = 1.5;

    /** The least a boot takes, so the sequence is read rather than glimpsed. */
    public static final double BOOT_MIN_SECONDS = 2.0;

    /**
     * The most a boot takes. A big system on a mechanical disk reaches it, which is the point: the wait is what a
     * player is choosing when they leave it there. Closing the monitor does not stop the machine coming up.
     */
    public static final double BOOT_MAX_SECONDS = 20.0;

    /** Shutting down is a share of coming up: the same work, less of it. */
    public static final int SHUTDOWN_DIVISOR = 3;

    public static final double SHUTDOWN_MIN_SECONDS = 1.0;

    public static final double SHUTDOWN_MAX_SECONDS = 5.0;

    private BootTiming() {
    }

    /**
     * The ticks a self-test takes on a machine with that much in it.
     *
     * @param memoryModules how many memory modules are seated, each of which is counted
     * @param devices       how many devices have to be found: disks, cards and drives holding something
     * @param era           the machine's generation, which is how fast its firmware works
     */
    public static int postTicks(final int memoryModules, final int devices, final HardwareEra era) {
        final double found = POST_BASE_SECONDS
                + Math.max(0, memoryModules) * POST_SECONDS_PER_MODULE
                + Math.max(0, devices) * POST_SECONDS_PER_DEVICE;
        return ticks(clamp(found / SetupTiming.eraFactor(era), POST_MIN_SECONDS, POST_MAX_SECONDS));
    }

    /**
     * The ticks a system takes to come up.
     *
     * @param footprintMb        the size of the installed system
     * @param diskSpeedMultiplier the tier of the disk it boots from, where a mechanical disk is 1
     * @param era                the machine's generation
     * @param tightRam           whether the machine leaves the system no room to spare
     */
    public static int bootTicks(final int footprintMb, final int diskSpeedMultiplier, final HardwareEra era,
                                final boolean tightRam) {
        final double rate = BOOT_FOOTPRINT_MB_PER_SECOND
                * Math.max(1, diskSpeedMultiplier) * SetupTiming.eraFactor(era);
        double seconds = BOOT_BASE_SECONDS + Math.max(0, footprintMb) / rate;
        if (tightRam) {
            seconds *= BOOT_TIGHT_RAM_FACTOR;
        }
        return ticks(clamp(seconds, BOOT_MIN_SECONDS, BOOT_MAX_SECONDS));
    }

    /** Whether a machine with that much memory leaves the system on it no room to spare. */
    public static boolean tightRam(final int installedMb, final int systemMb) {
        return systemMb > 0 && installedMb < systemMb * BOOT_RAM_HEADROOM;
    }

    /** The ticks shutting down takes, from the ticks coming up took. */
    public static int shutdownTicks(final int bootTicks) {
        final double seconds = (double) Math.max(0, bootTicks) / SetupTiming.TICKS_PER_SECOND / SHUTDOWN_DIVISOR;
        return ticks(clamp(seconds, SHUTDOWN_MIN_SECONDS, SHUTDOWN_MAX_SECONDS));
    }

    private static double clamp(final double seconds, final double least, final double most) {
        return Math.max(least, Math.min(most, seconds));
    }

    private static int ticks(final double seconds) {
        return (int) Math.round(seconds * SetupTiming.TICKS_PER_SECOND);
    }
}
