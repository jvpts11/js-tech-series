/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SphereMapTest {

    /* The game's default Overworld border, from west to east. */
    private static final double OVERWORLD = 59_999_968;
    private static final double MOON = 3_474.8;
    private static final double MARS = 6_779;

    @Test
    void widthOf_isTheOverworldsTimesTheDiameterOverTheEarths() {
        assertEquals(0.27, SphereMap.widthOf(OVERWORLD, MOON) / OVERWORLD, 0.005);
        assertEquals(0.53, SphereMap.widthOf(OVERWORLD, MARS) / OVERWORLD, 0.005);
    }

    @Test
    void widthOf_neverPassesTheOverworlds() {
        assertEquals(OVERWORLD, SphereMap.widthOf(OVERWORLD, SphereMap.EARTH_DIAMETER * 11), 1e-6);
    }

    @Test
    void longitudeAndLatitude_reachTheFarSideAndThePolesAtTheEnds() {
        final double width = SphereMap.widthOf(OVERWORLD, MOON);
        assertEquals(180, SphereMap.longitude(width / 2, width), 1e-9);
        assertEquals(-180, SphereMap.longitude(-width / 2, width), 1e-9);
        assertEquals(90, SphereMap.latitude(-width / 4, width), 1e-9);
        assertEquals(-90, SphereMap.latitude(width / 4, width), 1e-9);
        assertEquals(0, SphereMap.longitude(0, width), 1e-9);
    }

    @Test
    void xAndZ_undoLongitudeAndLatitude() {
        final double width = SphereMap.widthOf(OVERWORLD, MARS);
        assertEquals(1_234_567, SphereMap.x(SphereMap.longitude(1_234_567, width), width), 1e-6);
        assertEquals(-765_432, SphereMap.z(SphereMap.latitude(-765_432, width), width), 1e-6);
    }

    @Test
    void blocksPerDegree_areTheSameBothWaysAndAboutFortyFiveThousandOnTheMoon() {
        final double width = SphereMap.widthOf(OVERWORLD, MOON);
        assertEquals(45_000, SphereMap.blocksPerDegree(width), 500);
        assertEquals(SphereMap.blocksPerDegree(width), (width / 2) / 180, 1e-6);
    }
}
