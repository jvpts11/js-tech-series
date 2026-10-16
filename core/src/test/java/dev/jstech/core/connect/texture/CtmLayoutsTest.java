/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.connect.ConnectedQuadrants;
import org.junit.jupiter.api.Test;

class CtmLayoutsTest {

    private static final int UP = ConnectedQuadrants.UP;
    private static final int RIGHT = ConnectedQuadrants.RIGHT;
    private static final int DOWN_RIGHT = ConnectedQuadrants.DOWN_RIGHT;
    private static final int DOWN = ConnectedQuadrants.DOWN;
    private static final int DOWN_LEFT = ConnectedQuadrants.DOWN_LEFT;
    private static final int LEFT = ConnectedQuadrants.LEFT;
    private static final int UP_LEFT = ConnectedQuadrants.UP_LEFT;
    private static final int EDGES = UP | RIGHT | DOWN | LEFT;
    private static final int EVERY = 255;
    private static final int UP_LEFT_QUARTER = 0;
    private static final int UP_RIGHT_QUARTER = 1;
    private static final int DOWN_RIGHT_QUARTER = 2;
    private static final int DOWN_LEFT_QUARTER = 3;

    @Test
    void quarter_takesItsOwnTextureWhereNeitherEdgeJoins() {
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 0, 0, 2, 2), CtmLayouts.quarter(UP_LEFT_QUARTER, 0));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 1, 1, 2, 2), CtmLayouts.quarter(DOWN_RIGHT_QUARTER, 0));
    }

    @Test
    void quarter_takesTheCompanionsTilesAsCtmLaysThemOut() {
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 0, 0, 4, 4),
                CtmLayouts.quarter(UP_LEFT_QUARTER, EVERY), "joined all round: the first tile");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 2, 0, 4, 4),
                CtmLayouts.quarter(UP_LEFT_QUARTER, UP), "joined above only: the second");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 0, 2, 4, 4),
                CtmLayouts.quarter(UP_LEFT_QUARTER, LEFT), "joined sideways only: the third");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 2, 2, 4, 4),
                CtmLayouts.quarter(UP_LEFT_QUARTER, EDGES), "both edges, no corner: the fourth");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 3, 3, 4, 4),
                CtmLayouts.quarter(DOWN_RIGHT_QUARTER, DOWN | RIGHT), "the same for the down-right quarter");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 1, 1, 4, 4),
                CtmLayouts.quarter(DOWN_RIGHT_QUARTER, DOWN | RIGHT | DOWN_RIGHT));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 3, 0, 4, 4),
                CtmLayouts.quarter(UP_RIGHT_QUARTER, UP | LEFT), "its own edge, above, not its right");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 0, 3, 4, 4),
                CtmLayouts.quarter(DOWN_LEFT_QUARTER, LEFT | UP_LEFT));
    }

    @Test
    void edgesQuarter_takesTheCornerPieceForACornerAlone() {
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 0, 0, 4, 4),
                CtmLayouts.edgesQuarter(UP_LEFT_QUARTER, UP_LEFT));
        assertEquals(CtmLayouts.quarter(UP_LEFT_QUARTER, UP), CtmLayouts.edgesQuarter(UP_LEFT_QUARTER, UP));
    }

    @Test
    void planes_pickTheirEndsAndMiddles() {
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 0, 0, 2, 2), CtmLayouts.horizontal(0), "alone");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 1, 0, 2, 2), CtmLayouts.horizontal(LEFT | RIGHT));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 1, 1, 2, 2), CtmLayouts.horizontal(LEFT));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 0, 1, 2, 2), CtmLayouts.horizontal(RIGHT));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 0, 1, 2, 2), CtmLayouts.vertical(UP | DOWN));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 1, 1, 2, 2), CtmLayouts.vertical(UP));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 1, 0, 2, 2), CtmLayouts.vertical(DOWN));
    }

    @Test
    void simple_joinsAsATwoByTwo() {
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 0, 0, 2, 2), CtmLayouts.simple(0));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 1, 0, 2, 2), CtmLayouts.simple(LEFT | RIGHT));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 0, 1, 2, 2), CtmLayouts.simple(UP | DOWN));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 1, 1, 2, 2), CtmLayouts.simple(EVERY));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 0, 0, 2, 2), CtmLayouts.simple(EDGES),
                "every edge but a missing corner is not joined all round");
    }

    @Test
    void edgesFull_picksByWhereTheChangeMeetsTheFace() {
        assertEquals(new CtmLayouts.Cell(CtmLayouts.OWN, 0, 0, 1, 1), CtmLayouts.edgesFull(0, false));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 1, 2, 4, 4), CtmLayouts.edgesFull(0, true),
                "covered wholly");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 1, 1, 4, 4), CtmLayouts.edgesFull(DOWN, false));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 0, 2, 4, 4), CtmLayouts.edgesFull(RIGHT, false));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 1, 0, 4, 4),
                CtmLayouts.edgesFull(UP_LEFT | DOWN_RIGHT, false), "two corners across");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 3, 0, 4, 4), CtmLayouts.edgesFull(LEFT | UP, false),
                "an inner corner of two edges");
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 2, 1, 4, 4), CtmLayouts.edgesFull(DOWN_LEFT, false));
        assertEquals(new CtmLayouts.Cell(CtmLayouts.COMPANION, 1, 2, 4, 4),
                CtmLayouts.edgesFull(UP | DOWN, false), "across the face");
    }

    @Test
    void cell_givesItsPlaceInItsTexture() {
        final CtmLayouts.Cell cell = new CtmLayouts.Cell(CtmLayouts.COMPANION, 3, 1, 4, 4);
        assertEquals(0.75F, cell.u0());
        assertEquals(0.25F, cell.v0());
        assertEquals(0.25F, cell.width());
        assertEquals(0.25F, cell.height());
    }
}
