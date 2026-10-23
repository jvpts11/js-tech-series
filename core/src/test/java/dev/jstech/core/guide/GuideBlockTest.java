/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.multiblock.ShapePreview;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuideBlockTest {

    @Test
    void modes_haveTwoWaysAtLeastAndAWordForEach() {
        final GuideBlock.Picture one = new GuideBlock.Picture("a:one", "", "{}", 0, 40, "one");
        final GuideBlock.Picture two = new GuideBlock.Picture("a:two", "", "{}", 0, 40, "two");

        assertEquals(2, new GuideBlock.Modes(List.of("first", "second"), List.of(one, two)).ways().size());
        assertThrows(IllegalArgumentException.class, () -> new GuideBlock.Modes(List.of("first"), List.of(one)));
        assertThrows(IllegalArgumentException.class, () -> new GuideBlock.Modes(List.of("first"), List.of(one,
                two)));
    }

    @Test
    void modes_drawOnlyFiguresEachWay() {
        final GuideBlock.Picture one = new GuideBlock.Picture("a:one", "", "{}", 0, 40, "one");

        assertThrows(IllegalArgumentException.class, () -> new GuideBlock.Modes(List.of("first", "second"),
                List.of(one, new GuideBlock.Paragraph("words"))));
    }

    @Test
    void scene_spansFromTheGridsCornerToItsFarthestBlock() {
        final GuideBlock.Scene scene = scene(List.of(block(0, 0, 0), block(2, 1, 0), block(0, 3, 4)), List.of());

        assertEquals(3, scene.width());
        assertEquals(4, scene.tall());
        assertEquals(5, scene.depth());
        assertTrue(scene.at(2, 1, 0).isPresent());
        assertFalse(scene.at(1, 1, 1).isPresent());
    }

    @Test
    void scene_refusesTwoBlocksInOnePlaceAndTwoBalloonsOfOneNumber() {
        assertThrows(IllegalArgumentException.class, () -> scene(List.of(block(1, 0, 0), block(1, 0, 0)),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> scene(List.of(block(0, 0, 0)), List.of(
                balloon(1), balloon(1))));
        assertThrows(IllegalArgumentException.class, () -> scene(List.of(), List.of()), "a scene holds a block");
    }

    @Test
    void scene_isDrawnTallEnoughToSeeAndNoTallerThanAPage() {
        assertThrows(IllegalArgumentException.class, () -> new GuideBlock.Scene(List.of(block(0, 0, 0)),
                GuideBlock.SceneView.CORNER, List.of(), false, GuideBlock.Scene.MIN_HEIGHT - 1, "caption"));
        assertThrows(IllegalArgumentException.class, () -> new GuideBlock.Scene(List.of(block(0, 0, 0)),
                GuideBlock.SceneView.CORNER, List.of(), false, GuideBlock.Scene.MAX_HEIGHT + 1, "caption"));
    }

    @Test
    void sceneBlock_standsInsideTheGridAndNamesItsBlockApartFromItsProperties() {
        assertThrows(IllegalArgumentException.class, () -> block(GuideBlock.Scene.GRID, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> block(0, -1, 0));
        assertEquals("minecraft:furnace", new GuideBlock.SceneBlock(0, 0, 0,
                "minecraft:furnace[facing=south,lit=true]", "").block());
    }

    @Test
    void sceneView_keepsItsTiltAndNearnessWithinBoundsAndATurnWithinOneRound() {
        final GuideBlock.SceneView view = new GuideBlock.SceneView(370, 120, 9);

        assertEquals(10, view.yaw(), 1.0E-4);
        assertEquals(GuideBlock.SceneView.MAX_PITCH, view.pitch());
        assertEquals(GuideBlock.SceneView.MAX_ZOOM, view.zoom());
        assertEquals(GuideBlock.SceneView.MIN_ZOOM, view.zoomed(0.01F).zoom());
        assertEquals(-170, view.turned(180, 0).yaw(), 1.0E-4);
    }

    @Test
    void scenePart_isABoxInsideItsBlockOrTheWholeBlock() {
        final GuideBlock.ScenePart whole = GuideBlock.ScenePart.of(1, 2, 3);

        assertTrue(whole.whole());
        assertFalse(whole.box(4, 4, 15, 12, 6, 16).whole());
        assertThrows(IllegalArgumentException.class, () -> whole.box(10, 0, 0, 4, 16, 16), "from past its end");
        assertThrows(IllegalArgumentException.class, () -> whole.box(0, 0, 0, 17, 16, 16), "past the block");
    }

    @Test
    void multiblock_laysItsPlacesFromTheGridsCornerAndLeavesOutWhatHasNoBlock() {
        final ShapePreview preview = new ShapePreview(List.of(
                ghost(-1, 0, 0, List.of("test:brick"), List.of(), false),
                ghost(0, 0, 0, List.of("test:oven"), List.of(), true),
                ghost(1, 1, -2, List.of(), List.of("test:windows"), false),
                ghost(1, 0, 0, List.of("test:unknown"), List.of(), false)), -1, 0, -2, 3, 2, 3);

        final List<GuideBlock.SceneBlock> blocks = GuideBlock.Multiblock.blocksOf(preview,
                first -> first.equals("test:unknown") ? "" : first.replace("#", ""));

        assertEquals(3, blocks.size(), "a place whose block the game lacks stays empty");
        assertEquals("test:brick", blocks.getFirst().state());
        assertEquals(0, blocks.getFirst().x());
        assertEquals(2, blocks.getFirst().z(), "the northmost place stands on the grid's first line");
        assertTrue(blocks.stream().anyMatch(block -> block.state().equals("test:oven") && block.x() == 1),
                "the controller stands in its place");
        assertTrue(blocks.stream().anyMatch(block -> block.state().equals("test:windows") && block.y() == 1
                && block.z() == 0), "a tag's place shows the block it gives");
    }

    @Test
    void multiblock_tooBigForASceneIsLeftOut() {
        final int big = GuideBlock.Scene.GRID + 1;
        final ShapePreview preview = new ShapePreview(List.of(ghost(0, 0, 0, List.of("test:brick"), List.of(),
                true)), 0, 0, 0, big, 1, 1);

        assertEquals(List.of(), GuideBlock.Multiblock.blocksOf(preview, first -> first));
        assertThrows(IllegalArgumentException.class, () -> new GuideBlock.Multiblock("", GuideBlock.SceneView.CORNER,
                100, "caption"), "a multiblock's scene names the multiblock");
        assertTrue(new GuideBlock.Multiblock("test:oven", GuideBlock.SceneView.CORNER, 100, "caption")
                .scene(List.of(block(0, 0, 0))).layers(), "it is peeled layer by layer");
    }

    @Test
    void steps_putTakeAndReplaceInTurnFramedRoundEveryBlock() {
        final GuideBlock.SceneBlock grass = new GuideBlock.SceneBlock(0, 0, 0, "minecraft:grass_block", "");
        final GuideBlock.SceneBlock furnace = new GuideBlock.SceneBlock(0, 1, 0, "minecraft:furnace", "");
        final GuideBlock.SceneBlock chest = new GuideBlock.SceneBlock(3, 1, 0, "minecraft:chest", "");
        final GuideBlock.SceneBlock lit = new GuideBlock.SceneBlock(0, 1, 0, "minecraft:furnace[lit=true]", "");
        final GuideBlock.Scene scene = new GuideBlock.Scene(List.of(grass), GuideBlock.SceneView.CORNER, List.of(),
                false, 100, "caption", List.of(
                new GuideBlock.SceneStep("one", List.of(furnace), List.of(), 20),
                new GuideBlock.SceneStep("two", List.of(chest, lit), List.of(), 20),
                new GuideBlock.SceneStep("three", List.of(), List.of(GuideBlock.ScenePart.of(0, 1, 0)), 20)));

        assertEquals(List.of(grass, furnace), scene.blocksAt(0));
        assertEquals(List.of(grass, lit, chest), scene.blocksAt(1), "a block put where one stands takes its place");
        assertEquals(List.of(grass, chest), scene.blocksAt(2), "a place taken is emptied");
        assertEquals(scene.blocksAt(2), scene.blocksAt(9), "past the last step, the last step's");
        assertEquals(4, scene.width(), "framed round the chest of the second step too");
        assertEquals(2, scene.tall());
    }

    @Test
    void steps_aloneMayHoldEveryBlockOfTheScene() {
        final GuideBlock.Scene scene = new GuideBlock.Scene(List.of(), GuideBlock.SceneView.CORNER, List.of(), false,
                100, "caption", List.of(new GuideBlock.SceneStep("one", List.of(block(1, 0, 0)), List.of(), 20)));

        assertEquals(List.of(block(1, 0, 0)), scene.blocksAt(0));
        assertThrows(IllegalArgumentException.class, () -> new GuideBlock.SceneStep("zero", List.of(), List.of(),
                0), "a step is shown a tick at least");
    }

    @Test
    void inWorld_turnsTheScenesBackToTheNorth() {
        final GuideBlock.Scene scene = scene(List.of(block(0, 0, 0), block(0, 0, 2)), List.of());
        final ShapePreview world = scene.inWorld(0);

        assertEquals(3, world.sizeZ());
        assertEquals(-2, world.minZ(), "the scene's back, north, is the world's negative z");
        assertTrue(world.ghosts().stream().allMatch(ghost -> !ghost.controller()
                && ghost.blocks().equals(List.of("minecraft:stone"))));
    }

    @Test
    void structure_turnsItsSouthToTheScenesFrontAndLeavesAirOut() {
        final List<GuideBlock.Structure.Saved> saved = List.of(
                new GuideBlock.Structure.Saved(0, 0, 0, "minecraft:stone", ""),
                new GuideBlock.Structure.Saved(1, 0, 2, "minecraft:oak_slab[type=bottom]", ""),
                new GuideBlock.Structure.Saved(1, 1, 1, "minecraft:air", ""),
                new GuideBlock.Structure.Saved(0, 1, 1, "minecraft:structure_void", ""),
                new GuideBlock.Structure.Saved(2, 0, 0, "minecraft:chest", "{Items:[]}"));
        final List<GuideBlock.SceneBlock> blocks = GuideBlock.Structure.blocksOf(3, 2, 3, saved);

        assertEquals(List.of(new GuideBlock.SceneBlock(0, 0, 2, "minecraft:stone", ""),
                new GuideBlock.SceneBlock(1, 0, 0, "minecraft:oak_slab[type=bottom]", ""),
                new GuideBlock.SceneBlock(2, 0, 2, "minecraft:chest", "{Items:[]}")), blocks);
        assertEquals(List.of(), GuideBlock.Structure.blocksOf(GuideBlock.Scene.GRID + 1, 1, 1, saved),
                "too big for a scene");
        assertTrue(new GuideBlock.Structure("test:hut", GuideBlock.SceneView.CORNER, 100, "caption")
                .scene(blocks).layers(), "it is peeled layer by layer");
    }

    @Test
    void picture_showsItsFramesInTurn() {
        final GuideBlock.Picture strip = new GuideBlock.Picture("test:blink", "", "{}", 32, 16, "caption", 3, 10);

        assertEquals(0, strip.frameAt(9));
        assertEquals(1, strip.frameAt(10));
        assertEquals(2, strip.frameAt(29));
        assertEquals(0, strip.frameAt(30), "from the first again after the last");
        assertThrows(IllegalArgumentException.class, () -> new GuideBlock.Picture("", "kind", "{}", 32, 16,
                "caption", 2, 10), "a drawing is no strip of frames");
    }

    private static ShapePreview.Ghost ghost(final int x, final int y, final int z, final List<String> blocks,
                                            final List<String> tags, final boolean controller) {
        return new ShapePreview.Ghost(x, y, z, y, blocks, tags, controller);
    }

    private static GuideBlock.Scene scene(final List<GuideBlock.SceneBlock> blocks,
                                          final List<GuideBlock.SceneBalloon> balloons) {
        return new GuideBlock.Scene(blocks, GuideBlock.SceneView.CORNER, balloons, false, 100, "caption");
    }

    private static GuideBlock.SceneBlock block(final int x, final int y, final int z) {
        return new GuideBlock.SceneBlock(x, y, z, "minecraft:stone", "");
    }

    private static GuideBlock.SceneBalloon balloon(final int number) {
        return new GuideBlock.SceneBalloon(number, GuideBlock.ScenePart.of(0, 0, 0), "balloon", "");
    }
}
