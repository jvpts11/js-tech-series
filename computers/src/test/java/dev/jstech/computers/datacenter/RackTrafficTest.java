/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datacenter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RackTrafficTest {

    private RackTraffic traffic;

    @BeforeEach
    void setUp() {
        traffic = new RackTraffic();
        traffic.read(0L);
    }

    @Test
    void read_givesWhatWasCountedAndStartsOver() {
        traffic.moved(5L, 64L);
        traffic.answered(5L, 11L);
        traffic.answered(6L, 9L);
        assertEquals(new RackTraffic.Sample(64L, 20L, 2), traffic.read(20L));
        assertEquals(RackTraffic.Sample.NONE, traffic.read(40L));
    }

    @Test
    void moved_afterALongUnreadStretch_startsOverFirst() {
        traffic.moved(5L, 64L);
        traffic.moved(500L, 8L);
        assertEquals(new RackTraffic.Sample(8L, 0L, 0), traffic.read(510L));
    }

    @Test
    void read_afterALongUnreadStretch_isNothing() {
        traffic.moved(5L, 64L);
        assertEquals(RackTraffic.Sample.NONE, traffic.read(500L));
    }

    @Test
    void sample_plus_addsEveryCount() {
        assertEquals(new RackTraffic.Sample(3L, 5L, 7),
                new RackTraffic.Sample(1L, 2L, 3).plus(new RackTraffic.Sample(2L, 3L, 4)));
    }
}
