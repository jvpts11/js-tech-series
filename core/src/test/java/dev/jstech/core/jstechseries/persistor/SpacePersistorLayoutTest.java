/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class SpacePersistorLayoutTest {

    @Test
    void layout_hasNothingOverlappingNorOutside() {
        final GuiLayout layout = SpacePersistorLayout.layout();
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void face_fitsTheWindowUnderTheTabs() {
        assertTrue(SpacePersistorLayout.FACE_Y >= SpacePersistorLayout.WINDOW_TOP, "the face starts in the window");
        assertTrue(SpacePersistorLayout.FACE_Y + SpacePersistorLayout.FACE_HEIGHT
                <= SpacePersistorLayout.WINDOW_BOTTOM, "and ends inside it");
        assertTrue(SpacePersistorLayout.FACE_X + SpacePersistorLayout.FACE_WIDTH <= SpacePersistorLayout.WIDTH,
                "the face fits the screen across");
    }

    @Test
    void zooms_showWholeChunksWithItsOwnInTheMiddle() {
        for (int zoom = 0; zoom < SpacePersistorLayout.zooms(); zoom++) {
            final int cell = SpacePersistorLayout.cell(zoom);
            assertEquals(SpacePersistorLayout.DISPLAY, SpacePersistorLayout.across(zoom) * cell,
                    "whole chunks fill the display at zoom " + zoom);
            assertEquals(SpacePersistorLayout.across(zoom) / 2, SpacePersistorLayout.middle(zoom));
        }
        assertEquals(SpacePersistorLayout.cell(SpacePersistorLayout.zooms() - 1), SpacePersistorLayout.cell(99),
                "a zoom past the last is the last");
    }

    @Test
    void chunkAt_findsTheChunkUnderThePointerAndNothingOffTheDisplay() {
        final int middle = SpacePersistorLayout.middle(0);
        final int cell = SpacePersistorLayout.cell(0);
        final double x = SpacePersistorLayout.DISPLAY_X + middle * cell + 1;
        final double y = SpacePersistorLayout.DISPLAY_Y + middle * cell + 1;
        assertEquals(OptionalLong.of(PersistorShapes.pack(0, 0)), SpacePersistorLayout.chunkAt(x, y, 0),
                "its own chunk is in the middle");
        assertEquals(OptionalLong.of(PersistorShapes.pack(-1, 2)), SpacePersistorLayout.chunkAt(x - cell,
                y + 2 * cell, 0), "a chunk west and two south");
        assertTrue(SpacePersistorLayout.chunkAt(SpacePersistorLayout.DISPLAY_X - 1, y, 0).isEmpty(),
                "left of the display is no chunk");
        assertTrue(SpacePersistorLayout.chunkAt(x, SpacePersistorLayout.DISPLAY_Y + SpacePersistorLayout.DISPLAY,
                0).isEmpty(), "nor is below it");
    }

    @Test
    void controls_answerWhereTheyAreDrawn() {
        assertEquals(2, SpacePersistorLayout.keyAt(SpacePersistorLayout.COLUMN_X + 2 * SpacePersistorLayout.KEY_PITCH
                + 3, SpacePersistorLayout.KEYS_Y + 3), "the third key");
        assertEquals(-1, SpacePersistorLayout.keyAt(SpacePersistorLayout.COLUMN_X, SpacePersistorLayout.KEYS_Y - 2),
                "above the keys is none");
        assertEquals(0, SpacePersistorLayout.drumAt(SpacePersistorLayout.WHEEL_X + 2, SpacePersistorLayout.WHEEL_Y
                + 1), "the tens' drum");
        assertEquals(1, SpacePersistorLayout.drumAt(SpacePersistorLayout.WHEEL_X + SpacePersistorLayout.WHEEL_DIGIT
                + 2, SpacePersistorLayout.WHEEL_Y + 1), "the units'");
        assertTrue(SpacePersistorLayout.drumUp(SpacePersistorLayout.WHEEL_Y + 1), "the top half turns it up");
        assertTrue(!SpacePersistorLayout.drumUp(SpacePersistorLayout.WHEEL_Y + SpacePersistorLayout.WHEEL_HEIGHT - 1),
                "the bottom half down");
        assertTrue(SpacePersistorLayout.onSwitch(SpacePersistorLayout.SWITCH_X + 1, SpacePersistorLayout.SWITCH_Y
                + 1), "the switch");
        assertTrue(SpacePersistorLayout.onName(SpacePersistorLayout.COLUMN_X + 1, SpacePersistorLayout.NAME_Y + 1),
                "the name plate");
        assertEquals(1, SpacePersistorLayout.zoomAt(SpacePersistorLayout.DISPLAY_X + SpacePersistorLayout.ZOOM_X + 1,
                SpacePersistorLayout.DISPLAY_Y + SpacePersistorLayout.ZOOM_Y + SpacePersistorLayout.ZOOM_PITCH + 1),
                "the lower zoom key takes it further");
    }
}
