/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class NetworkLinksLayoutTest {

    /** The Devices list at the window's default width and at its narrowest: the window less its padding. */
    private static final int DEFAULT_W = NetworkServicesLayout.DEFAULT_W - NetworkServicesLayout.WINDOW_PAD;
    private static final int NARROWEST = NetworkServicesLayout.MIN_W - NetworkServicesLayout.WINDOW_PAD;
    /** The tab's height at the window's default height and at its lowest, under the tabs and the network's line. */
    private static final int DEFAULT_H = NetworkServicesLayout.DEFAULT_H - 38;
    private static final int LOWEST = 150 - 38;
    /** The longest link a node shows in English: a fibre whose link is down. */
    private static final String LONGEST_LINK = "Fibre · link down";

    @Test
    void devices_isCleanAtTheDefaultSizeWithALostLink() {
        final GuiLayout layout = NetworkLinksLayout.devices(DEFAULT_W, DEFAULT_H, true);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void devices_isCleanAtTheNarrowestAndLowestWindow() {
        final GuiLayout layout = NetworkLinksLayout.devices(NARROWEST, LOWEST, true);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void devices_isCleanWithEveryLinkUp() {
        final GuiLayout layout = NetworkLinksLayout.devices(DEFAULT_W, DEFAULT_H, false);

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void linkColumn_holdsTheLongestLinkAfterTheCardSquare() {
        final float needed = NetworkLinksLayout.CARD_ROOM + LONGEST_LINK.length() * GuiLayout.GLYPH_WIDTH;

        assertTrue(needed <= NetworkLinksLayout.LINK_W, "the link column needs " + needed);
    }

    @Test
    void nameW_leavesANameRoomAtEveryWidth() {
        assertTrue(NetworkLinksLayout.nameW(DEFAULT_W) >= 60, "names at the default width");
        assertTrue(NetworkLinksLayout.nameW(NARROWEST) >= 60, "names at the narrowest");
    }

    @Test
    void showsType_givesTheTypeColumnUpWhenNarrow() {
        assertTrue(NetworkLinksLayout.showsType(DEFAULT_W), "the default width shows the type");
        assertFalse(NetworkLinksLayout.showsType(NARROWEST), "the narrowest gives it to the names");
    }

    @Test
    void listH_leavesRoomForTheFooterAndItsGap() {
        final int h = DEFAULT_H;

        assertTrue(NetworkLinksLayout.HEADER_H + NetworkLinksLayout.listH(h, true) + NetworkLinksLayout.FOOTER_GAP
                <= NetworkLinksLayout.footerY(h), "the list ends before the footer");
    }
}
