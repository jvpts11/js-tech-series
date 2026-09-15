/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class OpenWithLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = OpenWithLayout.layout();
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    void list_showsFourProgramsBetweenTheNoteAndTheButtons() {
        assertEquals(4, (OpenWithLayout.LIST_H - OpenWithLayout.WELL_INSET * 2) / OpenWithLayout.ROW_H);
        assertTrue(OpenWithLayout.NOTE_Y + OpenWithLayout.LINE_H < OpenWithLayout.LIST_Y);
        assertTrue(OpenWithLayout.LIST_Y + OpenWithLayout.LIST_H < OpenWithLayout.BUTTON_Y);
    }

    @Test
    void note_holdsTheSentenceAboutAThreeLetterExtensionWrittenSmall() {
        // "Nothing on this computer opens .fk files yet." is 222 units of the game's font, written at 0.85.
        assertTrue((int) (OpenWithLayout.noteW() / 0.85f) >= 222);
        assertTrue(OpenWithLayout.PAD + OpenWithLayout.noteW() < OpenWithLayout.WIDTH);
    }

    @Test
    void buttons_fitTheirLabelsAndStayInsideTheMargin() {
        // "Only this time" is 65 pixels in the game's font and "Always" 33, with four pixels of margin each side.
        assertTrue(OpenWithLayout.ONCE_W >= 65 + 8);
        assertTrue(OpenWithLayout.ALWAYS_W >= 33 + 8);
        assertTrue(OpenWithLayout.onceX() >= OpenWithLayout.PAD);
    }
}
