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
 * <p>The architecture is a field of its own rather than a reading of the era, because the two are separate
 * questions: the era says when the chip was made and the architecture says what it understands. They line up for
 * this mod's own processors, and the shorter constructor is the one that says so.
 */
public record CpuSpec(HardwareEra era,
                      ArchitectureSpec architecture,
                      CpuSocketId socket,
                      int cores,
                      int freqMhz,
                      int tdpWatts,
                      boolean alien) {

    private static final int CAPACITY_FACTOR = 40;

    public CpuSpec {
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(architecture, "architecture must not be null");
        Objects.requireNonNull(socket, "socket must not be null");
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

    /** A processor of this mod's own hardware, built on the architecture its era is made of. */
    public CpuSpec(final HardwareEra era, final CpuSocketId socket, final int cores, final int freqMhz,
                   final int tdpWatts, final boolean alien) {
        this(era, Architectures.of(era), socket, cores, freqMhz, tdpWatts, alien);
    }

    public long orchestrationCapacity() {
        final long multiplier = alien ? 3L : 1L;
        final long scaled = (long) cores * freqMhz * CAPACITY_FACTOR * multiplier;
        return (scaled + 500L) / 1000L; // round to nearest items/tick
    }
}
