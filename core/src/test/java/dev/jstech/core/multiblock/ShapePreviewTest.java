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
        assertTrue(preview.layer(0).stream().anyMatch(ShapePreview.Ghost::controller));
        assertEquals(Map.of(CASING, 12, GLASS, 4), preview.materials());
    }

    @Test
    void pattern_showsTheControllerItNamesAndListsItApart() {
        final MultiblockPattern pattern = MultiblockPattern.builder("smelter")
                .layer("C#C")
                .where('C', BlockMatch.blocks(CASING))
                .controller("test:smelter")
                .build();
        final ShapePreview preview = ShapePreviews.pattern(pattern, Orientation.NORTH, 0);
        assertEquals(Optional.of("test:smelter"), preview.controllerBlock());
        assertEquals(Map.of(CASING, 2), preview.materials());
    }

    @Test
    void withController_namesOnlyAControllerTheShapeLeftUnnamed() {
        final MultiblockPattern bare = MultiblockPattern.builder("bare")
                .layer("C#")
                .where('C', BlockMatch.blocks(CASING))
                .build();
        final ShapePreview unnamed = ShapePreviews.pattern(bare, Orientation.NORTH, 0);
        assertEquals(Optional.empty(), unnamed.controllerBlock());
        final ShapePreview named = unnamed.withController("test:first");
        assertEquals(Optional.of("test:first"), named.controllerBlock());
        assertEquals(Optional.of("test:first"), named.withController("test:second").controllerBlock());
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
    void cuboid_ofItsLeastSideFormsWhereItIsShown() {
        final CuboidShape processor = CuboidShape.of(Place.of(BlockMatch.blocks(CASING)), new Limit(2, 16));
        final ShapePreview preview = ShapePreviews.cuboid(processor, 1);
        assertEquals(27, preview.blocks());
        assertFormsAsShown(processor, preview);
    }

    @Test
    void turned_movesEveryPlaceRoundTheController() {
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

    /* Builds the preview's first choices round a controller at the origin, and checks the shape forms there. */
    private static void assertFormsAsShown(final IStructureShape shape, final ShapePreview preview) {
        final Map<Long, String> world = new HashMap<>();
        for (final ShapePreview.Ghost ghost : preview.ghosts()) {
            world.put(PatternMatcher.encodePosition(ghost.x(), ghost.y(), ghost.z()),
                    ghost.controller() ? "test:controller" : ghost.first());
        }
        assertInstanceOf(ShapeMatch.Formed.class, shape.check(pos -> world.getOrDefault(pos, Cells.AIR),
                PatternMatcher.encodePosition(0, 0, 0)));
    }
}
