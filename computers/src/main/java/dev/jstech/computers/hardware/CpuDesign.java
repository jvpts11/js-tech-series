/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.Objects;

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
 */
public record CpuDesign(Microarchitecture arch, String codename, boolean smt, int efficiencyCores, int efficiencyMhz,
                        Microarchitecture efficiencyArch, IntegratedGraphics graphics) {

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
    }

    /** A chip of that design and codename, one thread a core and no efficiency cores. */
    public static CpuDesign of(final Microarchitecture arch, final String codename) {
        return new CpuDesign(arch, codename, false, 0, 0, Microarchitectures.UNSPECIFIED, IntegratedGraphics.NONE);
    }

    /** The same chip with two threads to each performance core. */
    public CpuDesign withSmt() {
        return new CpuDesign(arch, codename, true, efficiencyCores, efficiencyMhz, efficiencyArch, graphics);
    }

    /** The same chip with {@code count} efficiency cores of that design, running at their own clock. */
    public CpuDesign withEfficiencyCores(final int count, final int mhz, final Microarchitecture design) {
        return new CpuDesign(arch, codename, smt, count, mhz, design, graphics);
    }

    /** The same chip carrying those graphics on its die. */
    public CpuDesign withGraphics(final IntegratedGraphics onDie) {
        return new CpuDesign(arch, codename, smt, efficiencyCores, efficiencyMhz, efficiencyArch, onDie);
    }

    /** The design and the codename as a tooltip reads them, "Centro Conroe", or empty when neither is named. */
    public String label() {
        return (arch.name() + " " + codename).trim();
    }
}
