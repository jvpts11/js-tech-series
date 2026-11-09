/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datacenter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectionDiagnosticsTest {

    private SectionDiagnostics diagnostics;

    private static final int BASE = 38;

    @BeforeEach
    void setUp() {
        diagnostics = new SectionDiagnostics();
    }

    @Test
    void latencyTenths_averagesTheMinutesAnswers() {
        diagnostics.second(20L, new RackTraffic.Sample(100L, 40L, 10), BASE);
        diagnostics.second(40L, new RackTraffic.Sample(100L, 22L, 5), BASE);
        assertEquals(41, diagnostics.latencyTenths());
    }

    @Test
    void latencyTenths_noAnswers_isNone() {
        diagnostics.second(20L, RackTraffic.Sample.NONE, BASE);
        assertEquals(SectionDiagnostics.NONE, diagnostics.latencyTenths());
    }

    @Test
    void throughput_isItemsASecondOverTheSecondsGathered() {
        diagnostics.second(20L, new RackTraffic.Sample(1000L, 0L, 0), BASE);
        diagnostics.second(40L, new RackTraffic.Sample(3000L, 0L, 0), BASE);
        assertEquals(2000, diagnostics.throughput());
    }

    @Test
    void second_aFullMinute_addsOnePointToTheHour() {
        for (int i = 0; i < SectionDiagnostics.SECONDS; i++) {
            diagnostics.second(20L * i, new RackTraffic.Sample(60L, 50L, 10), BASE);
        }
        assertEquals(List.of(50), diagnostics.hourLatency());
        assertEquals(List.of(60), diagnostics.hourThroughput());
    }

    @Test
    void hour_keepsOnlyTheLastSixtyMinutes() {
        for (int minute = 0; minute < SectionDiagnostics.MINUTES + 5; minute++) {
            for (int i = 0; i < SectionDiagnostics.SECONDS; i++) {
                diagnostics.second(0L, new RackTraffic.Sample(minute, 0L, 0), BASE);
            }
        }
        final List<Integer> hour = diagnostics.hourThroughput();
        assertEquals(SectionDiagnostics.MINUTES, hour.size());
        assertEquals(5, hour.get(0));
        assertEquals(SectionDiagnostics.MINUTES + 4, hour.get(hour.size() - 1));
    }

    @Test
    void latencyOver_onlyPastTwiceTheBase() {
        diagnostics.second(20L, new RackTraffic.Sample(10L, 76L, 10), BASE);
        assertFalse(diagnostics.latencyOver(BASE));
        diagnostics.second(40L, new RackTraffic.Sample(10L, 200L, 10), BASE);
        assertTrue(diagnostics.latencyOver(BASE));
        assertFalse(diagnostics.latencyOver(0));
    }

    @Test
    void second_spikeThatEnds_isRememberedWithItsLength() {
        diagnostics.second(100L, new RackTraffic.Sample(10L, 200L, 10), BASE);
        assertEquals(SectionDiagnostics.NONE, diagnostics.lastOverEnded());
        for (int i = 1; i <= SectionDiagnostics.SECONDS; i++) {
            diagnostics.second(100L + 20L * i, new RackTraffic.Sample(10L, 10L, 10), BASE);
        }
        assertTrue(diagnostics.lastOverEnded() > 100L);
        assertEquals(diagnostics.lastOverEnded() - 100L, diagnostics.lastOverTicks());
    }

    @Test
    void settle_raisesOnlyNewAlertsAndKeepsTheirTime() {
        assertEquals(List.of("a", "b"), diagnostics.settle(10L, new LinkedHashSet<>(List.of("a", "b"))));
        assertEquals(List.of(), diagnostics.settle(30L, Set.of("a", "b")));
        assertEquals(10L, diagnostics.since("a"));
        assertEquals(List.of("c"), diagnostics.settle(50L, Set.of("a", "c")));
        assertEquals(SectionDiagnostics.NONE, diagnostics.since("b"));
        assertEquals(50L, diagnostics.since("c"));
    }

    @Test
    void restore_bringsBackTheHourTheAlertsAndTheLastSpike() {
        for (int i = 0; i < SectionDiagnostics.SECONDS; i++) {
            diagnostics.second(20L * i, new RackTraffic.Sample(120L, 40L, 10), BASE);
        }
        diagnostics.settle(5L, Set.of("budget"));
        final SectionDiagnostics.Saved saved = diagnostics.saved();
        final SectionDiagnostics back = new SectionDiagnostics();
        back.restore(saved);
        assertEquals(diagnostics.hourLatency(), back.hourLatency());
        assertEquals(diagnostics.hourThroughput(), back.hourThroughput());
        assertEquals(5L, back.since("budget"));
        assertEquals(Map.of("budget", 5L), back.saved().since());
    }
}
