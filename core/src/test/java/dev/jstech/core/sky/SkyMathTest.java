/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.sky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkyMathTest {

    private static final double CLOSE = 1e-9;

    @Test
    void celestial_putsNoonAtNoughtAndMidnightHalfway() {
        assertEquals(0, SkyMath.celestial(6_000), CLOSE, "the clock shows noon at 6,000");
        assertEquals(0.5, SkyMath.celestial(18_000), CLOSE, "and midnight at 18,000");
        assertEquals(SkyMath.celestial(6_000), SkyMath.celestial(30_000), CLOSE, "a day later it is noon again");
    }

    @Test
    void direction_isAUnitVectorOverheadAtNought() {
        final double[] overhead = SkyMath.direction(0, 0);
        assertEquals(1, overhead[1], CLOSE);
        final double[] leaning = SkyMath.direction(0.13, 37);
        assertEquals(1, leaning[0] * leaning[0] + leaning[1] * leaning[1] + leaning[2] * leaning[2], CLOSE);
        assertEquals(Math.PI, SkyMath.separation(SkyMath.direction(0, 0), SkyMath.direction(0.5, 0)), CLOSE,
                "half a turn round is the opposite side of the sky");
    }

    @Test
    void drift_movesABodyRoundOnceInItsOrbit() {
        assertEquals(0.75, SkyMath.drift(0.75, 0, 10), CLOSE, "a body of no orbit stays where it stands");
        assertEquals(1.25, SkyMath.drift(0.75, 2, 1), CLOSE, "half its orbit in half its days");
        assertEquals(0.5, SkyMath.turned(4, 2), CLOSE, "a surface turning once in four days is half turned in two");
        assertEquals(0, SkyMath.turned(0, 7), CLOSE, "one that never turns stays put");
    }

    @Test
    void coverage_followsTheOverlapOfTwoDiscs() {
        assertEquals(0, SkyMath.coverage(0.1, 0.1, 0.25), CLOSE, "apart, nothing is hidden");
        assertEquals(1, SkyMath.coverage(0.1, 0.12, 0), CLOSE, "a larger body over the middle hides it all");
        assertEquals(0.25, SkyMath.coverage(0.1, 0.05, 0), CLOSE, "a body half as wide hides a quarter");
        final double half = SkyMath.coverage(0.1, 0.1, 0.1);
        assertTrue(half > 0.35 && half < 0.45, "two discs a radius apart overlap by about two fifths; got " + half);
        assertTrue(SkyMath.coverage(0.1, 0.1, 0.05) > half, "the nearer, the more hidden");
    }

    @Test
    void litShare_isNewBesideTheSunAndFullOpposite() {
        assertEquals(0, SkyMath.litShare(0), CLOSE);
        assertEquals(1, SkyMath.litShare(Math.PI), CLOSE);
        assertEquals(0.5, SkyMath.litShare(Math.PI / 2), CLOSE, "a quarter round, half lit");
    }

    @Test
    void sunlight_fallsWithTheSquareOfTheDistance() {
        assertEquals(1, SkyMath.sunlight(1), CLOSE);
        assertEquals(0.25, SkyMath.sunlight(2), CLOSE);
        assertEquals(SkyMath.DIMMEST, SkyMath.sunlight(40), CLOSE, "Pluto's day is as dark as the dimmest shown");
        assertEquals(SkyMath.BRIGHTEST, SkyMath.sunlight(0.3), CLOSE, "and Mercury's as bright as the brightest");
    }

    @Test
    void eclipseLight_leavesTheCoronaAtTotality() {
        assertEquals(1, SkyMath.eclipseLight(0), CLOSE);
        assertEquals(SkyMath.TOTALITY, SkyMath.eclipseLight(1), CLOSE);
        assertEquals(1 - (1 - SkyMath.TOTALITY) / 2, SkyMath.eclipseLight(0.5), CLOSE);
    }
}
