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
        final GuiLayout layout = NetworkServicesLayout.layout(NetworkServicesLayout.DEFAULT_CONTENT_W, 16, 32, 2,
                THREE_BUTTONS, 64);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void layout_isCleanAtTheNarrowestWindow() {
        final GuiLayout layout = NetworkServicesLayout.layout(NARROWEST, 3, 2, 2, THREE_BUTTONS, 64);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void layout_isCleanWithNothingListed() {
        final GuiLayout layout = NetworkServicesLayout.layout(NetworkServicesLayout.DEFAULT_CONTENT_W, 0, 0, 2,
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
    void roleBoxesX_layTheBoxesFromTheRightEndAndLeaveTheLabelRoom() {
        final int[] xs = NetworkServicesLayout.roleBoxesX(NARROWEST, NetworkServicesLayout.ROLE_BOX_WIDTHS);

        assertEquals(NARROWEST - NetworkServicesLayout.PAD - NetworkServicesLayout.ROLE_BOX_WIDTHS[1], xs[1]);
        assertEquals(xs[1] - NetworkServicesLayout.ROLE_BOX_GAP - NetworkServicesLayout.ROLE_BOX_WIDTHS[0], xs[0]);
        assertTrue(xs[0] > NetworkServicesLayout.PAD + 60, "\"Role on\" and the machine keep some room");
    }

    @Test
    void sections_putTheSubframesRightUnderTheRole() {
        final NetworkServicesLayout.Sections at = NetworkServicesLayout.sections(2, 2, 2);

        assertTrue(at.subframesHeading() > NetworkServicesLayout.ROLE_Y + NetworkServicesLayout.LINE_H);
        assertTrue(at.installedHeading() > at.succession() + NetworkServicesLayout.SUCCESSION_LINES
                * NetworkServicesLayout.LINE_H);
    }

    @Test
    void buttonsX_leaveTheHostLineRoomAtTheNarrowestWindow() {
        final int[] xs = NetworkServicesLayout.buttonsX(NARROWEST, THREE_BUTTONS);

        assertTrue(xs[0] > NetworkServicesLayout.NAME_X + 30, "the host line keeps some room; it starts at " + xs[0]);
    }

    @Test
    void sections_growWithTheRows() {
        final NetworkServicesLayout.Sections plain = NetworkServicesLayout.sections(1, 0, 2);
        final NetworkServicesLayout.Sections busy = NetworkServicesLayout.sections(3, 2, 2);

        assertEquals(plain.installedHeading() + NetworkServicesLayout.ROW_H, busy.installedHeading());
        assertEquals(busy.installedHeading() - plain.installedHeading() + 2 * NetworkServicesLayout.ROW_H,
                busy.servicesHeading() - plain.servicesHeading());
    }

    @Test
    void sections_startUnderTheCardAndTheRole() {
        final NetworkServicesLayout.Sections at = NetworkServicesLayout.sections(1, 1, 2);

        assertTrue(at.subframesHeading() >= NetworkServicesLayout.ROLE_Y + NetworkServicesLayout.LINE_H,
                "the first heading is under the card and the role");
    }
}
