/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.program.install.MakeOpts;
import dev.jstech.computers.program.install.voice.PortageVoices;
import dev.jstech.computers.program.tty.TtyScript;
import java.util.ArrayList;
import java.util.List;

/**
 * A package built from source on a machine whose system builds what it installs.
 *
 * <p>It is the same merge the installer of that system runs, in the same words, because it is the same tool:
 * the dependencies worked out, the source fetched, and then every phase of the build with the compiler's lines
 * going by, for as long as this machine takes over it. How long that is comes from the machine. Each source is
 * fetched for as long as it is big over the machine's connection, and the compile is work got through at the
 * rate the processor manages with as many of its cores as the build options let it use, which a system left
 * as it was installed has set to one.
 */
final class SourceBuild {

    /** No build is quicker than this or slower than that, however the machine was built. */
    private static final long LEAST_SECONDS = 5L;
    private static final long MOST_SECONDS = 1_800L;

    /** The smallest program still counts as this much to compile, since none of it is nothing. */
    private static final long LEAST_WORK_MB = 16L;

    /** How many items of news such a system has waiting, which it mentions every time until they are read. */
    private static final int NEWS_WAITING = 2;

    private SourceBuild() {
    }

    /**
     * The merge of one program on that machine, with whatever has to be built before it.
     *
     * @param ask    whether to list what would be merged and ask before merging it
     * @param merged what having it merged means to the machine, done when the build ends and not before
     */
    static TtyScript of(final ProgramSpec spec, final IOsHost host, final boolean ask, final Runnable merged) {
        final int jobs = jobs(host);
        final int factor = SetupTiming.eraFactor(host.installedEra());
        final List<SourceChains.Link> chain = SourceChains.of(spec);
        double compiled = 0.0;
        for (final SourceChains.Link link : chain) {
            compiled += link.compiles() ? link.sizeMb() : 0.0;
        }
        /* The whole build is as long as the program is big; each package has the share of it its source is. */
        final int whole = buildTicks(spec, host.maxCpuMhz(), jobs);
        final List<PortageVoices.Merge> merges = new ArrayList<>(chain.size());
        for (final SourceChains.Link link : chain) {
            final int build = link.compiles() && compiled > 0.0
                    ? Math.max(20, (int) Math.round(whole * link.sizeMb() / compiled)) : 0;
            final int fetch = link.archive().isEmpty() ? 0
                    : SetupTiming.networkTicks((int) Math.ceil(link.sizeMb()), false, factor);
            merges.add(new PortageVoices.Merge(link.atom(), link.version(), "", link.archive(), link.sizeMb(),
                    link.flagsOn(), link.flagsOff(), fetch, build));
        }
        return PortageVoices.emerge(merges, ask, jobs, NEWS_WAITING, merged);
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
}
