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
        assertClean(ConfigScreenLayout.layout(480, 270, 2, EVERY_KIND));
        assertClean(ConfigScreenLayout.layout(640, 360, 2, EVERY_KIND));
    }

    @Test
    void nameWidth_leavesEveryNameRoomBesideItsControlAndItsDefaultButton() {
        for (final ConfigDraft.Control control : EVERY_KIND) {
            assertTrue(ConfigScreenLayout.nameWidth(ConfigScreenLayout.SMALLEST_WIDTH, control, true, true) >= 40,
                    () -> control + " leaves its name too little room");
        }
    }

    @Test
    void railWidth_narrowsOnASmallScreenOnly() {
        assertEquals(ConfigScreenLayout.NARROW_RAIL_WIDTH, ConfigScreenLayout.railWidth(320));
        assertEquals(ConfigScreenLayout.RAIL_WIDTH, ConfigScreenLayout.railWidth(480));
    }

    @Test
    void cardHeight_growsByALineOfDescription() {
        assertEquals(ConfigScreenLayout.NOTE_LINE,
                ConfigScreenLayout.cardHeight(2) - ConfigScreenLayout.cardHeight(1));
    }

    @Test
    void controls_endAtTheCardsInnerRightEdge() {
        for (final ConfigDraft.Control control : EVERY_KIND) {
            assertEquals(ConfigScreenLayout.contentRight(480) - ConfigScreenLayout.CARD_INSET,
                    ConfigScreenLayout.controlX(480, control, true) + ConfigScreenLayout.controlWidth(control, true));
        }
    }

    @Test
    void tabs_shareTheRailWithoutOverlapping() {
        for (int tab = 0; tab < 2; tab++) {
            assertTrue(ConfigScreenLayout.tabX(480, tab, 3) + ConfigScreenLayout.tabWidth(480, 3)
                    <= ConfigScreenLayout.tabX(480, tab + 1, 3));
        }
        assertTrue(ConfigScreenLayout.tabX(480, 2, 3) + ConfigScreenLayout.tabWidth(480, 3)
                <= ConfigScreenLayout.railWidth(480) - ConfigScreenLayout.TAB_INSET);
    }

    private static void assertClean(final GuiLayout layout) {
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }
}
