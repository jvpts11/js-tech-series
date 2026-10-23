/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class YieldTest {

    @Test
    void byHand_halvesWhatAMachineMultiplies() {
        assertEquals(new Yield(1, 1), new Yield(1, 2).byHand());
        assertEquals(new Yield(1, 2), new Yield(1, 4).byHand());
    }

    @Test
    void byHand_makesTwoOfThreeWhereAMachineMakesOneOfOne() {
        assertEquals(new Yield(3, 2), new Yield(1, 1).byHand());
    }

    @Test
    void byHand_takesMoreWhereAMachineTakesSeveral() {
        assertEquals(new Yield(6, 1), new Yield(4, 1).byHand());
    }

    @Test
    void byHand_alwaysYieldsLessThanTheMachine() {
        for (int in = 1; in <= 9; in++) {
            for (int out = 1; out <= 9; out++) {
                final Yield machine = new Yield(in, out);
                final Yield hand = machine.byHand();
                assertEquals(true, (double) hand.out() / hand.in() < (double) out / in, machine + " by hand " + hand);
            }
        }
    }

    @Test
    void yield_refusesNothingInOrOut() {
        assertThrows(IllegalArgumentException.class, () -> new Yield(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Yield(1, 0));
    }
}
