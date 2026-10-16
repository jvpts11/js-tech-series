/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WindTest {

    private static final double CLOSE = 1e-9;

    @Test
    void fromDegrees_isTheWayItComesFrom() {
        // Blowing towards east comes from the west; towards south (positive Z) from the north.
        assertEquals(270, new Wind(4, 0).fromDegrees(), CLOSE);
        assertEquals(0, new Wind(0, 4).fromDegrees(), CLOSE);
        assertEquals(90, new Wind(-4, 0).fromDegrees(), CLOSE);
        assertEquals(180, new Wind(0, -4).fromDegrees(), CLOSE);
    }

    @Test
    void fromPoint_isOneOfEight() {
        assertEquals(6, new Wind(4, 0).fromPoint());
        assertEquals(0, new Wind(0, 4).fromPoint());
        assertEquals(1, new Wind(-3, 3).fromPoint());
    }

    @Test
    void speed_isItsLength() {
        assertEquals(5, new Wind(3, 4).speed(), CLOSE);
        assertEquals(0, Wind.STILL.speed(), CLOSE);
    }
}
