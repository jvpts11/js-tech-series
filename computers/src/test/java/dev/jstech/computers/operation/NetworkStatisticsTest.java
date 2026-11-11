/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkStatisticsTest {

    private NetworkStatistics stats;

    private static final int SELECT = 1;
    private static final int CRAFT = 5;

    @BeforeEach
    void setUp() {
        stats = new NetworkStatistics();
    }

    @Test
    void summaries_countEachTypeWithItsFailuresAndMeans() {
        stats.record(100L, SELECT, false, 4, 10, 64L);
        stats.record(120L, SELECT, true, 8, 20, 16L);
        stats.record(130L, CRAFT, false, 0, 600, 8L);

        final List<NetworkStatistics.Summary> hour = stats.summaries(140L, NetworkStatistics.HOUR);

        assertEquals(List.of(new NetworkStatistics.Summary(SELECT, 2, 1, 6, 15, 80L),
                new NetworkStatistics.Summary(CRAFT, 1, 0, 0, 600, 8L)), hour);
        assertEquals(50, hour.getFirst().failedPercent());
    }

    @Test
    void summaries_lastHourForgetsWhatAnHourAgoSettled() {
        stats.record(0L, SELECT, false, 0, 0, 0L);
        stats.record(900L, SELECT, false, 0, 0, 0L);

        assertEquals(1, stats.summaries(1_500L, NetworkStatistics.HOUR).getFirst().count());
        assertEquals(2, stats.summaries(1_500L, NetworkStatistics.DAY).getFirst().count());
    }

    @Test
    void summaries_dayAndWeekReachBackTheirWholeHours() {
        stats.record(0L, CRAFT, false, 0, 0, 0L);
        stats.record(30_000L, CRAFT, false, 0, 0, 0L);

        final long now = 40_000L;
        assertEquals(1, stats.summaries(now, NetworkStatistics.DAY).getFirst().count());
        assertEquals(2, stats.summaries(now, NetworkStatistics.WEEK).getFirst().count());
    }

    @Test
    void summaries_weekForgetsWhatIsOlderThanSevenDays() {
        stats.record(0L, CRAFT, false, 0, 0, 0L);

        assertEquals(List.of(), stats.summaries(8L * NetworkStatistics.TICKS_PER_DAY, NetworkStatistics.WEEK));
    }

    @Test
    void peakToday_keepsTheMostAtOnceAndWhenUntilTheDayTurns() {
        stats.observe(1_000L, 3, 0L, 1L);
        stats.observe(2_000L, 7, 0L, 1L);
        stats.observe(3_000L, 5, 0L, 1L);

        assertEquals(7, stats.peakToday(3_500L));
        assertEquals(2_000L, stats.peakTodayAt(3_500L));
        assertEquals(0, stats.peakToday(NetworkStatistics.TICKS_PER_DAY + 10L));
    }

    @Test
    void graph_averagesTheShareUsedOverEachSampleOldestFirst() {
        for (long tick = 0L; tick < NetworkStatistics.SAMPLE_TICKS; tick++) {
            stats.observe(tick, 1, tick < 10L ? 100L : 0L, 100L);
        }

        final int[] graph = stats.graph(NetworkStatistics.SAMPLE_TICKS);

        assertEquals(NetworkStatistics.GRAPH_SAMPLES, graph.length);
        assertEquals(50, graph[NetworkStatistics.GRAPH_SAMPLES - 2]);
        assertEquals(0, graph[NetworkStatistics.GRAPH_SAMPLES - 1]);
    }

    @Test
    void graph_forgetsWhatIsOlderThanTwoHours() {
        stats.observe(0L, 1, 100L, 100L);

        final int[] graph = stats.graph(2L * NetworkStatistics.TICKS_PER_HOUR + NetworkStatistics.SAMPLE_TICKS);

        assertArrayEquals(new int[NetworkStatistics.GRAPH_SAMPLES], graph);
    }

    @Test
    void restore_bringsBackWhatWasSaved() {
        stats.record(5_000L, CRAFT, true, 40, 400, 32L);
        stats.observe(5_000L, 9, 50L, 100L);
        final NetworkStatistics back = new NetworkStatistics();

        back.restore(stats.saved());

        assertEquals(stats.summaries(5_100L, NetworkStatistics.DAY), back.summaries(5_100L, NetworkStatistics.DAY));
        assertEquals(9, back.peakToday(5_100L));
        assertArrayEquals(stats.graph(5_100L), back.graph(5_100L));
    }

    @Test
    void saved_ofNothingRestoresToNothing() {
        final NetworkStatistics back = new NetworkStatistics();

        back.restore(stats.saved());

        assertEquals(List.of(), back.summaries(0L, NetworkStatistics.WEEK));
        assertEquals(0, back.peakToday(0L));
    }
}
