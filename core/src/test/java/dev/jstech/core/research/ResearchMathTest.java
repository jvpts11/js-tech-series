/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResearchMathTest {

    private static final double CLOSE = 1e-12;

    @Test
    void spent_countsOneForEachPartEntered() {
        assertEquals(0, ResearchMath.spent(0.0, 8));
        assertEquals(1, ResearchMath.spent(0.001, 8));
        assertEquals(1, ResearchMath.spent(0.125, 8));
        assertEquals(2, ResearchMath.spent(0.126, 8));
        assertEquals(8, ResearchMath.spent(1.0, 8));
    }

    @Test
    void advance_goesByThePointsWorkedAndSpendsTheDataOfEachPartEntered() {
        final ResearchMath.Step step = ResearchMath.advance(0.0, 250, 1_000, new int[] {8}, new int[] {8});
        assertEquals(0.25, step.share(), CLOSE);
        assertArrayEquals(new int[] {2}, step.spends());
        assertFalse(step.made());
    }

    @Test
    void advance_startsOnlyWithTheFirstDataHeld() {
        final ResearchMath.Step step = ResearchMath.advance(0.0, 100, 1_000, new int[] {4}, new int[] {0});
        assertEquals(0.0, step.share(), CLOSE);
        assertArrayEquals(new int[] {0}, step.spends());
    }

    @Test
    void advance_stopsAtTheEdgeOfTheLastPartItsDataPaysFor() {
        final ResearchMath.Step step = ResearchMath.advance(0.1, 900, 1_000, new int[] {4, 10}, new int[] {1, 10});
        assertEquals(0.5, step.share(), CLOSE, "two of four sample data cover half, one more is held");
        assertArrayEquals(new int[] {1, 4}, step.spends());
    }

    @Test
    void advance_needsNoDataOnceItsPartIsPaidFor() {
        final ResearchMath.Step step = ResearchMath.advance(0.3, 10, 1_000, new int[] {2}, new int[] {0});
        assertEquals(0.31, step.share(), CLOSE);
        assertArrayEquals(new int[] {0}, step.spends());
    }

    @Test
    void advance_endsAtTheWholeOfItAndNoFurther() {
        final ResearchMath.Step step = ResearchMath.advance(0.9, 5_000, 1_000, new int[] {10}, new int[] {5});
        assertEquals(1.0, step.share(), CLOSE);
        assertArrayEquals(new int[] {1}, step.spends());
        assertTrue(step.made());
    }

    @Test
    void advance_neverGoesBack() {
        final ResearchMath.Step step = ResearchMath.advance(0.5, -100, 1_000, new int[] {}, new int[] {});
        assertEquals(0.5, step.share(), CLOSE);
    }

    @Test
    void groupShares_givesTheStrongestWholeAndEachOtherItsShare() {
        assertArrayEquals(new double[] {8.1, 24.0, 0.9}, ResearchMath.groupShares(new double[] {9, 24, 1}, 0.9),
                CLOSE);
        assertEquals(33.0, ResearchMath.groupPower(new double[] {9, 24, 1}, 0.9), CLOSE);
    }

    @Test
    void groupShares_givesTheFirstOfTwoAsStrongItsWhole() {
        assertArrayEquals(new double[] {10.0, 9.0}, ResearchMath.groupShares(new double[] {10, 10}, 0.9), CLOSE);
        assertEquals(0.0, ResearchMath.groupPower(new double[] {}, 0.9), CLOSE);
    }

    @Test
    void groupShares_refusesANegativePowerOrShare() {
        assertThrows(IllegalArgumentException.class, () -> ResearchMath.groupShares(new double[] {-1}, 0.9));
        assertThrows(IllegalArgumentException.class, () -> ResearchMath.groupShares(new double[] {1}, 1.5));
    }

    @Test
    void advance_refusesACostOfNoPointsAndUnmatchedCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> ResearchMath.advance(0, 1, 0, new int[] {}, new int[] {}));
        assertThrows(IllegalArgumentException.class,
                () -> ResearchMath.advance(0, 1, 10, new int[] {1}, new int[] {}));
    }
}
