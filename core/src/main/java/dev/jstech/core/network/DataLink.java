/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.tier.HardwareEra;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * One data line in one era: what a data cable is, with what it carries and how far a run of it reaches. It is also
 * what the slowest cable on a way between two machines is told by.
 *
 * <p>Speed is in items a tick, what an Operation crossing the cable moves at most; range is in cables, how long a run
 * of it can be before a router or a repeater renews it. The numbers are first estimates, to tune in play:
 *
 * <pre>
 *               Vintage    Legacy     Transition  Standard           Advanced
 *   access      4 / 32     16 / 48    64 / 64     128 / 80           256 / 96
 *   backbone    16 / 96    64 / 160   256 / 200   512 / 512 straight 2,048 / 1,024 straight
 *   long dist.  1 / 2,000  8 / 5,000  24 / 7,500  64 / 10,000        256 / 20,000
 *   HPC         none       none       1,024 / 24  2,048 / 32         8,192 / 48
 *   crafting    32 / 16, the same cable in every era
 * </pre>
 *
 * <p>The backbone's fibre, from the Standard on, runs only straight: an optical router is what turns it. The long
 * distance line runs between exactly two ends and never shares a block with another cable.
 *
 * <p>The crafting line has one cable for every era, so its link is always told by the first.
 *
 * @param line the line
 * @param era  the era of the cable; the first era for the crafting line, whatever era was asked for
 */
public record DataLink(DataLine line, HardwareEra era) {

    /** How many ends a run of a line that runs between two places has, and so how many cables a block of it joins. */
    public static final int ENDS = 2;

    /* Items a tick and range in cables, by line and era from Vintage to Advanced; none where the cable is missing. */
    private static final long[][] THROUGHPUT = {
            {4L, 16L, 64L, 128L, 256L},
            {16L, 64L, 256L, 512L, 2_048L},
            {1L, 8L, 24L, 64L, 256L},
            {0L, 0L, 1_024L, 2_048L, 8_192L},
            {32L, 32L, 32L, 32L, 32L},
    };
    private static final int[][] RANGE = {
            {32, 48, 64, 80, 96},
            {96, 160, 200, 512, 1_024},
            {2_000, 5_000, 7_500, 10_000, 20_000},
            {0, 0, 24, 32, 48},
            {16, 16, 16, 16, 16},
    };
    /* How far a peripheral cable reaches in each era, from Vintage to Advanced. */
    private static final int[] PERIPHERAL_RANGE = {8, 12, 14, 16, 20};

    public DataLink {
        Objects.requireNonNull(line, "line");
        Objects.requireNonNull(era, "era");
        if (line == DataLine.CRAFTING) {
            era = HardwareEra.VINTAGE;
        }
    }

    /** The link a cable of {@code lineId} in its {@code generation} is, or null when that is no data cable. */
    public static @Nullable DataLink of(final @Nullable String lineId, final int generation) {
        final DataLine line = DataLine.ofLineId(lineId);
        final HardwareEra era = HardwareEra.find(generation);
        return line == null || era == null ? null : new DataLink(line, era);
    }

    /** The link named {@code name} as {@link #serializedName()} writes it, or null when it names none. */
    public static @Nullable DataLink byName(final @Nullable String name) {
        if (name == null) {
            return null;
        }
        if (name.equals(DataLine.CRAFTING.serializedName())) {
            return new DataLink(DataLine.CRAFTING, HardwareEra.VINTAGE);
        }
        final int cut = name.lastIndexOf('_');
        if (cut <= 0) {
            return null;
        }
        final DataLine line = DataLine.byName(name.substring(0, cut));
        final HardwareEra era = eraNamed(name.substring(cut + 1));
        return line == null || era == null ? null : new DataLink(line, era);
    }

    /** How far a peripheral cable of {@code era} reaches, in cables; 0 for an era with none. */
    public static int peripheralRange(final HardwareEra era) {
        return era.id() < PERIPHERAL_RANGE.length ? PERIPHERAL_RANGE[era.id()] : 0;
    }

    /** The link's name: the line and the era, {@code access_legacy}; the crafting line's is its own. */
    public String serializedName() {
        return this.line == DataLine.CRAFTING ? this.line.serializedName()
                : this.line.serializedName() + "_" + this.era.serializedName();
    }

    /** The generation of the line the cable is. */
    public int generation() {
        return this.line.generation(this.era);
    }

    /** Whether the line has a cable in this era: HPC begins at the Transition, and no line goes past the Advanced. */
    public boolean exists() {
        return this.era.id() < THROUGHPUT[this.line.id()].length && THROUGHPUT[this.line.id()][this.era.id()] > 0;
    }

    /** Items a tick the cable carries; none for a cable that does not exist. */
    public long throughput() {
        return exists() ? THROUGHPUT[this.line.id()][this.era.id()] : 0L;
    }

    /** Cables a run of it reaches before it has to be renewed; none for a cable that does not exist. */
    public int range() {
        return exists() ? RANGE[this.line.id()][this.era.id()] : 0;
    }

    /** Whether it runs only straight, as the backbone's fibre does: an optical router is what turns it. */
    public boolean straight() {
        return this.line == DataLine.BACKBONE && this.era.id() >= HardwareEra.STANDARD.id();
    }

    /** Whether a run of it goes between exactly two ends and a block of it holds nothing else: the long distance. */
    public boolean betweenTwoEnds() {
        return this.line == DataLine.LONG_DISTANCE;
    }

    private static @Nullable HardwareEra eraNamed(final String name) {
        for (final HardwareEra era : HardwareEra.values()) {
            if (era.serializedName().equals(name)) {
                return era;
            }
        }
        return null;
    }
}
