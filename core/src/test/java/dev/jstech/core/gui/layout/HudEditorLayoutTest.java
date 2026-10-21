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

class HudEditorLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout layout = HudEditorLayout.layout();
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void window_standsALittleUnderTheMiddle() {
        assertEquals(140, HudEditorLayout.left(480));
        assertEquals(146, HudEditorLayout.top(270));
    }

    @Test
    void buttons_fitInTheWindowWithItsRoomAtTheRight() {
        assertTrue(HudEditorLayout.doneX() + HudEditorLayout.DONE_WIDTH <= HudEditorLayout.WIDTH
                - HudEditorLayout.PAD, "Done ends inside the window's room");
    }
}
