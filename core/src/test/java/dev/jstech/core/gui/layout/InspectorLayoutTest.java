/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InspectorLayoutTest {

    @Test
    void layout_isCleanAtItsFullest() {
        final GuiLayout layout = InspectorLayout.layout(InspectorLayout.WIDTH, 16, 6, 3);
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void layout_isCleanAtItsNarrowest() {
        final GuiLayout layout = InspectorLayout.layout(InspectorLayout.MIN_WIDTH, 16, 6, 3);
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void left_standsBesideTheScreenOrAtTheEdge() {
        assertEquals(199 - InspectorLayout.GAP - InspectorLayout.WIDTH, InspectorLayout.left(199));
        assertEquals(InspectorLayout.EDGE, InspectorLayout.left(20), "no room beside: at the screen's edge");
    }

    @Test
    void width_narrowsToTheRoomBesideTheScreen() {
        assertEquals(InspectorLayout.WIDTH, InspectorLayout.width(300));
        final int room = 152 - InspectorLayout.GAP - InspectorLayout.EDGE;
        assertEquals(room, InspectorLayout.width(152));
        assertEquals(152 - InspectorLayout.GAP, InspectorLayout.left(152) + InspectorLayout.width(152),
                "it ends where the room before the screen starts");
        assertEquals(InspectorLayout.MIN_WIDTH, InspectorLayout.width(40));
    }

    @Test
    void buttons_comeAfterEveryProblem() {
        assertEquals(InspectorLayout.problemsY(4) + InspectorLayout.HEADING + 3 * InspectorLayout.PROBLEM
                + InspectorLayout.PAD, InspectorLayout.buttonsY(4, 3));
    }
}
