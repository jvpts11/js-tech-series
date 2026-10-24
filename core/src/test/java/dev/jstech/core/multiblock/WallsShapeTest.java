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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WallsShapeTest {

    private static final String BRICK = "test:brick";
    private static final String DRAIN = "test:drain";
    /* An L of five places: three along x, two more along z from its west end. */
    private static final List<int[]> FLOOR = List.of(new int[] {0, 0}, new int[] {1, 0}, new int[] {2, 0},
            new int[] {0, 1}, new int[] {0, 2});
    private static final WallsShape SMELTERY = WallsShape.of(Place.of(BlockMatch.blocks(BRICK)),
            Place.of(BlockMatch.blocks(BRICK, DRAIN)).counting("drain", BlockMatch.blocks(DRAIN)),
            InsideRule.empty(), new Limit(1, 64), new Limit(1, 8));

    @Test
    void check_findsAFloorOfAnyOutlineWithoutItsCorners() {
        final Map<Long, String> world = new HashMap<>();
        build(world, 2);
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                SMELTERY.check(provider(world), pos(-1, 1, 0)));
        assertEquals(10, formed.inside().size());
        assertEquals(5, formed.sizeX());
        assertEquals(3, formed.sizeY());
        assertEquals(5, formed.sizeZ());
        assertEquals(5 + 11 * 2 - 1, formed.parts().size());
    }

    @Test
    void check_risesAsHighAsTheWallsRunUnbroken() {
        final Map<Long, String> world = new HashMap<>();
        build(world, 4);
        world.put(pos(3, 4, 0), "minecraft:air");
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                SMELTERY.check(provider(world), pos(-1, 1, 0)));
        assertEquals(4, formed.sizeY());
        assertEquals(15, formed.inside().size());
    }

    @Test
    void check_failsAPlaceOfTheFloorThatIsMissing() {
        final Map<Long, String> world = new HashMap<>();
        build(world, 2);
        world.remove(pos(2, 0, 0));
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                SMELTERY.check(provider(world), pos(-1, 1, 0)));
        assertEquals(ShapeMatch.FLOOR, failure.wanted());
        assertEquals(pos(2, 0, 0), failure.at());
    }

    @Test
    void check_failsAWallBrokenAtTheTerminalsLayer() {
        final Map<Long, String> world = new HashMap<>();
        build(world, 2);
        world.put(pos(1, 1, 1), "minecraft:dirt");
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                SMELTERY.check(provider(world), pos(-1, 1, 0)));
        assertEquals(ShapeMatch.WALL, failure.wanted());
        assertEquals("minecraft:dirt", failure.found());
    }

    @Test
    void check_seesAGapInTheWallsAsAFloorTooLarge() {
        final Map<Long, String> world = new HashMap<>();
        build(world, 2);
        world.remove(pos(3, 1, 0));
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                SMELTERY.check(provider(world), pos(-1, 1, 0)));
        assertEquals(ShapeMatch.Wrong.SIZE, failure.wrong());
        assertEquals(ShapeMatch.AREA, failure.wanted());
    }

    @Test
    void check_countsTheDrainsInItsWalls() {
        final Map<Long, String> world = new HashMap<>();
        build(world, 2);
        world.put(pos(3, 1, 0), DRAIN);
        world.put(pos(0, 2, 3), DRAIN);
        assertEquals(2, assertInstanceOf(ShapeMatch.Formed.class,
                SMELTERY.check(provider(world), pos(-1, 1, 0))).count("drain"));
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                SMELTERY.limit("drain", 0, 1).check(provider(world), pos(-1, 1, 0)));
        assertEquals("drain", failure.wanted());
    }

    /* The L of floor at y 0, its walls from y 1 up {@code layers} high, round every side but the corners. */
    private static void build(final Map<Long, String> world, final int layers) {
        for (final int[] cell : FLOOR) {
            world.put(pos(cell[0], 0, cell[1]), BRICK);
        }
        for (final int[] cell : FLOOR) {
            for (final int[] side : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                final int x = cell[0] + side[0];
                final int z = cell[1] + side[1];
                if (inFloor(x, z)) {
                    continue;
                }
                for (int y = 1; y <= layers; y++) {
                    world.put(pos(x, y, z), BRICK);
                }
            }
        }
    }

    private static boolean inFloor(final int x, final int z) {
        for (final int[] cell : FLOOR) {
            if (cell[0] == x && cell[1] == z) {
                return true;
            }
        }
        return false;
    }

    private static IBlockProvider provider(final Map<Long, String> world) {
        return pos -> world.getOrDefault(pos, "minecraft:air");
    }

    private static long pos(final int x, final int y, final int z) {
        return PatternMatcher.encodePosition(x, y, z);
    }
}
