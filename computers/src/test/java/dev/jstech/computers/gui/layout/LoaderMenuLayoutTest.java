/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class LoaderMenuLayoutTest {

    @Test
    void layout_isCleanWithEveryEntryAMenuMayHold() {
        final GuiLayout l = LoaderMenuLayout.layout(BootMenu.MOST_ENTRIES);
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void layout_isCleanWithTheTwoEntriesEveryLoaderHas() {
        assertTrue(LoaderMenuLayout.layout(2).isClean());
    }

    @Test
    void lastRowBottom_staysInsideTheBoxThatHoldsTheRows() {
        assertTrue(LoaderMenuLayout.lastRowBottom() < LoaderMenuLayout.BOX_Y + LoaderMenuLayout.BOX_H,
                "a full menu's last row ends at " + LoaderMenuLayout.lastRowBottom());
    }

    @Test
    void itemRoom_endsBeforeTheRightRuleOfTheBox() {
        assertTrue(LoaderMenuLayout.ITEMS_X + LoaderMenuLayout.itemRoom()
                < LoaderMenuLayout.BOX_X + LoaderMenuLayout.BOX_W);
    }

    @Test
    void lockup_standsClearOfTheBoxAndAboveTheCount() {
        assertTrue(LoaderMenuLayout.LOCKUP_X > LoaderMenuLayout.BOX_X + LoaderMenuLayout.BOX_W);
        assertTrue(LoaderMenuLayout.LOCKUP_X + LoaderMenuLayout.LOCKUP_W < LoaderMenuLayout.WIDTH);
        assertTrue(LoaderMenuLayout.LOCKUP_Y + LoaderMenuLayout.LOCKUP_H < LoaderMenuLayout.FOOT_Y);
    }
}
