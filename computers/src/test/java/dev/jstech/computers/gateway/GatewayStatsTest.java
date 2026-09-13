/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GatewayStatsTest {

    @Test
    void lastMinute_countsWhatWasServedInTheWindow() {
        final GatewayStats stats = new GatewayStats();
        stats.count(GatewayStats.Kind.CALL, 10);
        stats.count(GatewayStats.Kind.CALL, 350);
        stats.count(GatewayStats.Kind.OPERATION, 350);
        assertEquals(2, stats.lastMinute(GatewayStats.Kind.CALL, 400));
        assertEquals(1, stats.lastMinute(GatewayStats.Kind.OPERATION, 400));
        assertEquals(0, stats.lastMinute(GatewayStats.Kind.FILE, 400));
    }

    @Test
    void lastMinute_forgetsWhatIsOlderThanAMinute() {
        final GatewayStats stats = new GatewayStats();
        stats.count(GatewayStats.Kind.CALL, 10);
        assertEquals(1, stats.lastMinute(GatewayStats.Kind.CALL, 1_150));
        assertEquals(0, stats.lastMinute(GatewayStats.Kind.CALL, 1_250));
    }

    @Test
    void count_startsABucketOverWhenTheWindowWrapsOntoIt() {
        final GatewayStats stats = new GatewayStats();
        stats.count(GatewayStats.Kind.CALL, 10);
        stats.count(GatewayStats.Kind.CALL, 1_210);
        assertEquals(1, stats.lastMinute(GatewayStats.Kind.CALL, 1_220));
    }

    @Test
    void reset_clearsEverything() {
        final GatewayStats stats = new GatewayStats();
        stats.count(GatewayStats.Kind.FILE, 5);
        stats.reset();
        assertEquals(0, stats.lastMinute(GatewayStats.Kind.FILE, 5));
    }
}
