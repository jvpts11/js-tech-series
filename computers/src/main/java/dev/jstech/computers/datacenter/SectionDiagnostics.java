/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datacenter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How a datacenter section has been doing (computers/docs/design/servers-and-racks.md): the latency of its servers'
 * answers and the items through it, over the last minute and minute by minute over the last hour; since when each of
 * its alerts has stood; and the last time its latency went over twice its base, and for how long.
 *
 * <p>Its router hands it one reading a second, the section's racks summed ({@link RackTraffic}). Times are game
 * ticks. Latencies are kept in tenths of a tick, so a reading of 3.8 ticks is 38.
 */
public final class SectionDiagnostics {

    // The last minute, a second to a slot, oldest overwritten first.
    private final long[] secondItems = new long[SECONDS];
    private final long[] secondWaited = new long[SECONDS];
    private final int[] secondAnswers = new int[SECONDS];
    private int secondCursor;
    private int secondsFilled;
    // The last hour, a minute to a slot: the minute's latency in tenths of a tick (NONE when nothing was answered) and
    // its items a second.
    private final int[] minuteLatency = new int[MINUTES];
    private final int[] minuteThroughput = new int[MINUTES];
    private int minuteCursor;
    private int minutesFilled;
    // The minute being gathered.
    private long minuteItems;
    private long minuteWaited;
    private int minuteAnswers;
    private int minuteSeconds;
    // Since when each standing alert has stood, by its key.
    private final Map<String, Long> since = new LinkedHashMap<>();
    // The latency over twice the base: when it went over, NONE when it is not; and when the last time ended and how
    // long it lasted, NONE when it never did.
    private long overSince = NONE;
    private long lastOverEnded = NONE;
    private long lastOverTicks;

    /** What stands for no value: no answer in a minute, no time at all. */
    public static final int NONE = -1;
    /** The seconds the last minute keeps, and the minutes the last hour does. */
    public static final int SECONDS = 60;
    public static final int MINUTES = 60;
    /** How many times its base the latency may be before it raises a warning. */
    public static final int OVER_BASE = 2;

    /**
     * Adds one second's reading of the section and moves the hour on at each minute's end.
     *
     * @param now       the game time of the reading
     * @param reading   what the section's racks did in that second
     * @param baseTenths the section's base latency, in tenths of a tick; 0 when it has no servers to tell
     */
    public void second(final long now, final RackTraffic.Sample reading, final int baseTenths) {
        secondItems[secondCursor] = reading.items();
        secondWaited[secondCursor] = reading.waitedTicks();
        secondAnswers[secondCursor] = reading.answers();
        secondCursor = (secondCursor + 1) % SECONDS;
        secondsFilled = Math.min(SECONDS, secondsFilled + 1);

        minuteItems += reading.items();
        minuteWaited += reading.waitedTicks();
        minuteAnswers += reading.answers();
        if (++minuteSeconds >= SECONDS) {
            minuteLatency[minuteCursor] = minuteAnswers == 0 ? NONE : tenths(minuteWaited, minuteAnswers);
            minuteThroughput[minuteCursor] = (int) Math.min(Integer.MAX_VALUE, minuteItems / SECONDS);
            minuteCursor = (minuteCursor + 1) % MINUTES;
            minutesFilled = Math.min(MINUTES, minutesFilled + 1);
            minuteItems = 0L;
            minuteWaited = 0L;
            minuteAnswers = 0;
            minuteSeconds = 0;
        }

        final boolean over = latencyOver(baseTenths);
        if (over && overSince == NONE) {
            overSince = now;
        } else if (!over && overSince != NONE) {
            lastOverEnded = now;
            lastOverTicks = now - overSince;
            overSince = NONE;
        }
    }

    /** The latency of the last minute's answers, in tenths of a tick; {@link #NONE} when nothing was answered. */
    public int latencyTenths() {
        long waited = 0L;
        long answers = 0L;
        for (int i = 0; i < SECONDS; i++) {
            waited += secondWaited[i];
            answers += secondAnswers[i];
        }
        return answers == 0L ? NONE : tenths(waited, answers);
    }

    /** The items a second through the section over the last minute, or the seconds of it gathered so far. */
    public int throughput() {
        long items = 0L;
        for (int i = 0; i < SECONDS; i++) {
            items += secondItems[i];
        }
        return secondsFilled == 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, items / secondsFilled);
    }

    /** Whether the last minute's latency is over twice {@code baseTenths}; never while there is no base or answer. */
    public boolean latencyOver(final int baseTenths) {
        final int latency = latencyTenths();
        return baseTenths > 0 && latency != NONE && latency > OVER_BASE * baseTenths;
    }

    /** The last hour's latency, a minute to an entry, the oldest first, in tenths of a tick; NONE for no answer. */
    public List<Integer> hourLatency() {
        return hour(minuteLatency);
    }

    /** The last hour's throughput, a minute to an entry, the oldest first, in items a second. */
    public List<Integer> hourThroughput() {
        return hour(minuteThroughput);
    }

    /**
     * Sets the alerts standing now, by their keys, keeping since when each that stood already has; returns the keys
     * that are new, which are the ones to tell.
     */
    public List<String> settle(final long now, final Set<String> standing) {
        since.keySet().retainAll(standing);
        final List<String> raised = new ArrayList<>();
        for (final String key : standing) {
            if (since.putIfAbsent(key, now) == null) {
                raised.add(key);
            }
        }
        return raised;
    }

    /** Since when the alert of {@code key} has stood, or {@link #NONE} when it does not. */
    public long since(final String key) {
        return since.getOrDefault(key, (long) NONE);
    }

    /** When the last time the latency went over twice its base ended; {@link #NONE} when it never did, or still is. */
    public long lastOverEnded() {
        return lastOverEnded;
    }

    /** How long the last time the latency went over twice its base lasted, in ticks. */
    public long lastOverTicks() {
        return lastOverTicks;
    }

    /** What is kept of it across a restart: the hour, the alerts' times and the last time the latency went over. */
    public Saved saved() {
        return new Saved(hourLatency(), hourThroughput(), Map.copyOf(since), lastOverEnded, lastOverTicks);
    }

    /** Takes back what {@link #saved()} kept; the last minute starts empty. */
    public void restore(final Saved saved) {
        minutesFilled = 0;
        minuteCursor = 0;
        final int count = Math.min(MINUTES, Math.min(saved.latency().size(), saved.throughput().size()));
        for (int i = 0; i < count; i++) {
            minuteLatency[minuteCursor] = saved.latency().get(i);
            minuteThroughput[minuteCursor] = saved.throughput().get(i);
            minuteCursor = (minuteCursor + 1) % MINUTES;
            minutesFilled++;
        }
        since.clear();
        since.putAll(saved.since());
        lastOverEnded = saved.lastOverEnded();
        lastOverTicks = saved.lastOverTicks();
    }

    /* The minutes of {@code ring} gathered so far, the oldest first. */
    private List<Integer> hour(final int[] ring) {
        final List<Integer> out = new ArrayList<>(minutesFilled);
        final int start = Math.floorMod(minuteCursor - minutesFilled, MINUTES);
        for (int i = 0; i < minutesFilled; i++) {
            out.add(ring[(start + i) % MINUTES]);
        }
        return out;
    }

    /* {@code waited} ticks over {@code answers} answers, in tenths of a tick, rounded to the nearest. */
    private static int tenths(final long waited, final long answers) {
        return (int) Math.min(Integer.MAX_VALUE, (waited * 10L + answers / 2L) / answers);
    }

    /**
     * What a section keeps of its diagnostics across a restart.
     *
     * @param latency       the hour's latencies, a minute each, the oldest first
     * @param throughput    the hour's throughputs, the same way
     * @param since         since when each standing alert has stood, by its key
     * @param lastOverEnded when the latency last stopped being over twice its base, or NONE
     * @param lastOverTicks how long it was over then
     */
    public record Saved(List<Integer> latency, List<Integer> throughput, Map<String, Long> since, long lastOverEnded,
                        long lastOverTicks) {

        public Saved {
            latency = List.copyOf(latency);
            throughput = List.copyOf(throughput);
            since = Map.copyOf(since);
        }
    }
}
