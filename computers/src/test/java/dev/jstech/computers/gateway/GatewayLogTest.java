/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GatewayLogTest {

    private static GatewayLog.Entry entry(final int n) {
        return new GatewayLog.Entry(n, "CC #" + n, "call " + n, "ok", GatewayLog.Tone.OK);
    }

    @Test
    void add_keepsTheNewestFirst() {
        final GatewayLog log = new GatewayLog();
        log.add(entry(1));
        log.add(entry(2));
        assertEquals(List.of(entry(2), entry(1)), log.entries());
        assertEquals(List.of(entry(2)), log.recent(1));
    }

    @Test
    void add_dropsTheOldestPastTheCapacity() {
        final GatewayLog log = new GatewayLog();
        for (int i = 1; i <= GatewayLog.CAPACITY + 5; i++) {
            log.add(entry(i));
        }
        assertEquals(GatewayLog.CAPACITY, log.size());
        assertEquals(entry(GatewayLog.CAPACITY + 5), log.entries().get(0));
        assertEquals(entry(6), log.entries().get(GatewayLog.CAPACITY - 1));
    }

    @Test
    void restore_takesTheSavedOrderBack() {
        final GatewayLog log = new GatewayLog();
        log.restore(List.of(entry(1), entry(2), entry(3)));
        assertEquals(List.of(entry(3), entry(2), entry(1)), log.entries());
    }

    @Test
    void clock_readsTheDayFromSixInTheMorning() {
        assertEquals("06:00:00", GatewayLog.clock(0));
        assertEquals("12:00:00", GatewayLog.clock(6_000));
        assertEquals("18:30:00", GatewayLog.clock(12_500));
        assertEquals("05:59:56", GatewayLog.clock(23_999));
        assertEquals("06:00:00", GatewayLog.clock(24_000));
    }
}
