/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IEasingTest {

    private static final double CLOSE = 1e-4;

    @Test
    void named_everyCurveStartsAtNoughtAndEndsAtOne() {
        for (final IEasing curve : IEasing.NAMED.values()) {
            assertEquals(0.0, curve.apply(0.0), CLOSE, curve.written());
            assertEquals(1.0, curve.apply(1.0), CLOSE, curve.written());
        }
    }

    @Test
    void named_findsTheCurvesByNameWhateverTheCase() {
        assertSame(IEasing.LINEAR, IEasing.named("linear"));
        assertSame(IEasing.NAMED.get("ease-out-expo"), IEasing.named("  Ease-Out-Expo "));
    }

    @Test
    void named_readsACubicBezierWrittenAsCssWritesIt() {
        final IEasing curve = IEasing.named("cubic-bezier(0, 0, 0, 1)");
        assertInstanceOf(IEasing.CubicBezier.class, curve);
        assertEquals(curve, IEasing.named(curve.written()));
    }

    @Test
    void nameOf_writesAKnownCurveByItsNameAndAnyOtherByItsNumbers() {
        assertEquals("ease-out-expo", IEasing.nameOf(IEasing.named("cubic-bezier(0.19, 1.0, 0.22, 1.0)")));
        assertEquals("steps(3)", IEasing.nameOf(new IEasing.Steps(3)));
        assertEquals("linear", IEasing.nameOf(IEasing.LINEAR));
    }

    @Test
    void named_readsSteps() {
        assertEquals(new IEasing.Steps(16), IEasing.named("steps(16)"));
    }

    @Test
    void named_refusesWhatNamesNoCurve() {
        assertThrows(IllegalArgumentException.class, () -> IEasing.named("bouncy"));
        assertThrows(IllegalArgumentException.class, () -> IEasing.named("cubic-bezier(0, 0, 1)"));
        assertThrows(IllegalArgumentException.class, () -> IEasing.named("cubic-bezier(a, 0, 1, 1)"));
        assertThrows(IllegalArgumentException.class, () -> IEasing.named("cubic-bezier(0, 1e999, 1, 1)"));
        assertThrows(IllegalArgumentException.class, () -> IEasing.named("steps(x)"));
        assertThrows(IllegalArgumentException.class, () -> IEasing.named("steps(0)"));
    }

    @Test
    void linear_isTimeItselfHeldBetweenNoughtAndOne() {
        assertEquals(0.25, IEasing.LINEAR.apply(0.25), CLOSE);
        assertEquals(0.0, IEasing.LINEAR.apply(-1.0), CLOSE);
        assertEquals(1.0, IEasing.LINEAR.apply(2.0), CLOSE);
    }

    @Test
    void cubicBezier_easeOutRunsAheadOfTimeAndEaseInBehind() {
        assertTrue(IEasing.named("ease-out").apply(0.3) > 0.3);
        assertTrue(IEasing.named("ease-in").apply(0.3) < 0.3);
    }

    @Test
    void cubicBezier_matchesAKnownPointOfTheCssEaseCurve() {
        // CSS's "ease" is a little past 0.8 halfway through its time.
        assertEquals(0.8024, IEasing.named("ease").apply(0.5), 1e-3);
    }

    @Test
    void cubicBezier_neverGoesBackwards() {
        for (final IEasing curve : IEasing.NAMED.values()) {
            double last = 0.0;
            for (int i = 1; i <= 100; i++) {
                final double at = curve.apply(i / 100.0);
                assertTrue(at >= last - CLOSE, curve.written() + " went back at " + i);
                last = at;
            }
        }
    }

    @Test
    void steps_jumpsInEqualStepsAndLandsAtTheEnd() {
        final IEasing four = new IEasing.Steps(4);
        assertEquals(0.0, four.apply(0.2), CLOSE);
        assertEquals(0.25, four.apply(0.3), CLOSE);
        assertEquals(0.75, four.apply(0.99), CLOSE);
        assertEquals(1.0, four.apply(1.0), CLOSE);
    }
}
