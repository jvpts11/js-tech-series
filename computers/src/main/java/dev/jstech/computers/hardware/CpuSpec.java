/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.HardwareEra;

import java.util.Objects;

/**
 * Immutable specification of a CPU.
 *
 * <p>The instruction set architecture (ISA) is a field of its own rather than a reading of the era, because the two
 * are separate questions: the era says when the chip was made and the ISA says what it understands. They line up for
 * this mod's own processors, and the shorter constructor is the one that says so.
 *
 * <p>How much a processor orchestrates is its cores, times their clock, times the work its design does per clock
 * ({@link CpuDesign}): an old chip at a high clock no longer outruns a better one that came after it. A chip is
 * declared with its design on top, {@code new CpuSpec(...).on(Microarchitectures.HASWELL, "")}.
 */
public record CpuSpec(HardwareEra era,
                      IsaSpec isa,
                      CpuSocketId socket,
                      int cores,
                      int freqMhz,
                      int tdpWatts,
                      boolean alien,
                      CpuDesign design) {

    /** Items per tick one core of the P6's design orchestrates at a gigahertz. */
    private static final long CAPACITY_FACTOR = 40L;

    /*
     * What the sum below is counted in: megahertz, thousandths of a design's efficiency, and tenths of the thread
     * factor (10 for one thread a core, 12 for two). Dividing by all three at once, after everything is multiplied,
     * keeps the whole sum in whole numbers, so no chip loses or gains an item a tick to a rounding on the way.
     */
    private static final long SCALE = 1000L * 1000L * 10L;

    public CpuSpec {
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(isa, "isa must not be null");
        Objects.requireNonNull(socket, "socket must not be null");
        Objects.requireNonNull(design, "design must not be null");
        if (cores <= 0) {
            throw new IllegalArgumentException("cores must be > 0; got " + cores);
        }
        if (freqMhz <= 0) {
            throw new IllegalArgumentException("freqMhz must be > 0; got " + freqMhz);
        }
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
    }

    /** A processor that says nothing of its design, which counts as the P6's. */
    public CpuSpec(final HardwareEra era, final IsaSpec isa, final CpuSocketId socket, final int cores,
                   final int freqMhz, final int tdpWatts, final boolean alien) {
        this(era, isa, socket, cores, freqMhz, tdpWatts, alien, CpuDesign.UNSPECIFIED);
    }

    /** A processor of this mod's own hardware, built on the instruction set its era is made of. */
    public CpuSpec(final HardwareEra era, final CpuSocketId socket, final int cores, final int freqMhz,
                   final int tdpWatts, final boolean alien) {
        this(era, Isas.of(era), socket, cores, freqMhz, tdpWatts, alien);
    }

    /** The same processor, built on that design and named by that codename (empty when the design says it all). */
    public CpuSpec on(final Microarchitecture arch, final String codename) {
        return with(CpuDesign.of(arch, codename));
    }

    /** The same processor with two threads to each performance core. */
    public CpuSpec withSmt() {
        return with(design.withSmt());
    }

    /** The same processor with {@code count} efficiency cores of that design, running at their own clock. */
    public CpuSpec withEfficiencyCores(final int count, final int mhz, final Microarchitecture arch) {
        return with(design.withEfficiencyCores(count, mhz, arch));
    }

    /** The same processor carrying those graphics on its die, which give its board a video output. */
    public CpuSpec withGraphics(final IntegratedGraphics graphics) {
        return with(design.withGraphics(graphics));
    }

    /** Whether the processor carries graphics of its own. */
    public boolean hasIntegratedGraphics() {
        return design.graphics().present();
    }

    /**
     * Items per tick this processor orchestrates: its cores times their clock in gigahertz times their design's
     * efficiency, a fifth more with two threads a core, plus the efficiency cores at their own clock and design, all
     * times 40 (three times that for a chip of another world). Never below one: a processor that runs does some work.
     */
    public long orchestrationCapacity() {
        final long threads = design.smt() ? 12L : 10L;
        final long performance = (long) cores * freqMhz * design.arch().efficiencyMilli() * threads;
        final long efficiency = (long) design.efficiencyCores() * design.efficiencyMhz()
                * design.efficiencyArch().efficiencyMilli() * 10L;
        final long multiplier = alien ? 3L : 1L;
        final long scaled = (performance + efficiency) * CAPACITY_FACTOR * multiplier;
        return Math.max(1L, (scaled + SCALE / 2L) / SCALE);
    }

    private CpuSpec with(final CpuDesign next) {
        return new CpuSpec(era, isa, socket, cores, freqMhz, tdpWatts, alien, next);
    }
}
