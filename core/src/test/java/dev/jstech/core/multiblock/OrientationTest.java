/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrientationTest {

    @Test
    void turns_moveAPlaceAsTheRotationsDo() {
        final List<Orientation> turns = Orientation.turns();
        final Rotation[] rotations = {Rotation.NORTH, Rotation.EAST, Rotation.SOUTH, Rotation.WEST};
        for (int i = 0; i < turns.size(); i++) {
            final int[] turned = rotations[i].transform(2, 5);
            assertArrayEquals(new int[] {turned[0], 3, turned[1]}, turns.get(i).apply(2, 3, 5));
            assertEquals(Optional.of(rotations[i]), turns.get(i).rotation());
        }
    }

    @Test
    void sets_holdFourEightTwentyFourAndFortyEightDistinctWays() {
        assertEquals(4, new HashSet<>(Orientation.turns()).size());
        assertEquals(8, new HashSet<>(Orientation.turnsAndMirrors()).size());
        assertEquals(24, new HashSet<>(Orientation.lying()).size());
        assertEquals(48, new HashSet<>(Orientation.lyingAndMirrors()).size());
    }

    @Test
    void sets_putTheFourTurnsFirst() {
        assertEquals(Orientation.turns(), Orientation.turnsAndMirrors().subList(0, 4));
        assertEquals(Orientation.turns(), Orientation.lying().subList(0, 4));
        assertEquals(Orientation.turns(), Orientation.lyingAndMirrors().subList(0, 4));
    }

    @Test
    void mirrors_areNotProperAndLyingWaysAreNotAllUpright() {
        for (final Orientation each : Orientation.turnsAndMirrors().subList(4, 8)) {
            assertFalse(each.proper());
            assertTrue(each.upright());
            assertEquals(Optional.empty(), each.rotation());
        }
        for (final Orientation each : Orientation.lying()) {
            assertTrue(each.proper());
        }
        assertEquals(4, Orientation.lying().stream().filter(Orientation::upright).count());
    }

    @Test
    void then_turnsTwiceAsOneTurnOfTwice() {
        assertEquals(Orientation.SOUTH, Orientation.EAST.then(Orientation.EAST));
        assertEquals(Orientation.NORTH, Orientation.WEST.then(Orientation.EAST));
        final Orientation lying = Orientation.lying().get(10);
        assertArrayEquals(Orientation.EAST.apply(lying.apply(1, 2, 3)[0], lying.apply(1, 2, 3)[1],
                lying.apply(1, 2, 3)[2]), lying.then(Orientation.EAST).apply(1, 2, 3));
    }

    @Test
    void constructor_refusesAnAxisTakenTwice() {
        assertThrows(IllegalArgumentException.class, () -> new Orientation(0, 1, 0, 1, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new Orientation(0, 2, 1, 1, 2, 1));
    }
}
