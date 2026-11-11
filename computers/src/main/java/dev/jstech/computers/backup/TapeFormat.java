/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * The tape of each era, as the Tape Library of that era takes it: its real native capacity, held in items as a disk of
 * the tape's era weighs them, and how long it takes to wind from one end to the other. Cold storage, huge and slow: a
 * tape winds to where a backup lies before it reads or writes (computers/docs/design/programs.md).
 *
 * <p>Free of Minecraft so the winding and the room are tested without a world. The winding times are estimates of the
 * real drives' end-to-end times, to be weighed in play.
 */
public enum TapeFormat {

    /** The half-inch reel of the Vintage: 150 MB. */
    REEL(HardwareEra.VINTAGE, 150L, 60),
    /** The DLT cartridge of the Legacy: 40 GB. */
    DLT(HardwareEra.LEGACY, 2_560L, 90),
    /** The LTO-3 cartridge of the Transition: 400 GB. */
    LTO_3(HardwareEra.TRANSITION, 1_600L, 90),
    /** The LTO-6 cartridge of the Standard: 2.5 TB. */
    LTO_6(HardwareEra.STANDARD, 10_240L, 100),
    /** The LTO-9 cartridge of the Advanced: 18 TB. */
    LTO_9(HardwareEra.ADVANCED, 73_728L, 110);

    private final Era era;
    private final long capacityItems;
    private final int fullWindSeconds;

    /** Ticks in a second of the game. */
    private static final int TICKS_PER_SECOND = 20;

    TapeFormat(final Era era, final long capacityItems, final int fullWindSeconds) {
        this.era = era;
        this.capacityItems = capacityItems;
        this.fullWindSeconds = fullWindSeconds;
    }

    /** What the format is called, a name rather than a word: 9-track, DLT, LTO-3, LTO-6 and LTO-9. */
    public String label() {
        return this == REEL ? "9-track" : name().replace('_', '-');
    }

    /** The era the tape is of, which decides what an item weighs on it and which library takes it. */
    public Era era() {
        return era;
    }

    /** How many items the tape holds, as a disk of its era weighs them. */
    public long capacityItems() {
        return capacityItems;
    }

    /** The tape's room in megabytes: what its label says. */
    public long capacityMb() {
        return capacityItems * EraStorage.mbPerItem(era);
    }

    /**
     * The ticks the tape takes to wind from where its head is to {@code to}, both counted in items from its start:
     * the share of the whole tape between them, at the time a wind from one end to the other takes.
     */
    public long windTicks(final long from, final long to) {
        final long span = Math.min(capacityItems, Math.abs(to - from));
        return span * fullWindSeconds * TICKS_PER_SECOND / capacityItems;
    }

    /** The format a tape of {@code era} is, the newest era's for one past them all. */
    public static TapeFormat of(final Era era) {
        for (final TapeFormat format : values()) {
            if (format.era.equals(era)) {
                return format;
            }
        }
        return LTO_9;
    }
}
