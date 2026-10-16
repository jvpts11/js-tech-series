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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpaceSlotsTest {

    @Test
    void slots_numberEveryPlaceOnce() {
        final Set<Integer> seen = new HashSet<>();
        seen.add(SpaceSlots.CENTRE);
        for (int face = 0; face < 6; face++) {
            assertTrue(seen.add(SpaceSlots.face(face)));
        }
        for (int axis = SpaceSlots.X; axis <= SpaceSlots.Z; axis++) {
            for (int k = 0; k < 4; k++) {
                assertTrue(seen.add(SpaceSlots.edge(axis, (k & 1) != 0, (k & 2) != 0)));
            }
        }
        for (int k = 0; k < 8; k++) {
            assertTrue(seen.add(SpaceSlots.corner((k & 1) != 0, (k & 2) != 0, (k & 4) != 0)));
        }
        assertEquals(SpaceSlots.COUNT, seen.size());
    }

    @Test
    void slots_tellTheirKindAndWhereTheyStand() {
        final int up = SpaceSlots.face(1);
        assertTrue(SpaceSlots.isFace(up));
        assertEquals(1, SpaceSlots.faceOf(up));
        assertEquals(SpaceSlots.Y, SpaceSlots.faceAxis(1));
        assertTrue(SpaceSlots.faceFar(1));
        final int edge = SpaceSlots.edge(SpaceSlots.Y, true, false);
        assertTrue(SpaceSlots.isEdge(edge));
        assertEquals(SpaceSlots.Y, SpaceSlots.edgeAxis(edge));
        assertTrue(SpaceSlots.edgeFirstFar(edge));
        assertFalse(SpaceSlots.edgeSecondFar(edge));
        final int corner = SpaceSlots.corner(true, false, true);
        assertTrue(SpaceSlots.cornerFar(corner, SpaceSlots.X));
        assertFalse(SpaceSlots.cornerFar(corner, SpaceSlots.Y));
        assertThrows(IllegalArgumentException.class, () -> SpaceSlots.face(6));
        assertThrows(IllegalArgumentException.class, () -> SpaceSlots.faceOf(SpaceSlots.CENTRE));
    }
}
