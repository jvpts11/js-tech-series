/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class UpdateWindowLayoutTest {

    /** The margin a popup keeps from the edges of the window it opens in. */
    private static final int POPUP_MARGIN = 16;
    /** The longest the wait under the furnace says, in English, the language every key is first written in. */
    private static final String LONGEST_WAIT = "The card's furnace is busy with a Workshop job (12 of 64): this one "
            + "waits its turn.";

    @Test
    void layout_isClean() {
        final GuiLayout l = UpdateWindowLayout.layout();
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    void tabs_areCleanEachOnItsOwn() {
        for (final GuiLayout l : new GuiLayout[] {UpdateWindowLayout.enchant(), UpdateWindowLayout.repair(),
                UpdateWindowLayout.smelt()}) {
            assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
        }
    }

    @Test
    void tabs_fitAcrossTheWindow() {
        final int last = UpdateWindowLayout.tabX(UpdateWindowLayout.TABS - 1) + UpdateWindowLayout.TAB_W;
        assertTrue(last <= UpdateWindowLayout.W - UpdateWindowLayout.PAD, "the tabs end at " + last);
    }

    @Test
    void window_fitsTheSmallestInteractor() {
        assertTrue(UpdateWindowLayout.W <= NetworkInteractorLayout.minContentWidth() - POPUP_MARGIN
                        && UpdateWindowLayout.H <= NetworkInteractorLayout.minContentHeight() - POPUP_MARGIN,
                UpdateWindowLayout.W + "x" + UpdateWindowLayout.H + " in " + NetworkInteractorLayout.minContentWidth()
                        + "x" + NetworkInteractorLayout.minContentHeight());
    }

    @Test
    void waitLines_holdTheLongestWait() {
        assertTrue(LONGEST_WAIT.length() <= 2 * UpdateWindowLayout.smallChars(),
                UpdateWindowLayout.smallChars() + " characters a line");
    }

    @Test
    void panel_endsAboveTheFooter() {
        assertTrue(UpdateWindowLayout.PANEL_Y + UpdateWindowLayout.PANEL_H < UpdateWindowLayout.FOOT_Y);
        assertTrue(UpdateWindowLayout.WAIT_Y + 2 * UpdateWindowLayout.LINE <= UpdateWindowLayout.PANEL_H);
        assertTrue(UpdateWindowLayout.COST_Y + UpdateWindowLayout.LINE <= UpdateWindowLayout.PANEL_H);
        assertTrue(UpdateWindowLayout.PAY_Y + UpdateWindowLayout.LINE <= UpdateWindowLayout.PANEL_H);
    }
}
