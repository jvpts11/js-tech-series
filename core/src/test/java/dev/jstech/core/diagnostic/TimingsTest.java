/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TimingsTest {

    @Test
    void summaries_sayTheLastTheAverageAndTheLongest() {
        final Timings timings = new Timings();
        timings.record("grid", 100L);
        timings.record("grid", 300L);
        timings.record("grid", 200L);

        final Timings.Summary grid = timings.summaries().getFirst();

        assertEquals("grid", grid.section());
        assertEquals(200L, grid.lastNanos());
        assertEquals(200L, grid.averageNanos());
        assertEquals(300L, grid.maxNanos());
        assertEquals(3, grid.samples());
    }

    @Test
    void record_keepsOnlyTheLatestSamples() {
        final Timings timings = new Timings();
        timings.record("tick", 1_000_000L);
        for (int i = 0; i < Timings.SAMPLES; i++) {
            timings.record("tick", 10L);
        }

        final Timings.Summary tick = timings.summaries().getFirst();

        assertEquals(Timings.SAMPLES, tick.samples());
        assertEquals(10L, tick.maxNanos(), "the long first time has gone out of the window");
        assertEquals(10L, tick.averageNanos());
    }

    @Test
    void summaries_putTheLongestOnAverageFirst() {
        final Timings timings = new Timings();
        timings.record("quick", 5L);
        timings.record("slow", 500L);
        timings.record("middle", 50L);

        final List<String> order = timings.summaries().stream().map(Timings.Summary::section).toList();

        assertEquals(List.of("slow", "middle", "quick"), order);
    }

    @Test
    void record_takesANegativeTimeAsNothing() {
        final Timings timings = new Timings();
        timings.record("odd", -40L);

        assertEquals(0L, timings.summaries().getFirst().maxNanos());
    }

    @Test
    void clear_forgetsEverything() {
        final Timings timings = new Timings();
        timings.record("grid", 100L);
        timings.clear();

        assertTrue(timings.summaries().isEmpty());
    }
}
