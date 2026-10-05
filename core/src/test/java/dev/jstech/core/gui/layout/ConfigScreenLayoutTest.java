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

import dev.jstech.core.config.ConfigDraft;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigScreenLayoutTest {

    private static final List<ConfigDraft.Control> EVERY_KIND = List.of(ConfigDraft.Control.TOGGLE,
            ConfigDraft.Control.NUMBER, ConfigDraft.Control.CHOICE, ConfigDraft.Control.TEXT, ConfigDraft.Control.FIXED,
            ConfigDraft.Control.NUMBER, ConfigDraft.Control.TOGGLE, ConfigDraft.Control.TEXT);

    @Test
    void layout_fitsEveryKindOfControlOnTheSmallestScreen() {
        assertClean(ConfigScreenLayout.layout(ConfigScreenLayout.SMALLEST_WIDTH, ConfigScreenLayout.SMALLEST_HEIGHT,
                2, EVERY_KIND));
    }

    @Test
    void layout_fitsOnATypicalScreenWithALongNote() {
        assertClean(ConfigScreenLayout.layout(480, 270, 4, EVERY_KIND));
        assertClean(ConfigScreenLayout.layout(640, 360, 3, EVERY_KIND));
    }

    @Test
    void layout_leavesEveryNameRoomBesideItsControl() {
        for (final ConfigDraft.Control control : EVERY_KIND) {
            assertTrue(ConfigScreenLayout.nameWidth(ConfigScreenLayout.SMALLEST_WIDTH, control) >= 40,
                    () -> control + " leaves its name too little room");
        }
    }

    @Test
    void visibleRows_stopAboveTheFooter() {
        final int rows = ConfigScreenLayout.visibleRows(ConfigScreenLayout.SMALLEST_HEIGHT, 2);
        assertTrue(ConfigScreenLayout.rowY(2, rows - 1) + ConfigScreenLayout.ROW_HEIGHT
                <= ConfigScreenLayout.rowsBottom(ConfigScreenLayout.SMALLEST_HEIGHT));
        assertEquals(rows + 1, ConfigScreenLayout.visibleRows(ConfigScreenLayout.SMALLEST_HEIGHT
                + ConfigScreenLayout.ROW_HEIGHT, 2));
    }

    @Test
    void controls_endAtTheContentsRightEdge() {
        for (final ConfigDraft.Control control : EVERY_KIND) {
            assertEquals(ConfigScreenLayout.contentRight(480) - 4,
                    ConfigScreenLayout.controlX(480, control) + ConfigScreenLayout.controlWidth(control));
        }
    }

    private static void assertClean(final GuiLayout layout) {
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }
}
