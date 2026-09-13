/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProgramRandomTest {

    @Test
    void nextLong_drawsTheSplitMix64SequenceFromZero() {
        final ProgramRandom random = new ProgramRandom();
        assertEquals(0xE220A8397B1DCDAFL, random.nextLong());
        assertEquals(0x6E789E6AA1B965F4L, random.nextLong());
        assertEquals(0x06C45D188009454FL, random.nextLong());
    }

    @Test
    void next_staysUnderItsBoundAndReachesEveryValue() {
        final ProgramRandom random = new ProgramRandom();
        final Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            final int drawn = random.next(7);
            assertTrue(drawn >= 0 && drawn < 7, "drew " + drawn);
            seen.add(drawn);
        }
        assertEquals(7, seen.size(), "every value under the bound comes up");
    }

    @Test
    void next_givesZeroForABoundUnderOne() {
        final ProgramRandom random = new ProgramRandom();
        assertEquals(0, random.next(1));
        assertEquals(0, random.next(0));
        assertEquals(0, random.next(-5));
    }

    @Test
    void nextDouble_staysFromZeroUpToOne() {
        final ProgramRandom random = new ProgramRandom();
        for (int i = 0; i < 10_000; i++) {
            final double drawn = random.nextDouble();
            assertTrue(drawn >= 0.0 && drawn < 1.0, "drew " + drawn);
        }
    }

    @Test
    void startFrom_drawsTheSameSequenceFromTheSameSeed() {
        final ProgramRandom first = new ProgramRandom();
        final ProgramRandom second = new ProgramRandom();
        first.startFrom(2026);
        second.nextLong();
        second.startFrom(2026);
        for (int i = 0; i < 16; i++) {
            assertEquals(first.next(1_000_000), second.next(1_000_000));
        }
    }

    @Test
    void state_carriesTheSequenceOnInAnotherGenerator() {
        final ProgramRandom running = new ProgramRandom();
        for (int i = 0; i < 5; i++) {
            running.next(100);
        }
        final ProgramRandom readBack = new ProgramRandom();
        readBack.startFrom(running.state());
        for (int i = 0; i < 16; i++) {
            assertEquals(running.nextLong(), readBack.nextLong());
        }
    }
}
