/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTest {

    @Test
    void constructor_refusesCornersTheWrongWayRound() {
        assertThrows(IllegalArgumentException.class, () -> new Box(5, 0, 0, 4, 0, 0));
    }

    @Test
    void between_ordersItsCorners() {
        assertEquals(new Box(1, 2, 3, 4, 5, 6), Box.between(4, 5, 6, 1, 2, 3));
    }

    @Test
    void intersects_whenTheyShareABlock() {
        final Box a = new Box(0, 0, 0, 4, 4, 4);
        assertTrue(a.intersects(new Box(4, 4, 4, 8, 8, 8)));
        assertFalse(a.intersects(new Box(5, 0, 0, 8, 4, 4)));
    }

    @Test
    void distanceSquared_isZeroInsideAndMeasuredToTheNearestFace() {
        final Box box = new Box(0, 0, 0, 10, 10, 10);
        assertEquals(0L, box.distanceSquared(5, 5, 5));
        assertEquals(9L, box.distanceSquared(13, 5, 5));
        assertEquals(2L, box.distanceSquared(-1, -1, 5));
    }
}
