/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class PartBoxTest {

    private static final PartBox WHOLE = new PartBox(0, 0, 0, 16, 16, 16);
    private static final int NORTH = 2;
    private static final int SOUTH = 3;
    private static final int EAST = 5;
    private static final int FACES = 6;

    @Test
    void minus_keepsABoxItDoesNotShareRoomWith() {
        final PartBox floor = new PartBox(0, 0, 0, 16, 2, 16);
        final PartBox above = new PartBox(0, 2, 0, 16, 4, 16);
        assertEquals(List.of(floor), floor.minus(above));
    }

    @Test
    void minus_leavesExactlyWhatLiesOutsideTheOther() {
        final PartBox middle = new PartBox(4, 4, 4, 12, 12, 12);
        final List<PartBox> left = WHOLE.minus(middle);
        assertEquals(6, left.size(), "a box round a hole is six boxes");
        assertEquals(volume(WHOLE) - volume(middle), left.stream().mapToInt(PartBoxTest::volume).sum());
        for (final PartBox box : left) {
            assertFalse(box.overlaps(middle), box + " lies outside the hole");
            for (final PartBox other : left) {
                assertFalse(box != other && box.overlaps(other), box + " and " + other + " share no room");
            }
        }
    }

    @Test
    void minus_ofAnEdgeStripLeavesTheRest() {
        final PartBox cover = new PartBox(0, 0, 0, 16, 16, 2);
        final PartBox slab = new PartBox(0, 0, 0, 16, 8, 16);
        assertEquals(List.of(new PartBox(0, 8, 0, 16, 16, 2)), cover.minus(slab));
    }

    @Test
    void turnedTo_turnsADevicePlateToEveryFace() {
        final PartBox plate = SpaceFit.deviceBox(NORTH);
        for (int face = 0; face < FACES; face++) {
            assertEquals(SpaceFit.deviceBox(face), plate.turnedTo(face), "the plate on face " + face);
        }
    }

    @Test
    void turnedTo_turnsABoxOffTheMiddleAsAModelTurns() {
        final PartBox corner = new PartBox(0, 12, 0, 4, 16, 2);
        assertEquals(corner, corner.turnedTo(NORTH));
        assertEquals(new PartBox(12, 12, 14, 16, 16, 16), corner.turnedTo(SOUTH), "half a turn about y");
        assertEquals(corner.turnedTo(SOUTH), corner.turnedTo(EAST).turnedTo(EAST),
                "two quarter turns about y are half a turn");
    }

    private static int volume(final PartBox box) {
        return (box.x1() - box.x0()) * (box.y1() - box.y0()) * (box.z1() - box.z0());
    }
}
