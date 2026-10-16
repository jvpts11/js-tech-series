/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlackbodyTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void glow_isNoneUpToTheDraperPoint() {
        assertEquals(0.0, Blackbody.glow(300.0), EPSILON);
        assertEquals(0.0, Blackbody.glow(Blackbody.DRAPER_POINT), EPSILON);
        assertFalse(Blackbody.glows(Blackbody.DRAPER_POINT));
        assertTrue(Blackbody.glows(Blackbody.DRAPER_POINT + 1.0));
    }

    @Test
    void glow_growsToTheWholeOnceWhiteHot() {
        final double half = (Blackbody.DRAPER_POINT + Blackbody.FULL_GLOW) / 2;
        assertEquals(0.5, Blackbody.glow(half), EPSILON);
        assertEquals(1.0, Blackbody.glow(Blackbody.FULL_GLOW), EPSILON);
        assertEquals(1.0, Blackbody.glow(6_000.0), EPSILON);
    }

    @Test
    void colour_isRedWhereItFirstGlows() {
        final int colour = Blackbody.colour(1_000.0);
        assertEquals(255, red(colour), "its brightest channel is red");
        assertTrue(green(colour) < red(colour) / 2, "with little green: " + Integer.toHexString(colour));
        assertTrue(blue(colour) <= green(colour), "and less blue: " + Integer.toHexString(colour));
    }

    @Test
    void colour_goesOrangeThenYellowAsItHeats() {
        final int orange = Blackbody.colour(2_000.0);
        final int yellow = Blackbody.colour(3_500.0);
        assertTrue(green(orange) > green(Blackbody.colour(1_000.0)), "more green at 2,000 K");
        assertTrue(green(yellow) > green(orange), "and more again at 3,500 K");
        assertTrue(blue(yellow) < green(yellow), "still short of blue");
    }

    @Test
    void colour_isNearlyWhiteAtDaylightsTemperature() {
        final int colour = Blackbody.colour(6_500.0);
        assertTrue(red(colour) > 235 && green(colour) > 235 && blue(colour) > 235,
                "white: " + Integer.toHexString(colour));
    }

    @Test
    void colour_turnsBlueHotterStill() {
        final int colour = Blackbody.colour(15_000.0);
        assertEquals(255, blue(colour), "blue is its brightest: " + Integer.toHexString(colour));
        assertTrue(red(colour) < blue(colour));
    }

    @Test
    void colour_isOpaqueAndHeldAtTheEndsOfItsTable() {
        assertEquals(0xFF, Blackbody.colour(1_200.0) >>> 24);
        assertEquals(Blackbody.colour(20_000.0), Blackbody.colour(90_000.0));
        assertEquals(Blackbody.colour(500.0), Blackbody.colour(10.0));
    }

    private static int red(final int colour) {
        return colour >> 16 & 0xFF;
    }

    private static int green(final int colour) {
        return colour >> 8 & 0xFF;
    }

    private static int blue(final int colour) {
        return colour & 0xFF;
    }
}
