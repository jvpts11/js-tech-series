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
 * Immutable specification of a GPU.
 *
 * <p>Its power is its cores, counted the way its design counts them, times the clock they run at, times the work its
 * design does per clock, in the same items per tick a processor's capacity is counted in. A card is declared with its
 * design on top, {@code new GpuSpec(...).on(Microarchitectures.KEPLER, "GK110", 875)}.
 *
 * @param clockMhz the clock the card's cores run at: its shaders' own clock on the designs that ran them faster than
 *                 the rest of the chip
 * @param arch     the design the card is built on
 * @param chip     the chip's codename, or empty
 */
public record GpuSpec(HardwareEra era,
                      PcieGeneration bus,
                      int cores,
                      int vramMb,
                      int tdpWatts,
                      int clockMhz,
                      Microarchitecture arch,
                      String chip) implements IExpansionCardSpec {

    /** What a card that says nothing of its clock is counted at. */
    private static final int UNSPECIFIED_MHZ = 1000;

    /* Megahertz and thousandths of a design's efficiency, divided out together after everything is multiplied. */
    private static final long SCALE = 1000L * 1000L;

    public GpuSpec {
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(arch, "arch must not be null");
        Objects.requireNonNull(chip, "chip may be empty but not missing");
        if (cores <= 0) {
            throw new IllegalArgumentException("cores must be > 0; got " + cores);
        }
        if (vramMb < 0) {
            throw new IllegalArgumentException("vramMb must be >= 0; got " + vramMb);
        }
        if (tdpWatts < 0) {
            throw new IllegalArgumentException("tdpWatts must be >= 0; got " + tdpWatts);
        }
        if (clockMhz <= 0) {
            throw new IllegalArgumentException("clockMhz must be > 0; got " + clockMhz);
        }
    }

    /** A card that says nothing of its design or its clock: a gigahertz, counted as the P6 is. */
    public GpuSpec(final HardwareEra era, final PcieGeneration bus, final int cores, final int vramMb,
                   final int tdpWatts) {
        this(era, bus, cores, vramMb, tdpWatts, UNSPECIFIED_MHZ, Microarchitectures.UNSPECIFIED, "");
    }

    /** The same card, built on that design and chip, its cores running at {@code mhz}. */
    public GpuSpec on(final Microarchitecture design, final String chipName, final int mhz) {
        return new GpuSpec(era, bus, cores, vramMb, tdpWatts, mhz, design, chipName);
    }

    public long threads() {
        return (long) cores * 4L;
    }

    /**
     * What the card can do, in items per tick: its cores as its design counts them, times their clock in gigahertz,
     * times the design's efficiency. Never below one: a card that lights a screen does some work.
     */
    public long power() {
        final long scaled = (long) arch.unitsOf(cores) * clockMhz * arch.efficiencyMilli();
        return Math.max(1L, (scaled + SCALE / 2L) / SCALE);
    }

    /** The design and the chip as a tooltip reads them, "Kepler GK110", or empty when neither is named. */
    public String designLabel() {
        return (arch.name() + " " + chip).trim();
    }

    @Override
    public ExpansionCardKind kind() {
        return ExpansionCardKind.GPU;
    }
}
