/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.os.install.SetupTiming;

/**
 * How long each kind of step takes on the machine it is running on.
 *
 * <p>None of it is a fixed number, because none of it is fixed in life. A fetch is as long as the file is big
 * over the connection the machine has; laying files out is as long as they are big over the disk they go on;
 * and compiling is work, a fixed amount of it for a given piece of software, got through at the rate the
 * processor manages with as many of its cores as the build was told it may use. That last one is the reason
 * to put a good processor in a machine meant for this distribution, and the reason the build options are
 * worth editing: a build left alone uses one core of however many there are.
 *
 * <p>Every figure is an estimate to tune in play.
 */
final class LiveTimes {

    /** What compiling a kernel costs, in megahertz-seconds across every core put to it. */
    static final long KERNEL_WORK = 512_000L;

    /** What compiling a megabyte of a package's source costs, in the same unit. */
    private static final long WORK_PER_SOURCE_MB = 6_000L;

    /** How much a mechanical disk has written out by the end of one tick. */
    private static final double MB_PER_TICK_WRITTEN = 8.0;

    /** No build is quicker than this or slower than that, however the machine was built. */
    private static final long LEAST_SECONDS = 3L;
    private static final long MOST_SECONDS = 1_800L;

    private static final int TICKS_PER_SECOND = 20;

    private LiveTimes() {
    }

    /** How long fetching that much over the network takes this machine. */
    static int fetch(final double megabytes, final LiveInstallState.Env env) {
        return (int) Math.max(4L, SetupTiming.networkTicks((int) Math.ceil(megabytes), false,
                Math.max(1, env.eraFactor())));
    }

    /**
     * How long laying that much out over a disk takes, which is the disk's speed and nothing else.
     *
     * <p>A mechanical disk takes a tick for every eight megabytes, which puts a base system at six seconds or
     * so; a faster disk divides that, down to a floor that keeps the step something that happened.
     */
    static int write(final double megabytes, final int diskSpeed) {
        return (int) Math.max(20L, Math.round(megabytes / MB_PER_TICK_WRITTEN / Math.max(1, diskSpeed)));
    }

    /** How long that much compiling takes with that many jobs, on the processor the machine has. */
    static int compile(final long work, final int jobs, final LiveInstallState.Env env) {
        final long seconds = Math.max(LEAST_SECONDS, Math.min(MOST_SECONDS,
                work / Math.max(100, env.mhz()) / Math.max(1, jobs)));
        return (int) (seconds * TICKS_PER_SECOND);
    }

    /** How long building a package whose source is that big takes. */
    static int build(final double sourceMegabytes, final int jobs, final LiveInstallState.Env env) {
        return compile(Math.round(sourceMegabytes * WORK_PER_SOURCE_MB), jobs, env);
    }

    /**
     * How many jobs a build really runs: what was asked for, and no more than the machine has cores.
     *
     * <p>Asking for more than there are buys nothing, because the cores are the ceiling.
     */
    static int jobs(final int asked, final LiveInstallState.Env env) {
        return Math.max(1, Math.min(asked, Math.max(1, env.cores())));
    }
}
