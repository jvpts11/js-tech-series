/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class StructureProjectorLayoutTest {

    @Test
    void layout_hasNothingOverlappingNorOutside() {
        final GuiLayout layout = StructureProjectorLayout.layout();
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void rowAt_findsTheListsRowsAndNothingBesideThem() {
        final int x = StructureProjectorLayout.LIST_X + 10;
        final int rows = StructureProjectorLayout.ROWS * StructureProjectorLayout.ROW_HEIGHT;
        assertEquals(0, StructureProjectorLayout.rowAt(x, StructureProjectorLayout.LIST_Y + 3));
        assertEquals(StructureProjectorLayout.ROWS - 1, StructureProjectorLayout.rowAt(x,
                StructureProjectorLayout.LIST_Y + 2 + rows - 1));
        assertEquals(-1, StructureProjectorLayout.rowAt(StructureProjectorLayout.LIST_X - 1,
                StructureProjectorLayout.LIST_Y + 3));
    }

    @Test
    void controls_answerWhereTheyAreDrawn() {
        assertTrue(StructureProjectorLayout.onKnob(StructureProjectorLayout.KNOB_X, StructureProjectorLayout.KNOB_Y));
        assertTrue(StructureProjectorLayout.onToggle(StructureProjectorLayout.TOGGLE_X + 1,
                StructureProjectorLayout.MIRROR_Y + 1, StructureProjectorLayout.MIRROR_Y));
        assertFalse(StructureProjectorLayout.onToggle(StructureProjectorLayout.TOGGLE_X + 1,
                StructureProjectorLayout.MISSING_Y + 1, StructureProjectorLayout.MIRROR_Y));
        assertTrue(StructureProjectorLayout.onWheel(StructureProjectorLayout.WHEEL_X + 2,
                StructureProjectorLayout.WHEEL_Y + 2));
        assertTrue(StructureProjectorLayout.onRocker(StructureProjectorLayout.ROCKER_X + 2,
                StructureProjectorLayout.ROCKER_Y + 2));
    }
}
