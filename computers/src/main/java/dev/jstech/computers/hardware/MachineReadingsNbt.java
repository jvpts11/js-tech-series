/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;

/**
 * A machine's {@link MachineReadings} in its save, so its graphs of the last 7 game days are still there after the
 * world reloads: the clock, each period's sums and counts, and the day's energy.
 */
public final class MachineReadingsNbt {

    private static final String CLOCK = "Clock";
    private static final String[] PERIODS = {"Hour", "Day", "Week"};
    private static final String SUMS = "Sums";
    private static final String COUNTS = "Counts";
    private static final String TODAY = "Today";
    private static final String JOULES = "Joules";

    private MachineReadingsNbt() {
    }

    /** Writes {@code readings} into {@code tag}. */
    public static void save(final MachineReadings readings, final CompoundTag tag) {
        final MachineReadings.Saved saved = readings.saved();
        tag.putLong(CLOCK, saved.clock());
        for (int i = 0; i < PERIODS.length; i++) {
            tag.put(PERIODS[i] + SUMS, new LongArrayTag(saved.sums()[i]));
            tag.putIntArray(PERIODS[i] + COUNTS, saved.counts()[i]);
        }
        tag.putLong(TODAY, saved.today());
        tag.putLong(JOULES, saved.joules());
    }

    /** Puts back into {@code readings} what {@link #save} wrote; a tag without it leaves them empty. */
    public static void load(final MachineReadings readings, final CompoundTag tag) {
        if (!tag.contains(CLOCK)) {
            return;
        }
        final long[][] sums = new long[PERIODS.length][];
        final int[][] counts = new int[PERIODS.length][];
        for (int i = 0; i < PERIODS.length; i++) {
            sums[i] = tag.getLongArray(PERIODS[i] + SUMS);
            counts[i] = tag.getIntArray(PERIODS[i] + COUNTS);
        }
        readings.restore(new MachineReadings.Saved(tag.getLong(CLOCK), sums, counts, tag.getLong(TODAY),
                tag.getLong(JOULES)));
    }
}
