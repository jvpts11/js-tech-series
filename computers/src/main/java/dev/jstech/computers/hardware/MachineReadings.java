/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

/**
 * What a machine read over the last game hour, the last game day and the last 7 game days, for its system monitors'
 * graphs (computers/docs/design/interface.md): what its parts draw, what it takes from the line, and its temperature,
 * each the mean of the readings in a sample; and the energy it took from the line in the game day.
 *
 * <p>One reading a second goes into a sample of each period: the hour in 50 samples, the day in 48, the week in 168. A
 * sample is emptied the first time the clock comes back to it, so a reading costs the same however long the machine
 * has run. Free of the game's types.
 */
public final class MachineReadings {

    private final Ring hour = new Ring(HOUR_SAMPLES, HOUR_TICKS / HOUR_SAMPLES);
    private final Ring day = new Ring(DAY_SAMPLES, DAY_TICKS / DAY_SAMPLES);
    private final Ring week = new Ring(WEEK_SAMPLES, HOUR_TICKS);
    private long today = Long.MIN_VALUE;
    private long joulesToday;

    /** What is read: what the parts draw and what the line gives, in watts, and the temperature, in °C. */
    public static final int DRAW = 0;
    public static final int LINE = 1;
    public static final int TEMPERATURE = 2;
    /** A sample with no reading in it, or a machine with no temperature of its own. */
    public static final int NONE = Integer.MIN_VALUE;
    /** How often a reading is taken, in ticks: a second. */
    public static final long SAMPLE_TICKS = 20L;
    /** The hours of each period: the last hour, the last day, the last 7 days. */
    public static final int HOUR = 1;
    public static final int DAY = 24;
    public static final int WEEK = 168;
    private static final int SERIES = 3;
    private static final int HOUR_SAMPLES = 50;
    private static final int DAY_SAMPLES = 48;
    private static final int WEEK_SAMPLES = 168;
    private static final long HOUR_TICKS = 1_000L;
    private static final long DAY_TICKS = 24_000L;
    private static final long SECONDS_PER_SAMPLE = SAMPLE_TICKS / 20L;

    /**
     * What is kept across a save: the clock, each period's sums and counts, and the day's energy.
     *
     * @param clock  the tick the readings were last brought to
     * @param sums   per period (hour, day, week), the sums of its samples, three series each
     * @param counts per period, how many readings each sum holds
     * @param today  the game day the energy is of
     * @param joules the energy taken from the line that day
     */
    public record Saved(long clock, long[][] sums, int[][] counts, long today, long joules) {
        public Saved {
            sums = new long[][] {sums[0].clone(), sums[1].clone(), sums[2].clone()};
            counts = new int[][] {counts[0].clone(), counts[1].clone(), counts[2].clone()};
        }
    }

    /**
     * Takes the reading of {@code now}: what the parts draw and what the line gives, in watts, and the temperature in
     * °C, or {@link #NONE} for a machine with none.
     */
    public void read(final long now, final int draw, final int line, final int celsius) {
        for (final Ring ring : new Ring[] {hour, day, week}) {
            ring.add(now, DRAW, draw);
            ring.add(now, LINE, line);
            if (celsius != NONE) {
                ring.add(now, TEMPERATURE, celsius);
            }
        }
        final long thisDay = Math.floorDiv(now, DAY_TICKS);
        if (thisDay != today) {
            today = thisDay;
            joulesToday = 0L;
        }
        joulesToday += Math.max(0L, line) * SECONDS_PER_SAMPLE;
    }

    /**
     * The means of {@code series} over the last {@code hours} ({@link #HOUR}, {@link #DAY} or {@link #WEEK}), one a
     * sample, oldest first; {@link #NONE} for a sample with no reading.
     */
    public int[] series(final long now, final int series, final int hours) {
        return (hours >= WEEK ? week : hours > HOUR ? day : hour).series(now, series);
    }

    /** The energy taken from the line in the game day of {@code now}, in joules. */
    public long joulesToday(final long now) {
        return Math.floorDiv(now, DAY_TICKS) == today ? joulesToday : 0L;
    }

    /** What to keep across a save. */
    public Saved saved() {
        return new Saved(Math.max(hour.clock, Math.max(day.clock, week.clock)),
                new long[][] {hour.sums, day.sums, week.sums}, new int[][] {hour.counts, day.counts, week.counts},
                today, joulesToday);
    }

    /** Takes back what a save kept; a period of the wrong size is left empty. */
    public void restore(final Saved saved) {
        final Ring[] rings = {hour, day, week};
        for (int i = 0; i < rings.length; i++) {
            rings[i].restore(saved.clock(), saved.sums()[i], saved.counts()[i]);
        }
        today = saved.today();
        joulesToday = saved.joules();
    }

    /** One period's samples, three series each, a sum and how many readings it holds. */
    private static final class Ring {
        private final long[] sums;
        private final int[] counts;
        private final int samples;
        private final long sampleTicks;
        private long clock = Long.MIN_VALUE;

        private Ring(final int samples, final long sampleTicks) {
            this.samples = samples;
            this.sampleTicks = sampleTicks;
            this.sums = new long[samples * SERIES];
            this.counts = new int[samples * SERIES];
        }

        private void add(final long now, final int series, final int value) {
            advance(now);
            final int at = slotOf(Math.floorDiv(now, sampleTicks)) * SERIES + series;
            sums[at] += value;
            counts[at]++;
        }

        private int[] series(final long now, final int series) {
            advance(now);
            final int[] out = new int[samples];
            final long current = Math.floorDiv(now, sampleTicks);
            for (int i = 0; i < samples; i++) {
                final int at = slotOf(current - (samples - 1) + i) * SERIES + series;
                out[i] = counts[at] == 0 ? NONE : (int) Math.round((double) sums[at] / counts[at]);
            }
            return out;
        }

        /* Empties every sample the clock passed since it was last brought forward, all of them after a full lap. */
        private void advance(final long now) {
            final long sample = Math.floorDiv(now, sampleTicks);
            final long last = clock == Long.MIN_VALUE ? sample : Math.floorDiv(clock, sampleTicks);
            for (long s = last + 1; s <= last + Math.min(Math.max(0L, sample - last), samples); s++) {
                final int at = slotOf(s) * SERIES;
                for (int k = 0; k < SERIES; k++) {
                    sums[at + k] = 0L;
                    counts[at + k] = 0;
                }
            }
            clock = Math.max(clock, now);
        }

        private int slotOf(final long sample) {
            return (int) Math.floorMod(sample, samples);
        }

        private void restore(final long savedClock, final long[] savedSums, final int[] savedCounts) {
            if (savedClock == Long.MIN_VALUE || savedSums.length != sums.length
                    || savedCounts.length != counts.length) {
                return;
            }
            System.arraycopy(savedSums, 0, sums, 0, sums.length);
            System.arraycopy(savedCounts, 0, counts, 0, counts.length);
            clock = savedClock;
        }
    }
}
