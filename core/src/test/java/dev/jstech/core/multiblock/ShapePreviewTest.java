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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShapePreviewTest {

    private static final String CASING = "test:casing";
    private static final String GLASS = "test:glass";

    @Test
    void pattern_laysOutEveryPlaceByLayerWithItsBlocks() {
        final MultiblockPattern pattern = MultiblockPattern.builder("oven")
                .layer("CCC", "C#C", "CCC")
                .layer("CGC", "G G", "CGC")
                .where('C', BlockMatch.blocks(CASING))
                .where('G', new BlockMatch(List.of(GLASS), List.of("test:windows")))
                .build();
        final ShapePreview preview = ShapePreviews.pattern(pattern, Orientation.NORTH, 0);
        assertEquals(2, preview.layers());
        assertEquals(9, preview.layer(0).size());
        assertEquals(8, preview.layer(1).size());
        assertEquals(3, preview.sizeX());
        assertEquals(17, preview.blocks());
        assertTrue(preview.layer(0).stream().anyMatch(ShapePreview.Ghost::terminal));
        assertEquals(Map.of(CASING, 12, GLASS, 4), preview.materials());
    }

    @Test
    void pattern_showsAndCountsTheBlockOfAPlaceThatTakesAirFirstNeverTheAir() {
        final MultiblockPattern pattern = MultiblockPattern.builder("vent")
                .layer("C#V")
                .where('C', BlockMatch.blocks(CASING))
                .where('V', BlockMatch.blocks("minecraft:air", GLASS))
                .build();
        final ShapePreview preview = ShapePreviews.pattern(pattern, Orientation.NORTH, 0);
        assertEquals(Map.of(CASING, 1, GLASS, 1), preview.materials());
        assertTrue(preview.ghosts().stream().noneMatch(ghost -> "minecraft:air".equals(ghost.first())),
                "no place is shown as air");
    }

    @Test
    void pattern_showsTheTerminalItNamesAndListsItApart() {
        final MultiblockPattern pattern = MultiblockPattern.builder("smelter")
                .layer("C#C")
                .where('C', BlockMatch.blocks(CASING))
                .terminal("test:smelter")
                .build();
        final ShapePreview preview = ShapePreviews.pattern(pattern, Orientation.NORTH, 0);
        assertEquals(Optional.of("test:smelter"), preview.terminalBlock());
        assertEquals(Map.of(CASING, 2), preview.materials());
    }

    @Test
    void withTerminal_namesOnlyATerminalTheShapeLeftUnnamed() {
        final MultiblockPattern bare = MultiblockPattern.builder("bare")
                .layer("C#")
                .where('C', BlockMatch.blocks(CASING))
                .build();
        final ShapePreview unnamed = ShapePreviews.pattern(bare, Orientation.NORTH, 0);
        assertEquals(Optional.empty(), unnamed.terminalBlock());
        final ShapePreview named = unnamed.withTerminal("test:first");
        assertEquals(Optional.of("test:first"), named.terminalBlock());
        assertEquals(Optional.of("test:first"), named.withTerminal("test:second").terminalBlock());
        assertEquals(unnamed.blocks(), named.blocks());
    }

    @Test
    void choices_groupThePlacesWhereMoreThanOneBlockWillDo() {
        final MultiblockPattern pattern = MultiblockPattern.builder("windows")
                .layer("CGC", "G#W")
                .where('C', BlockMatch.blocks(CASING))
                .where('G', new BlockMatch(List.of(GLASS, "test:tinted"), List.of()))
                .where('W', new BlockMatch(List.of(GLASS), List.of("test:windows")))
                .build();
        final List<ShapePreview.Choice> choices = ShapePreviews.pattern(pattern, Orientation.NORTH, 0).choices();
        assertEquals(2, choices.size());
        assertEquals(List.of(GLASS, "test:tinted"), choices.getFirst().blocks());
        assertEquals(2, choices.getFirst().places());
        assertEquals(List.of("test:windows"), choices.get(1).tags());
        assertEquals(1, choices.get(1).places());
    }

    @Test
    void pattern_turnsWithItsOrientationAndGrowsItsSlice() {
        final MultiblockPattern tower = MultiblockPattern.builder("tower")
                .layer("#C")
                .layer("GG")
                .layer("CC")
                .where('C', BlockMatch.blocks(CASING))
                .where('G', BlockMatch.blocks(GLASS))
                .repeatLayer(1, 2, 6)
                .build();
        final ShapePreview least = ShapePreviews.pattern(tower, Orientation.EAST, 0);
        assertEquals(4, least.layers());
        assertTrue(least.ghosts().stream().anyMatch(g -> g.x() == 0 && g.z() == 1 && g.y() == 0));
        assertEquals(6, ShapePreviews.pattern(tower, Orientation.NORTH, 2).layers());
        assertEquals(8, ShapePreviews.pattern(tower, Orientation.NORTH, 50).layers());
    }

    @Test
    void box_atItsLeastSizeFormsWhereItIsShown() {
        final BoxShape tank = BoxShape.of(Place.of(BlockMatch.blocks(CASING)), Place.of(BlockMatch.blocks(GLASS)),
                InsideRule.empty(), new Limit(3, 9), new Limit(4, 9));
        final ShapePreview preview = ShapePreviews.box(tank, 0);
        assertEquals(3 * 4 * 3 - 2, preview.blocks());
        assertFormsAsShown(tank, preview);
    }

    @Test
    void walls_atTheirLeastAreaFormWhereTheyAreShown() {
        final WallsShape smeltery = WallsShape.of(Place.of(BlockMatch.blocks(CASING)),
                Place.of(BlockMatch.blocks(CASING)), InsideRule.empty(), new Limit(4, 64), new Limit(2, 8));
        final ShapePreview preview = ShapePreviews.walls(smeltery, 0);
        assertEquals(3, preview.layers());
        assertFormsAsShown(smeltery, preview);
    }

    @Test
    void walls_ofAnAreaNoSquareFitsShowAFloorWithinItAndFormWhereShown() {
        final WallsShape kiln = WallsShape.of(Place.of(BlockMatch.blocks(CASING)),
                Place.of(BlockMatch.blocks(CASING)), InsideRule.empty(), new Limit(10, 12), new Limit(2, 8));
        for (final int grow : new int[] {0, 3}) {
            final ShapePreview preview = ShapePreviews.walls(kiln, grow);
            final long floor = preview.ghosts().stream().filter(ghost -> ghost.y() == -1).count();
            assertTrue(floor >= 10 && floor <= 12, "grown by " + grow + ", a floor of " + floor + " places");
            assertFormsAsShown(kiln, preview);
        }
    }

    @Test
    void cuboid_ofItsLeastSideFormsWhereItIsShown() {
        final CuboidShape processor = CuboidShape.of(Place.of(BlockMatch.blocks(CASING)), new Limit(2, 16));
        final ShapePreview preview = ShapePreviews.cuboid(processor, 1);
        assertEquals(27, preview.blocks());
        assertFormsAsShown(processor, preview);
    }

    @Test
    void turned_movesEveryPlaceRoundTheTerminal() {
        final MultiblockPattern hook = MultiblockPattern.builder("hook")
                .layer("#CC", "  C")
                .where('C', BlockMatch.blocks(CASING))
                .build();
        final ShapePreview east = ShapePreviews.pattern(hook, Orientation.NORTH, 0).turned(Orientation.EAST);
        assertEquals(ShapePreviews.pattern(hook, Orientation.EAST, 0), east);
        assertEquals(2, east.sizeX());
        assertEquals(3, east.sizeZ());
    }

    @Test
    void anchor_putsTheShapeInFrontLevelAndCentred() {
        final MultiblockPattern slab = MultiblockPattern.builder("slab")
                .layer("CCC", "C#C", "CCC")
                .where('C', BlockMatch.blocks(CASING))
                .build();
        final ShapePreview preview = ShapePreviews.pattern(slab, Orientation.NORTH, 0);
        assertArrayEquals(new int[] {10, 64, 18}, preview.anchor(10, 64, 20, 2, -1));
        assertArrayEquals(new int[] {12, 64, 20}, preview.anchor(10, 64, 20, 0, 1));
    }

    @Test
    void pattern_keepsThePlacesThatMustBeLeftEmptyApart() {
        final MultiblockPattern dock = MultiblockPattern.builder("dock")
                .layer("CxC", "CxC")
                .layer("C#C", "CCC")
                .where('C', BlockMatch.blocks(CASING))
                .inside('x', InsideRule.empty())
                .build();
        final ShapePreview preview = ShapePreviews.pattern(dock, Orientation.NORTH, 0);
        assertEquals(10, preview.blocks(), "nine casings and the terminal");
        assertEquals(2, preview.clears().size(), "the pit's two places are to be left empty");
        assertTrue(preview.clears().stream().allMatch(ghost -> ghost.layer() == 0 && ghost.x() == 0));
        assertEquals(Map.of(CASING, 9), preview.materials(), "nothing is built where it must be empty");
    }

    @Test
    void anchor_putsTheLayerOnTheGroundLevelWithWhatItIsShownBeside() {
        final MultiblockPattern sunk = MultiblockPattern.builder("sunk")
                .layer("CCC")
                .layer("CxC")
                .layer("C#C")
                .where('C', BlockMatch.blocks(CASING))
                .inside('x', InsideRule.empty())
                .ground(1)
                .build();
        final ShapePreview preview = ShapePreviews.pattern(sunk, Orientation.NORTH, 0);
        assertEquals(-1, preview.ground(), "the middle layer, a block under the terminal, stands on the ground");
        assertEquals(65, preview.anchor(10, 64, 20, 2, -1)[1], "it stands level with the projector, the bottom layer"
                + " under it");
        final MultiblockPattern standing = MultiblockPattern.builder("standing")
                .layer("CCC")
                .layer("C#C")
                .where('C', BlockMatch.blocks(CASING))
                .build();
        assertEquals(65, ShapePreviews.pattern(standing, Orientation.NORTH, 0).anchor(10, 64, 20, 2, -1)[1],
                "a shape not sunk stands on the ground by its bottom");
    }

    @Test
    void turned_keepsTheGroundUprightAndStandsOnItsBottomLaidDown() {
        final MultiblockPattern sunk = MultiblockPattern.builder("sunk")
                .layer("CC")
                .layer("C#")
                .where('C', BlockMatch.blocks(CASING))
                .ground(1)
                .build();
        final ShapePreview preview = ShapePreviews.pattern(sunk, Orientation.NORTH, 0);
        assertEquals(preview.ground(), preview.turned(Orientation.EAST).ground());
        final Orientation lying = Orientation.lying().stream().filter(o -> !o.upright()).findFirst().orElseThrow();
        final ShapePreview laid = preview.turned(lying);
        assertEquals(laid.minY(), laid.ground());
    }

    /* Builds the preview's first choices round a terminal at the origin, and checks the shape forms there. */
    private static void assertFormsAsShown(final IStructureShape shape, final ShapePreview preview) {
        final Map<Long, String> world = new HashMap<>();
        for (final ShapePreview.Ghost ghost : preview.ghosts()) {
            world.put(PatternMatcher.encodePosition(ghost.x(), ghost.y(), ghost.z()),
                    ghost.terminal() ? "test:terminal" : ghost.first());
        }
        assertInstanceOf(ShapeMatch.Formed.class, shape.check(pos -> world.getOrDefault(pos, Cells.AIR),
                PatternMatcher.encodePosition(0, 0, 0)));
    }
}
