/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TranslatedPaceTest {

    @Test
    void share_atHalfSpeed_isHalfTheGrant() {
        assertEquals(32, new TranslatedPace(50).share(64));
    }

    /** However a tick is cut into rounds, a program at half speed gets through half of what it was dealt. */
    @Test
    void share_keepsWhatAGrantLeavesOverForTheNext() {
        final TranslatedPace pace = new TranslatedPace(50);
        int ran = 0;
        for (int round = 0; round < 10; round++) {
            ran += pace.share(1);
        }
        assertEquals(5, ran);
    }

    @Test
    void share_atFourFifths_addsUpOverRounds() {
        final TranslatedPace pace = new TranslatedPace(80);
        int ran = 0;
        for (int round = 0; round < 100; round++) {
            ran += pace.share(7);
        }
        assertEquals(560, ran);
    }

    @Test
    void charged_aProgramThatRanItsWholeShare_isChargedTheWholeGrant() {
        final TranslatedPace pace = new TranslatedPace(50);
        final int share = pace.share(64);
        assertEquals(64, pace.charged(64, share, share));
    }

    /** One that stopped short pays for what it ran, at the translator's price, and the rest goes to the others. */
    @Test
    void charged_aProgramThatStoppedShort_paysForWhatItRan() {
        final TranslatedPace pace = new TranslatedPace(50);
        final int share = pace.share(64);
        assertEquals(20, pace.charged(64, share, 10));
        assertEquals(0, pace.charged(64, share, 0), "nothing run costs nothing");
    }

    /** Seven of eight instructions at four fifths cost eight and three quarters, which is nine whole ones. */
    @Test
    void charged_roundsUpToWholeInstructions() {
        final TranslatedPace pace = new TranslatedPace(80);
        final int share = pace.share(10);
        assertEquals(8, share);
        assertEquals(9, pace.charged(10, share, 7));
    }

    @Test
    void construct_aPaceOutsideOneToAHundred_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TranslatedPace(0));
        assertThrows(IllegalArgumentException.class, () -> new TranslatedPace(101));
    }
}
