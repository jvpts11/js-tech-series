/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.biome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.worldgen.biome.WorldSearch.Found;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorldSearchTest {

    /* A world as wide as Mars', 32 million blocks by 16. */
    private static final int HALF_WIDTH = 16_000_000;
    private static final int HALF_HEIGHT = 8_000_000;
    /* What the search may miss the nearest place by: its finest step, and the step it takes past the edge. */
    private static final double SLACK = 96;

    @Test
    void nearest_findsTheOnlyPlaceAcrossAHugeWorld() {
        final Disc far = new Disc(9_000_000, 5_000_000, 20_000);
        final Found found = search(far::holds, 0, 0);
        assertTrue(far.holds(found.x(), found.z()), "the place holds: " + found);
        assertNearest(far.nearestDistance(0, 0), found, 0, 0);
    }

    @Test
    void nearest_takesTheNearerOfTwoPlacesThoughItsRingComesLater() {
        // The diagonal one lies in an earlier ring of the grid, but farther as the crow flies.
        final Disc diagonal = new Disc(1_000_000, 1_000_000, 20_000);
        final Disc straight = new Disc(1_300_000, 0, 20_000);
        final Found found = search((x, z) -> diagonal.holds(x, z) || straight.holds(x, z), 0, 0);
        assertTrue(straight.holds(found.x(), found.z()), "the nearer place is found: " + found);
        assertNearest(straight.nearestDistance(0, 0), found, 0, 0);
    }

    @Test
    void nearest_findsTheStartWhenItHolds() {
        final Found found = search((x, z) -> true, 1234, -5678);
        assertEquals(new Found(1234, -5678), found);
    }

    @Test
    void nearest_findsNothingWhereNothingHolds() {
        final Optional<Found> found = WorldSearch.nearest((x, z) -> false, 0, 0, -HALF_WIDTH,
                HALF_WIDTH - 1, -HALF_HEIGHT, HALF_HEIGHT - 1);
        assertFalse(found.isPresent());
    }

    @Test
    void nearest_readsOnlyInsideTheBoundsAndStartsInsideThem() {
        final Disc corner = new Disc(HALF_WIDTH - 30_000, HALF_HEIGHT - 30_000, 20_000);
        final Found found = search((x, z) -> {
            assertTrue(x >= -HALF_WIDTH && x < HALF_WIDTH && z >= -HALF_HEIGHT && z < HALF_HEIGHT,
                    "read inside the bounds: " + x + ", " + z);
            return corner.holds(x, z);
        }, -20_000_000, 0);
        assertTrue(corner.holds(found.x(), found.z()), "the corner's place is found from past the west end: " + found);
    }

    @Test
    void nearest_findsAPlaceInASmallWorldAtTheFinestStep() {
        final Disc small = new Disc(700, -300, 40);
        final Found found = WorldSearch.nearest(small::holds, 0, 0, -1000, 999, -1000, 999).orElseThrow();
        assertTrue(small.holds(found.x(), found.z()), "the place holds: " + found);
        assertNearest(small.nearestDistance(0, 0), found, 0, 0);
    }

    @Test
    void nearest_putsThePlaceAStepInsideTheEdgeOfWhatHolds() {
        final Found found = WorldSearch.nearest((x, z) -> x >= 1000, 0, 0, -2000, 1999, -2000, 1999).orElseThrow();
        assertTrue(found.x() >= 1000 + 32 && found.x() <= 1000 + SLACK && found.z() == 0,
                "the place lies a step past the edge, straight from the start: " + found);
    }

    @Test
    void nearest_refusesBoundsThatHoldNoColumn() {
        assertThrows(IllegalArgumentException.class, () -> WorldSearch.nearest((x, z) -> true, 0, 0, 10, 9, 0, 0));
    }

    private static Found search(final WorldSearch.IProbe probe, final int fromX, final int fromZ) {
        return WorldSearch.nearest(probe, fromX, fromZ, -HALF_WIDTH, HALF_WIDTH - 1, -HALF_HEIGHT, HALF_HEIGHT - 1)
                .orElseThrow();
    }

    private static void assertNearest(final double nearest, final Found found, final int fromX, final int fromZ) {
        final double distance = Math.sqrt(found.distanceSquared(fromX, fromZ));
        assertTrue(distance <= nearest + SLACK, "found " + distance + " away, the nearest lies " + nearest);
    }

    /* A round place where something holds. */
    private record Disc(int x, int z, int radius) {

        boolean holds(final int atX, final int atZ) {
            final double dx = (double) atX - x;
            final double dz = (double) atZ - z;
            return dx * dx + dz * dz <= (double) radius * radius;
        }

        double nearestDistance(final int fromX, final int fromZ) {
            return Math.max(0, Math.hypot((double) fromX - x, (double) fromZ - z) - radius);
        }
    }
}
