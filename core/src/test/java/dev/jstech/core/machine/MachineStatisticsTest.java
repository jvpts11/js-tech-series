/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MachineStatisticsTest {

    private MachineStatistics statistics;

    private static final int TICKS = MachineStatistics.TICKS_A_SECOND;

    @BeforeEach
    void setUp() {
        statistics = new MachineStatistics();
    }

    @Test
    void efficiency_isNothingBeforeItsFirstSecond() {
        statistics.tick(true, 10L);
        assertEquals(0, statistics.efficiency());
    }

    @Test
    void efficiency_countsOnlyTheSecondsItHasRun() {
        seconds(10, TICKS, 5L, 0);
        assertEquals(100, statistics.efficiency());
        seconds(10, 0, 0L, 0);
        assertEquals(50, statistics.efficiency());
    }

    @Test
    void efficiency_looksBackOneMinuteOnly() {
        seconds(MachineStatistics.SECONDS_A_MINUTE, 0, 0L, 0);
        seconds(30, TICKS, 5L, 0);
        assertEquals(50, statistics.efficiency());
        assertEquals(30 * TICKS, statistics.workedTicks());
        seconds(MachineStatistics.SECONDS_A_MINUTE, TICKS, 5L, 0);
        assertEquals(100, statistics.efficiency());
    }

    @Test
    void second_takesAReadingEveryTwelveSeconds() {
        seconds(MachineStatistics.SAMPLE_SECONDS - 1, TICKS, 4L, 30);
        assertTrue(statistics.made().isEmpty());
        seconds(1, TICKS, 4L, 30);
        assertEquals(List.of(30), statistics.made());
        assertEquals(List.of(4L), statistics.power());
    }

    @Test
    void power_isWhatItDrewATickOnTheWhole() {
        for (int i = 0; i < MachineStatistics.SAMPLE_SECONDS; i++) {
            for (int t = 0; t < TICKS; t++) {
                statistics.tick(true, t % 2 == 0 ? 10L : 0L);
            }
            statistics.second(0);
        }
        assertEquals(List.of(5L), statistics.power());
    }

    @Test
    void made_keepsTheLastFiftyReadingsOldestFirst() {
        for (int reading = 0; reading < MachineStatistics.SAMPLES + 3; reading++) {
            seconds(MachineStatistics.SAMPLE_SECONDS, TICKS, 1L, reading);
        }
        final List<Integer> made = statistics.made();
        assertEquals(MachineStatistics.SAMPLES, made.size());
        assertEquals(3, made.getFirst());
        assertEquals(MachineStatistics.SAMPLES + 2, made.getLast());
    }

    @Test
    void stoppedBy_isForgottenAfterTenMinutes() {
        statistics.stopped("waiting", 1_000L);
        assertEquals("waiting", statistics.stoppedBy(1_000L + MachineStatistics.REMEMBERED));
        assertEquals(30, statistics.stoppedSecondsAgo(1_000L + 30 * TICKS));
        assertEquals("", statistics.stoppedBy(1_001L + MachineStatistics.REMEMBERED));
    }

    @Test
    void stoppedBy_isEmptyWhenNothingStoppedIt() {
        assertEquals("", statistics.stoppedBy(0L));
    }

    /* Runs {@code count} seconds, working {@code worked} ticks of each and drawing {@code drawn} a tick. */
    private void seconds(final int count, final int worked, final long drawn, final int perMinute) {
        for (int s = 0; s < count; s++) {
            for (int t = 0; t < TICKS; t++) {
                statistics.tick(t < worked, drawn);
            }
            statistics.second(perMinute);
        }
    }
}
