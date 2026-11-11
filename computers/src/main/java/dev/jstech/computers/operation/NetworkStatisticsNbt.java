/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;

/**
 * A Mainframe's {@link NetworkStatistics} in its save, so the last 7 game days are still there after the world
 * reloads: the clock, each type with its hour's slices and its week's hours, the graph and the day's peak.
 */
public final class NetworkStatisticsNbt {

    private static final String CLOCK = "Clock";
    private static final String TYPES = "Types";
    private static final String TYPE = "Type";
    private static final String SLICES = "Slices";
    private static final String HOURS = "Hours";
    private static final String GRAPH = "Graph";
    private static final String PEAK = "Peak";

    private NetworkStatisticsNbt() {
    }

    /** {@code statistics} as a tag of its own. */
    public static CompoundTag save(final NetworkStatistics statistics) {
        final NetworkStatistics.Saved saved = statistics.saved();
        final CompoundTag tag = new CompoundTag();
        tag.putLong(CLOCK, saved.clock());
        final ListTag types = new ListTag();
        for (int i = 0; i < saved.types().length; i++) {
            final CompoundTag type = new CompoundTag();
            type.putInt(TYPE, saved.types()[i]);
            type.put(SLICES, new LongArrayTag(saved.slices()[i]));
            type.put(HOURS, new LongArrayTag(saved.hours()[i]));
            types.add(type);
        }
        tag.put(TYPES, types);
        tag.putIntArray(GRAPH, saved.graph());
        tag.put(PEAK, new LongArrayTag(saved.peak()));
        return tag;
    }

    /** Puts back into {@code statistics} what {@link #save} wrote; a tag without it leaves them empty. */
    public static void load(final NetworkStatistics statistics, final CompoundTag tag) {
        if (!tag.contains(CLOCK)) {
            return;
        }
        final ListTag types = tag.getList(TYPES, Tag.TAG_COMPOUND);
        final int[] ids = new int[types.size()];
        final long[][] slices = new long[types.size()][];
        final long[][] hours = new long[types.size()][];
        for (int i = 0; i < types.size(); i++) {
            final CompoundTag type = types.getCompound(i);
            ids[i] = type.getInt(TYPE);
            slices[i] = type.getLongArray(SLICES);
            hours[i] = type.getLongArray(HOURS);
        }
        statistics.restore(new NetworkStatistics.Saved(tag.getLong(CLOCK), ids, slices, hours,
                tag.getIntArray(GRAPH), tag.getLongArray(PEAK)));
    }
}
