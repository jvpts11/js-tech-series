/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SceneProjectionTest {

    private static final double CLOSE = 1.0E-6;
    /** One block, seen head on from the south, on a frame 100 pixels square. */
    private static final GuideBlock.Scene ONE_BLOCK = new GuideBlock.Scene(List.of(
            new GuideBlock.SceneBlock(0, 0, 0, "minecraft:stone", "")), GuideBlock.SceneView.CORNER, List.of(),
            false, 100, "caption");

    @Test
    void project_putsTheSceneMiddleAtTheFramesMiddle() {
        final SceneProjection projection = new SceneProjection(ONE_BLOCK, new GuideBlock.SceneView(-35, 25, 1),
                100, 80);

        final SceneProjection.Point middle = projection.project(0.5, 0.5, 0.5);
        assertEquals(50, middle.x(), CLOSE);
        assertEquals(40, middle.y(), CLOSE);
    }

    @Test
    void project_seenHeadOnRunsEastAcrossAndUpUpTheFrame() {
        final SceneProjection projection = new SceneProjection(ONE_BLOCK, new GuideBlock.SceneView(0, 0, 1), 100,
                100);

        final SceneProjection.Point east = projection.project(1, 0.5, 0.5);
        final SceneProjection.Point top = projection.project(0.5, 1, 0.5);
        final SceneProjection.Point south = projection.project(0.5, 0.5, 1);
        assertEquals(50 + projection.scale() / 2, east.x(), CLOSE);
        assertEquals(50 - projection.scale() / 2, top.y(), CLOSE, "up is up the frame");
        assertTrue(south.depth() > 0, "the south side faces the reader");
    }

    @Test
    void project_tiltedDownShowsTheTopAndTurnedBringsTheSouthSideRight() {
        final SceneProjection tilted = new SceneProjection(ONE_BLOCK, new GuideBlock.SceneView(0, 90, 1), 100, 100);
        final SceneProjection turned = new SceneProjection(ONE_BLOCK, new GuideBlock.SceneView(90, 0, 1), 100, 100);

        assertTrue(tilted.project(0.5, 1, 0.5).depth() > tilted.project(0.5, 0, 0.5).depth(),
                "looking straight down, the top is nearest");
        assertEquals(50 + turned.scale() / 2, turned.project(0.5, 0.5, 1).x(), CLOSE,
                "turned a quarter round, the south side stands at the right");
    }

    @Test
    void scale_fitsTheWholeSceneTurnedAnyWayAndGrowsWithTheZoom() {
        final GuideBlock.Scene wide = new GuideBlock.Scene(List.of(new GuideBlock.SceneBlock(0, 0, 0,
                "minecraft:stone", ""), new GuideBlock.SceneBlock(3, 0, 0, "minecraft:stone", "")),
                GuideBlock.SceneView.CORNER, List.of(), false, 100, "caption");

        final SceneProjection near = new SceneProjection(wide, new GuideBlock.SceneView(0, 0, 2), 200, 100);
        assertEquals(100 / Math.sqrt(4 * 4 + 1 + 1) * 2, near.scale(), CLOSE,
                "the frame's shorter side over the scene's longest line, twice as near");
    }

    @Test
    void bounds_holdEveryCornerOfAPart() {
        final SceneProjection projection = new SceneProjection(ONE_BLOCK, new GuideBlock.SceneView(-35, 25, 1),
                100, 100);
        final GuideBlock.ScenePart tray = GuideBlock.ScenePart.of(0, 0, 0).box(4, 6, 12, 12, 8, 16);

        final SceneProjection.Bounds bounds = projection.bounds(tray);
        final SceneProjection.Point middle = projection.middleOf(tray);
        assertTrue(bounds.left() < middle.x() && middle.x() < bounds.right());
        assertTrue(bounds.top() < middle.y() && middle.y() < bounds.bottom());
        final SceneProjection.Bounds whole = projection.bounds(GuideBlock.ScenePart.of(0, 0, 0));
        assertTrue(whole.left() <= bounds.left() && whole.right() >= bounds.right(), "a part is inside its block");
    }
}
