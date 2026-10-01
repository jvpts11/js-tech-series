/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How long named pieces of work took: each time a piece runs, its time is kept among the last
 * {@value #SAMPLES} of it, and a summary says the last, the average and the longest of those. It knows nothing of the
 * game, so anything can be timed with it, and a summary is cheap enough to read every frame.
 */
public final class Timings {

    private final Map<String, Section> sections = new ConcurrentHashMap<>();

    /** How many of the latest times of each piece are kept. */
    public static final int SAMPLES = 100;

    /** Keeps that {@code section} took {@code nanos} nanoseconds this time. */
    public void record(final String section, final long nanos) {
        this.sections.computeIfAbsent(section, name -> new Section()).add(Math.max(0L, nanos));
    }

    /** A summary of each piece timed, the longest on average first. */
    public List<Summary> summaries() {
        final List<Summary> out = new ArrayList<>(this.sections.size());
        this.sections.forEach((name, section) -> out.add(section.summary(name)));
        out.sort(Comparator.comparingLong(Summary::averageNanos).reversed().thenComparing(Summary::section));
        return out;
    }

    /** Forgets every time kept. */
    public void clear() {
        this.sections.clear();
    }

    /**
     * What the latest times of one piece came to.
     *
     * @param section      the piece's name
     * @param lastNanos    the latest time
     * @param averageNanos the average of the times kept
     * @param maxNanos     the longest of the times kept
     * @param samples      how many times are kept
     */
    public record Summary(String section, long lastNanos, long averageNanos, long maxNanos, int samples) {
    }

    /* The latest times of one piece, in a ring. */
    private static final class Section {

        private final long[] ring = new long[SAMPLES];
        private int next;
        private int count;

        private synchronized void add(final long nanos) {
            this.ring[this.next] = nanos;
            this.next = (this.next + 1) % SAMPLES;
            this.count = Math.min(SAMPLES, this.count + 1);
        }

        private synchronized Summary summary(final String name) {
            long sum = 0L;
            long max = 0L;
            for (int i = 0; i < this.count; i++) {
                sum += this.ring[i];
                max = Math.max(max, this.ring[i]);
            }
            final long last = this.ring[Math.floorMod(this.next - 1, SAMPLES)];
            return new Summary(name, last, this.count == 0 ? 0L : sum / this.count, max, this.count);
        }
    }
}
