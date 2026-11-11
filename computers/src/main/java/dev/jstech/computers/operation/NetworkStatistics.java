/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A network's Operations over the last game hour, the last game day and the last 7 game days, the most it ran at once
 * in the game day, and how much of its orchestration capacity it used over the last 2 game hours, for the Network
 * Manager's Stats tab (computers/docs/design/interface.md). The periods are the event log's: game time.
 *
 * <p>Everything is a counter per interval: the last hour in twenty slices of 50 ticks, the week in 168 game hours, the
 * graph in a hundred samples of 20 ticks. Recording costs the same however busy the network is, and an interval is
 * emptied the first time the clock comes back to it, so a quiet network costs nothing to keep current. Free of the
 * game's types, so the windows are tested on their own.
 */
public final class NetworkStatistics {

    private final Map<Integer, TypeRings> rings = new TreeMap<>();
    /* Each graph sample's sum of the share of capacity in use, a percent per tick watched. */
    private final int[] usedSums = new int[GRAPH_SAMPLES];
    /* The latest tick the clock was brought to; the least long before it ever was. */
    private long clock = Long.MIN_VALUE;
    private long lastSlice = Long.MIN_VALUE;
    private long lastHour = Long.MIN_VALUE;
    private long lastSample = Long.MIN_VALUE;
    private long peakDay = Long.MIN_VALUE;
    private int peak;
    private long peakAt;

    public static final long TICKS_PER_HOUR = 1_000L;
    public static final long TICKS_PER_DAY = 24_000L;
    /** The hours of each period: the last hour, the last day, the last 7 days. */
    public static final int HOUR = 1;
    public static final int DAY = 24;
    public static final int WEEK = 168;
    /** How many samples the capacity graph holds, oldest first, and how many ticks each one is. */
    public static final int GRAPH_SAMPLES = 100;
    public static final long SAMPLE_TICKS = 20L;
    private static final int SLICES = 20;
    private static final long SLICE_TICKS = TICKS_PER_HOUR / SLICES;
    /* What one interval keeps: how many settled, how many fell short, the ticks waited and run, what they moved. */
    private static final int COUNT = 0;
    private static final int SHORT = 1;
    private static final int WAITED = 2;
    private static final int RAN = 3;
    private static final int MOVED = 4;
    private static final int FIGURES = 5;
    private static final int PERCENT = 100;

    /**
     * One Operation type over a period.
     *
     * @param type        the Operation type id
     * @param count       how many settled in the period
     * @param failed      of those, how many delivered less than asked
     * @param averageWait the mean ticks one waited before it ran
     * @param averageRun  the mean ticks one ran
     * @param moved       what they moved together
     */
    public record Summary(int type, int count, int failed, int averageWait, int averageRun, long moved) {

        /** The share that fell short, in whole percent. */
        public int failedPercent() {
            return count == 0 ? 0 : (int) Math.round(100.0 * failed / count);
        }
    }

    /**
     * What is kept across a save: the clock it was last brought to, and for each type its last hour's slices and its
     * week's hours, five figures each.
     *
     * @param clock  the tick the statistics were last brought to
     * @param types  the Operation types, in order
     * @param slices per type, the last hour's slices, five figures each
     * @param hours  per type, the week's hours, five figures each
     * @param graph  the capacity graph's sums
     * @param peak   the game day's peak, its day and the tick it was reached
     */
    public record Saved(long clock, int[] types, long[][] slices, long[][] hours, int[] graph, long[] peak) {
        public Saved {
            types = types.clone();
            slices = deepCopy(slices);
            hours = deepCopy(hours);
            graph = graph.clone();
            peak = peak.clone();
        }
    }

    /** Records one settled Operation of {@code type} at {@code now}. */
    public void record(final long now, final int type, final boolean shortfall, final int waited, final int ran,
                       final long moved) {
        advance(now);
        final TypeRings ring = rings.computeIfAbsent(type, t -> new TypeRings());
        add(ring.slices[sliceOf(now)], shortfall, waited, ran, moved);
        add(ring.hours[hourOf(now)], shortfall, waited, ran, moved);
    }

    /**
     * Notes one tick of the orchestrator at work: how many Operations were in flight, for the day's peak, and the
     * capacity its queues were given out of what they had, for the graph.
     */
    public void observe(final long now, final int inFlight, final long used, final long total) {
        advance(now);
        final long day = Math.floorDiv(now, TICKS_PER_DAY);
        if (day != peakDay) {
            peakDay = day;
            peak = 0;
            peakAt = now;
        }
        if (inFlight > peak) {
            peak = inFlight;
            peakAt = now;
        }
        final int share = total <= 0L ? 0 : (int) Math.min(PERCENT, Math.max(0L, used) * PERCENT / total);
        usedSums[sampleOf(now)] += share;
    }

    /** Each type over the last {@code hours} ({@link #HOUR}, {@link #DAY} or {@link #WEEK}), quiet ones left out. */
    public List<Summary> summaries(final long now, final int hours) {
        advance(now);
        final List<Summary> out = new ArrayList<>();
        for (final Map.Entry<Integer, TypeRings> entry : rings.entrySet()) {
            final long[] sum = new long[FIGURES];
            if (hours <= HOUR) {
                for (final long[] slice : entry.getValue().slices) {
                    addInto(sum, slice);
                }
            } else {
                final long current = Math.floorDiv(now, TICKS_PER_HOUR);
                for (int back = 0; back < Math.min(hours, WEEK); back++) {
                    addInto(sum, entry.getValue().hours[(int) Math.floorMod(current - back, WEEK)]);
                }
            }
            if (sum[COUNT] > 0L) {
                out.add(new Summary(entry.getKey(), (int) sum[COUNT], (int) sum[SHORT],
                        (int) (sum[WAITED] / sum[COUNT]), (int) (sum[RAN] / sum[COUNT]), sum[MOVED]));
            }
        }
        return out;
    }

    /** The most Operations in flight at once in the game day of {@code now}. */
    public int peakToday(final long now) {
        return Math.floorDiv(now, TICKS_PER_DAY) == peakDay ? peak : 0;
    }

    /** The tick the game day's peak was reached, or {@code now} when nothing ran today. */
    public long peakTodayAt(final long now) {
        return Math.floorDiv(now, TICKS_PER_DAY) == peakDay && peak > 0 ? peakAt : now;
    }

    /** The share of capacity used in each sample of the last 2 game hours, in percent, oldest first. */
    public int[] graph(final long now) {
        advance(now);
        final int[] out = new int[GRAPH_SAMPLES];
        final long current = Math.floorDiv(now, SAMPLE_TICKS);
        for (int i = 0; i < GRAPH_SAMPLES; i++) {
            out[i] = (int) Math.min(PERCENT, usedSums[(int) Math.floorMod(current - (GRAPH_SAMPLES - 1) + i,
                    GRAPH_SAMPLES)] / SAMPLE_TICKS);
        }
        return out;
    }

    /** What to keep across a save. */
    public Saved saved() {
        final int[] types = new int[rings.size()];
        final long[][] slices = new long[rings.size()][];
        final long[][] hours = new long[rings.size()][];
        int i = 0;
        for (final Map.Entry<Integer, TypeRings> entry : rings.entrySet()) {
            types[i] = entry.getKey();
            slices[i] = flat(entry.getValue().slices);
            hours[i] = flat(entry.getValue().hours);
            i++;
        }
        return new Saved(clock, types, slices, hours, usedSums, new long[] {peakDay, peak, peakAt});
    }

    /** Takes back what a save kept; a part of the wrong length is left out. */
    public void restore(final Saved saved) {
        rings.clear();
        for (int i = 0; i < saved.types().length && i < saved.slices().length && i < saved.hours().length; i++) {
            final TypeRings ring = new TypeRings();
            if (unflat(saved.slices()[i], ring.slices) && unflat(saved.hours()[i], ring.hours)) {
                rings.put(saved.types()[i], ring);
            }
        }
        if (saved.graph().length == GRAPH_SAMPLES) {
            System.arraycopy(saved.graph(), 0, usedSums, 0, GRAPH_SAMPLES);
        }
        if (saved.peak().length == 3) {
            peakDay = saved.peak()[0];
            peak = (int) saved.peak()[1];
            peakAt = saved.peak()[2];
        }
        if (saved.clock() != Long.MIN_VALUE) {
            clock = saved.clock();
            lastSlice = Math.floorDiv(clock, SLICE_TICKS);
            lastHour = Math.floorDiv(clock, TICKS_PER_HOUR);
            lastSample = Math.floorDiv(clock, SAMPLE_TICKS);
        }
    }

    private static void add(final long[] interval, final boolean shortfall, final int waited, final int ran,
                            final long moved) {
        interval[COUNT]++;
        if (shortfall) {
            interval[SHORT]++;
        }
        interval[WAITED] += Math.max(0, waited);
        interval[RAN] += Math.max(0, ran);
        interval[MOVED] += Math.max(0L, moved);
    }

    private static void addInto(final long[] sum, final long[] interval) {
        for (int i = 0; i < FIGURES; i++) {
            sum[i] += interval[i];
        }
    }

    private static int sliceOf(final long tick) {
        return (int) Math.floorMod(Math.floorDiv(tick, SLICE_TICKS), SLICES);
    }

    private static int hourOf(final long tick) {
        return (int) Math.floorMod(Math.floorDiv(tick, TICKS_PER_HOUR), WEEK);
    }

    private static int sampleOf(final long tick) {
        return (int) Math.floorMod(Math.floorDiv(tick, SAMPLE_TICKS), GRAPH_SAMPLES);
    }

    /*
     * Brings the clock to {@code now}, emptying every interval it passed since the last call: all of them when more
     * than their whole ring went by, so a long quiet spell costs no more than a full lap.
     */
    private void advance(final long now) {
        clock = Math.max(clock, now);
        final long slice = Math.floorDiv(now, SLICE_TICKS);
        if (lastSlice != Long.MIN_VALUE && slice > lastSlice) {
            for (long s = lastSlice + 1; s <= lastSlice + Math.min(slice - lastSlice, SLICES); s++) {
                for (final TypeRings ring : rings.values()) {
                    ring.slices[(int) Math.floorMod(s, SLICES)] = new long[FIGURES];
                }
            }
        }
        lastSlice = Math.max(slice, lastSlice);
        final long hour = Math.floorDiv(now, TICKS_PER_HOUR);
        if (lastHour != Long.MIN_VALUE && hour > lastHour) {
            for (long h = lastHour + 1; h <= lastHour + Math.min(hour - lastHour, WEEK); h++) {
                for (final TypeRings ring : rings.values()) {
                    ring.hours[(int) Math.floorMod(h, WEEK)] = new long[FIGURES];
                }
            }
        }
        lastHour = Math.max(hour, lastHour);
        final long sample = Math.floorDiv(now, SAMPLE_TICKS);
        if (lastSample != Long.MIN_VALUE && sample > lastSample) {
            for (long s = lastSample + 1; s <= lastSample + Math.min(sample - lastSample, GRAPH_SAMPLES); s++) {
                usedSums[(int) Math.floorMod(s, GRAPH_SAMPLES)] = 0;
            }
        }
        lastSample = Math.max(sample, lastSample);
    }

    private static long[] flat(final long[][] intervals) {
        final long[] out = new long[intervals.length * FIGURES];
        for (int i = 0; i < intervals.length; i++) {
            System.arraycopy(intervals[i], 0, out, i * FIGURES, FIGURES);
        }
        return out;
    }

    private static boolean unflat(final long[] flat, final long[][] into) {
        if (flat.length != into.length * FIGURES) {
            return false;
        }
        for (int i = 0; i < into.length; i++) {
            System.arraycopy(flat, i * FIGURES, into[i], 0, FIGURES);
        }
        return true;
    }

    private static long[][] deepCopy(final long[][] arrays) {
        final long[][] out = new long[arrays.length][];
        for (int i = 0; i < arrays.length; i++) {
            out[i] = arrays[i].clone();
        }
        return out;
    }

    /** One type's intervals: the last hour's slices and the week's hours. */
    private static final class TypeRings {
        private final long[][] slices = new long[SLICES][FIGURES];
        private final long[][] hours = new long[WEEK][FIGURES];
    }
}
