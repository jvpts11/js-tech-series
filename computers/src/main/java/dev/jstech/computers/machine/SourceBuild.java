/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.program.install.MakeOpts;
import dev.jstech.computers.program.install.voice.PortageVoices;
import dev.jstech.computers.program.tty.TtyScript;
import java.util.List;
import java.util.Locale;

/**
 * A package built from source on a machine whose system builds what it installs.
 *
 * <p>It is the same merge the installer of that system runs, in the same words, because it is the same tool:
 * the dependencies worked out, the source fetched, and then every phase of the build with the compiler's lines
 * going by, for as long as this machine takes over it. How long that is comes from the machine. The source is
 * as big as the program is, the fetch is as long as that is over the machine's connection, and the compile is
 * work got through at the rate the processor manages with as many of its cores as the build options let it
 * use, which a system left as it was installed has set to one.
 */
final class SourceBuild {

    /** How much of a program's installed size its source comes to, the usual kind of estimate. */
    private static final double SOURCE_SHARE = 0.25;

    /** No build is quicker than this or slower than that, however the machine was built. */
    private static final long LEAST_SECONDS = 5L;
    private static final long MOST_SECONDS = 1_800L;

    /** The smallest program still counts as this much to compile, since none of it is nothing. */
    private static final long LEAST_WORK_MB = 16L;

    private SourceBuild() {
    }

    /**
     * The merge of one program on that machine.
     *
     * @param ask    whether to list what would be merged and ask before merging it
     * @param merged what having it merged means to the machine, done when the build ends and not before
     */
    static TtyScript of(final ProgramSpec spec, final IOsHost host, final boolean ask, final Runnable merged) {
        final int jobs = jobs(host);
        final double sourceMb = Math.max(1.0, spec.minDiskMb() * SOURCE_SHARE);
        final String name = spec.commandName().toLowerCase(Locale.ROOT);
        final String version = ProgramVersions.of(spec.id());
        final PortageVoices.Merge merge = new PortageVoices.Merge(category(spec.kind()) + "/" + name, version, "",
                name + "-" + version + ".tar.xz", sourceMb, "nls", "-debug",
                SetupTiming.networkTicks((int) Math.ceil(sourceMb), false, SetupTiming.eraFactor(host.installedEra())),
                buildTicks(spec, host.maxCpuMhz(), jobs));
        return PortageVoices.emerge(List.of(merge), ask, jobs, merged);
    }

    /**
     * How many jobs a build on that machine runs: what its build options ask for, and no more than it has cores,
     * because asking for more than there are buys nothing.
     */
    static int jobs(final IOsHost host) {
        final int asked = MakeOpts.jobs(DiskFilesystem.read(host.systemDisk(), MakeOpts.PATH).orElse(null));
        return Math.max(1, Math.min(asked, Math.max(1, host.cpuCores())));
    }

    /**
     * How long the compile takes: the program's size over the processor's clock, shared between the jobs.
     *
     * <p>A balancing estimate, held between a few seconds and half an hour.
     */
    static int buildTicks(final ProgramSpec spec, final int mhz, final int jobs) {
        final long work = Math.max(LEAST_WORK_MB, spec.minDiskMb()) * 1_000L;
        final long seconds = work / Math.max(100, mhz) / Math.max(1, jobs);
        return (int) (Math.max(LEAST_SECONDS, Math.min(MOST_SECONDS, seconds)) * SetupTiming.TICKS_PER_SECOND);
    }

    /** Where the package tree files a program of that kind, near enough. */
    private static String category(final ProgramKind kind) {
        return switch (kind) {
            case APP -> "app-misc";
            case SERVICE -> "net-misc";
            case HYBRID -> "app-admin";
            case DESKTOP_ENVIRONMENT -> "x11-wm";
        };
    }
}
