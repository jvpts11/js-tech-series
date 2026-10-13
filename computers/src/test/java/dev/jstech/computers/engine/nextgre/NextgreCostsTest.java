/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class NextgreCostsTest {

    @Test
    void bench_takesTheWorkAtTheComputersSpeedRoundedUp() {
        assertEquals(9L, NextgreCosts.bench(9, 9, 9));
        assertEquals(10L, NextgreCosts.bench(10, 9, 9));
        assertEquals(2L, NextgreCosts.bench(1, 9, 5));
    }

    @Test
    void bench_ofNoRunsTakesNothing() {
        assertEquals(0L, NextgreCosts.bench(0, 9, 9));
    }

    @Test
    void machine_splitsTheRunsOverItsLanes() {
        assertEquals(600L, NextgreCosts.machine(3, 200, 1));
        assertEquals(400L, NextgreCosts.machine(3, 200, 2));
    }

    @Test
    void pull_addsTheStoresLatencyToItsTransfer() {
        assertEquals(5L + 2L, NextgreCosts.pull(128, 64, 5));
        assertEquals(0L, NextgreCosts.pull(0, 64, 5));
    }

    @Test
    void makespan_onOneLaneIsTheSum() {
        assertEquals(60L, NextgreCosts.makespan(List.of(10L, 20L, 30L), 1));
    }

    @Test
    void makespan_onEnoughLanesIsTheLongest() {
        assertEquals(30L, NextgreCosts.makespan(List.of(10L, 20L, 30L), 3));
        assertEquals(30L, NextgreCosts.makespan(List.of(10L, 20L, 30L), 8));
    }

    @Test
    void makespan_handsTheLongestOutFirst() {
        // 30 alone, then 20+10 on the other lane: 30, not the 40 handing them out in order would give.
        assertEquals(30L, NextgreCosts.makespan(List.of(10L, 20L, 30L), 2));
    }

    @Test
    void node_beginsOnceWhatItIsMadeOfIsThere() {
        assertEquals(50L, NextgreCosts.node(20, List.of(10L, 30L), 2));
        assertEquals(60L, NextgreCosts.node(20, List.of(10L, 30L), 1));
    }
}
