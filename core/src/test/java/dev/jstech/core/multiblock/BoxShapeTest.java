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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BoxShapeTest {

    private static final String CASING = "test:casing";
    private static final String GLASS = "test:glass";
    private static final String HATCH = "test:hatch";
    private static final BoxShape TANK = BoxShape.of(Place.of(BlockMatch.blocks(CASING)),
            Place.of(BlockMatch.blocks(CASING, GLASS, HATCH)).counting("hatch", BlockMatch.blocks(HATCH)),
            InsideRule.empty(), new Limit(3, 9), new Limit(3, 9));

    @Test
    void check_findsABoxOfWhateverSizeWasBuilt() {
        final Map<Long, String> world = new HashMap<>();
        box(world, 0, 0, 0, 4, 3, 5, GLASS);
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                TANK.check(provider(world), pos(2, 1, 0)));
        assertEquals(5, formed.sizeX());
        assertEquals(4, formed.sizeY());
        assertEquals(6, formed.sizeZ());
        assertEquals(3 * 2 * 4, formed.inside().size());
        assertEquals(5 * 4 * 6 - 3 * 2 * 4 - 1, formed.parts().size());
    }

    @Test
    void check_findsTheBoxFromATerminalOnItsTop() {
        final Map<Long, String> world = new HashMap<>();
        box(world, 0, 0, 0, 3, 3, 3, GLASS);
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                TANK.check(provider(world), pos(1, 3, 2)));
        assertEquals(4, formed.sizeY());
        assertEquals(8, formed.inside().size());
    }

    @Test
    void check_failsAFaceBlockOnAnEdge() {
        final Map<Long, String> world = new HashMap<>();
        box(world, 0, 0, 0, 4, 4, 4, GLASS);
        world.put(pos(2, 4, 4), GLASS);
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                TANK.check(provider(world), pos(2, 2, 0)));
        assertEquals(ShapeMatch.Wrong.BLOCK, failure.wrong());
        assertEquals(ShapeMatch.FRAME, failure.wanted());
        assertEquals(GLASS, failure.found());
        assertEquals(pos(2, 4, 4), failure.at());
    }

    @Test
    void check_namesABrokenEdgeEvenWhereItsDepthIsMeasured() {
        final Map<Long, String> world = new HashMap<>();
        box(world, 0, 0, 0, 2, 2, 2, GLASS);
        world.put(pos(2, 2, 1), "minecraft:dirt");
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                TANK.check(provider(world), pos(1, 1, 0)));
        assertEquals(ShapeMatch.Wrong.BLOCK, failure.wrong());
        assertEquals(pos(2, 2, 1), failure.at());
        assertEquals("minecraft:dirt", failure.found());
    }

    @Test
    void check_namesAFaceBlockMissingBesideTheTerminal() {
        final Map<Long, String> world = new HashMap<>();
        box(world, 0, 0, 0, 4, 4, 4, GLASS);
        world.remove(pos(3, 2, 0));
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                TANK.check(provider(world), pos(2, 2, 0)));
        assertEquals(ShapeMatch.FACE, failure.wanted());
        assertEquals(pos(3, 2, 0), failure.at());
    }

    @Test
    void check_failsABlockLeftInsideAnEmptyBox() {
        final Map<Long, String> world = new HashMap<>();
        box(world, 0, 0, 0, 4, 4, 4, GLASS);
        world.put(pos(2, 2, 2), "minecraft:dirt");
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                TANK.check(provider(world), pos(2, 2, 0)));
        assertEquals(ShapeMatch.INSIDE, failure.wanted());
        assertEquals(pos(2, 2, 2), failure.at());
    }

    @Test
    void check_seesABoxLargerThanItsMostAsTooLarge() {
        final Map<Long, String> world = new HashMap<>();
        box(world, 0, 0, 0, 10, 2, 2, GLASS);
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                TANK.check(provider(world), pos(5, 1, 0)));
        assertEquals(ShapeMatch.Wrong.SIZE, failure.wrong());
        assertEquals(ShapeMatch.SIZE_X, failure.wanted());
        assertEquals("11", failure.found());
    }

    @Test
    void check_keepsTheHatchesOnItsFacesWithinTheirLimit() {
        final BoxShape limited = TANK.limit("hatch", 1, 2);
        final Map<Long, String> world = new HashMap<>();
        box(world, 0, 0, 0, 4, 4, 4, CASING);
        assertEquals(ShapeMatch.Wrong.COUNT, assertInstanceOf(ShapeMatch.Failure.class,
                limited.check(provider(world), pos(2, 2, 0))).wrong());
        world.put(pos(0, 2, 2), HATCH);
        world.put(pos(4, 2, 2), HATCH);
        assertEquals(2, assertInstanceOf(ShapeMatch.Formed.class,
                limited.check(provider(world), pos(2, 2, 0))).count("hatch"));
        world.put(pos(2, 4, 2), HATCH);
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                limited.check(provider(world), pos(2, 2, 0)));
        assertEquals("hatch", failure.wanted());
        assertEquals("3", failure.found());
    }

    @Test
    void check_readsWhatStandsInside() {
        final BoxShape battery = BoxShape.of(Place.of(BlockMatch.blocks(CASING)), Place.of(BlockMatch.blocks(GLASS)),
                InsideRule.empty().with(InsidePart.of("cell", BlockMatch.blocks("test:cell")).gives("capacity", 100)),
                new Limit(3, 9), new Limit(3, 9));
        final Map<Long, String> world = new HashMap<>();
        box(world, 0, 0, 0, 3, 3, 3, GLASS);
        world.put(pos(1, 1, 1), "test:cell");
        world.put(pos(2, 2, 2), "test:cell");
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                battery.check(provider(world), pos(1, 1, 0)));
        assertEquals(200.0, formed.value("capacity", 0), 1e-9);
    }

    @Test
    void constructor_refusesASideBeyondTheMostAShapeMayBe() {
        assertThrows(IllegalArgumentException.class, () -> BoxShape.of(Place.of(BlockMatch.blocks(CASING)),
                Place.of(BlockMatch.blocks(CASING)), InsideRule.empty(), new Limit(3, RuleShape.MOST_SIDE + 1),
                new Limit(3, 9)));
    }

    /* A box from one corner to the other: casing on its edges, the face block between them, air inside. */
    private static void box(final Map<Long, String> world, final int x0, final int y0, final int z0, final int x1,
                            final int y1, final int z1, final String face) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    final int bounds = (x == x0 || x == x1 ? 1 : 0) + (y == y0 || y == y1 ? 1 : 0)
                            + (z == z0 || z == z1 ? 1 : 0);
                    if (bounds >= 2) {
                        world.put(pos(x, y, z), CASING);
                    } else if (bounds == 1) {
                        world.put(pos(x, y, z), face);
                    }
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
