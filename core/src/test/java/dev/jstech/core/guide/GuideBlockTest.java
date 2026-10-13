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

import java.util.List;
import org.junit.jupiter.api.Test;

class GuideBlockTest {

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
