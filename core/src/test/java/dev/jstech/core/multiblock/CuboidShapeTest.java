/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CuboidShapeTest {

    private static final String UNIT = "test:unit";
    private static final String STORAGE = "test:storage";
    private static final CuboidShape PROCESSOR = CuboidShape.of(Place.of(BlockMatch.blocks(UNIT, STORAGE))
            .counting("storage", BlockMatch.blocks(STORAGE)), new Limit(1, 16)).limit("storage", 1, 64);

    @Test
    void check_findsACuboidOfWhateverSizeWasBuilt() {
        final Map<Long, String> world = new HashMap<>();
        fill(world, 2, 3, 4);
        world.put(pos(1, 2, 3), STORAGE);
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                PROCESSOR.check(provider(world), pos(0, 0, 0)));
        assertEquals(2, formed.sizeX());
        assertEquals(3, formed.sizeY());
        assertEquals(4, formed.sizeZ());
        assertEquals(23, formed.parts().size());
        assertEquals(1, formed.count("storage"));
    }

    @Test
    void check_failsAPlaceOfTheCuboidThatHoldsNoUnit() {
        final Map<Long, String> world = new HashMap<>();
        fill(world, 3, 3, 3);
        world.put(pos(0, 0, 0), STORAGE);
        world.put(pos(2, 2, 2), "minecraft:dirt");
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                PROCESSOR.check(provider(world), pos(1, 1, 1)));
        assertEquals(ShapeMatch.UNIT, failure.wanted());
        assertEquals(pos(2, 2, 2), failure.at());
    }

    @Test
    void check_namesAUnitMissingOnALineThroughTheControllerRatherThanFormingSmaller() {
        final Map<Long, String> world = new HashMap<>();
        fill(world, 3, 3, 3);
        world.put(pos(0, 0, 0), STORAGE);
        world.remove(pos(2, 1, 1));
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                PROCESSOR.check(provider(world), pos(1, 1, 1)));
        assertEquals(ShapeMatch.UNIT, failure.wanted());
        assertEquals(pos(2, 1, 1), failure.at());
    }

    @Test
    void check_needsAtLeastOneStorageUnit() {
        final Map<Long, String> world = new HashMap<>();
        fill(world, 2, 2, 2);
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                PROCESSOR.check(provider(world), pos(0, 0, 0)));
        assertEquals(ShapeMatch.Wrong.COUNT, failure.wrong());
        assertEquals("storage", failure.wanted());
        assertEquals("0", failure.found());
    }

    /* Units from the origin up to the size given. */
    private static void fill(final Map<Long, String> world, final int sx, final int sy, final int sz) {
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    world.put(pos(x, y, z), UNIT);
                }
            }
        }
    }

    private static IBlockProvider provider(final Map<Long, String> world) {
        return pos -> world.getOrDefault(pos, "minecraft:air");
    }

    private static long pos(final int x, final int y, final int z) {
        return PatternMatcher.encodePosition(x, y, z);
    }
}
