/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;

/**
 * How a processor is built, beside how many cores it has and how fast they run: the design of its cores, the codename
 * of the chip, whether each core runs two threads, and, on a hybrid chip, the smaller efficiency cores beside the
 * performance ones.
 *
 * <p>Written as a design with what it adds named on top, {@code CpuDesign.of(HASWELL, "Devil's Canyon").withSmt()},
 * so a processor's declaration reads as the chip it is.
 *
 * @param arch             the design of the performance cores, which are the cores the spec counts
 * @param codename         the chip's own codename, or empty when its design's name says it all (Haswell)
 * @param smt              whether each performance core runs two threads, which is worth a fifth more work
 * @param efficiencyCores  how many efficiency cores the chip adds, 0 on a chip that has none
 * @param efficiencyMhz    the clock the efficiency cores run at, which is their own
 * @param efficiencyArch   the design of the efficiency cores
 * @param graphics         the graphics the chip carries on its die, {@link IntegratedGraphics#NONE} on most
 * @param memoryItems      the memory the chip carries in its package beside the die, in the buffer items a memory
 *                         module is counted in; 0 on most, and shared with its graphics when it has both
 * @param heatSinkWatts    the heat taken away by a heat sink the processor carries on itself, as a module with its
 *                         sink on it does, in watts; 0 on most, which take a cooler over them
 */
@ApiStatus.Experimental
public record CpuDesign(Microarchitecture arch, String codename, boolean smt, int efficiencyCores, int efficiencyMhz,
                        Microarchitecture efficiencyArch, IntegratedGraphics graphics, long memoryItems,
                        int heatSinkWatts) {

    /** A processor that says nothing of how it is built: one thread a core, of the P6's worth. */
    public static final CpuDesign UNSPECIFIED = of(Microarchitectures.UNSPECIFIED, "");

    public CpuDesign {
        Objects.requireNonNull(arch, "a design must name its cores' design");
        Objects.requireNonNull(codename, "a codename may be empty but not missing");
        Objects.requireNonNull(efficiencyArch, "efficiency cores must name their design, even when there are none");
        Objects.requireNonNull(graphics, "a chip with no graphics of its own says NONE");
        if (efficiencyCores < 0 || efficiencyMhz < 0) {
            throw new IllegalArgumentException("efficiency cores and their clock cannot be negative");
        }
        if (efficiencyCores > 0 && efficiencyMhz == 0) {
            throw new IllegalArgumentException("efficiency cores need a clock of their own");
        }
        if (memoryItems < 0) {
            throw new IllegalArgumentException("a chip's memory cannot be negative; got " + memoryItems);
        }
        if (heatSinkWatts < 0) {
            throw new IllegalArgumentException("a heat sink cannot take away less than nothing; got " + heatSinkWatts);
        }
    }

    /**
     * A chip of that design and codename, one thread a core, no efficiency cores, and no memory or heat sink of its
     * own.
     */
    public static CpuDesign of(final Microarchitecture arch, final String codename) {
        return new CpuDesign(arch, codename, false, 0, 0, Microarchitectures.UNSPECIFIED, IntegratedGraphics.NONE, 0L,
                0);
    }

    /** The same chip with two threads to each performance core. */
    public CpuDesign withSmt() {
        return new CpuDesign(arch, codename, true, efficiencyCores, efficiencyMhz, efficiencyArch, graphics,
                memoryItems, heatSinkWatts);
    }

    /** The same chip with {@code count} efficiency cores of that design, running at their own clock. */
    public CpuDesign withEfficiencyCores(final int count, final int mhz, final Microarchitecture design) {
        return new CpuDesign(arch, codename, smt, count, mhz, design, graphics, memoryItems, heatSinkWatts);
    }

    /** The same chip carrying those graphics on its die. */
    public CpuDesign withGraphics(final IntegratedGraphics onDie) {
        return new CpuDesign(arch, codename, smt, efficiencyCores, efficiencyMhz, efficiencyArch, onDie,
                memoryItems, heatSinkWatts);
    }

    /**
     * The same chip carrying {@code items} of memory in its package, as Quince's own chips do
     * (computers/docs/design/quince.md): its machine has that memory with no module in a slot.
     */
    public CpuDesign withMemory(final long items) {
        return new CpuDesign(arch, codename, smt, efficiencyCores, efficiencyMhz, efficiencyArch, graphics, items,
                heatSinkWatts);
    }

    /**
     * The same chip as a module carrying its own heat sink, which takes away {@code watts}, as the PowerQ G4's did
     * (computers/docs/design/quince.md): it is cooled with no cooler over it, and its sink takes the cooler's seat.
     */
    public CpuDesign withHeatSink(final int watts) {
        return new CpuDesign(arch, codename, smt, efficiencyCores, efficiencyMhz, efficiencyArch, graphics,
                memoryItems, watts);
    }

    /** The design and the codename as a tooltip reads them, "Centro Conroe", or empty when neither is named. */
    public String label() {
        return (arch.name() + " " + codename).trim();
    }
}
