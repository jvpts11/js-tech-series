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

class NetworkServicesLayoutTest {

    /** The card's three buttons at the width English labels take, with room for a longer translation. */
    private static final int[] THREE_BUTTONS = {84, 44, 72};
    private static final int NARROWEST = NetworkServicesLayout.MIN_CONTENT_W;

    @Test
    void layout_isCleanAtTheDefaultWidthWithEveryRowItCanSend() {
        final GuiLayout layout = NetworkServicesLayout.layout(NetworkServicesLayout.DEFAULT_CONTENT_W, 16, 32, true, 2,
                THREE_BUTTONS, 64);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void layout_isCleanAtTheNarrowestWindow() {
        final GuiLayout layout = NetworkServicesLayout.layout(NARROWEST, 3, 2, true, 2, THREE_BUTTONS, 64);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void layout_isCleanWithNothingListed() {
        final GuiLayout layout = NetworkServicesLayout.layout(NetworkServicesLayout.DEFAULT_CONTENT_W, 0, 0, false, 2,
                new int[] {50}, 20);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void buttonsX_layTheButtonsFromTheRightEndWithAGap() {
        final int[] xs = NetworkServicesLayout.buttonsX(300, 80, 40, 60);

        assertEquals(300 - NetworkServicesLayout.PAD - 60, xs[2]);
        assertEquals(xs[2] - NetworkServicesLayout.BUTTON_GAP - 40, xs[1]);
        assertEquals(xs[1] - NetworkServicesLayout.BUTTON_GAP - 80, xs[0]);
    }

    @Test
    void buttonsX_leaveTheHostLineRoomAtTheNarrowestWindow() {
        final int[] xs = NetworkServicesLayout.buttonsX(NARROWEST, THREE_BUTTONS);

        assertTrue(xs[0] > NetworkServicesLayout.NAME_X + 30, "the host line keeps some room; it starts at " + xs[0]);
    }

    @Test
    void sections_growWithTheRowsAndTheNote() {
        final NetworkServicesLayout.Sections plain = NetworkServicesLayout.sections(1, 0, false, 2);
        final NetworkServicesLayout.Sections busy = NetworkServicesLayout.sections(3, 2, true, 2);

        assertEquals(plain.subframesHeading() + 2 * NetworkServicesLayout.ROW_H, busy.subframesHeading());
        assertTrue(busy.servicesHeading() - plain.servicesHeading()
                >= 2 * NetworkServicesLayout.ROW_H + NetworkServicesLayout.NOTE_H, "the note takes its own room");
    }

    @Test
    void sections_startUnderTheCard() {
        final NetworkServicesLayout.Sections at = NetworkServicesLayout.sections(1, 1, false, 2);

        assertTrue(at.installedHeading() >= NetworkServicesLayout.CARD_H, "the first heading is under the card");
    }
}
