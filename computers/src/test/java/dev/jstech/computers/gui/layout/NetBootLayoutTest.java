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

class NetBootLayoutTest {

    @Test
    void agent_isCleanWithEveryRowFilled() {
        final GuiLayout layout = NetBootLayout.agent();
        assertTrue(layout.isClean(), layout.overlaps() + " " + layout.outOfBounds());
    }

    @Test
    void agent_keepsTheKeysInsideTheBox() {
        assertTrue(NetBootLayout.KEYS_Y + 7 < NetBootLayout.BOX_Y + NetBootLayout.BOX_H,
                "the keys line ends above the box's bottom border");
    }

    @Test
    void agent_keepsTheAddressesAboveTheBoxTitle() {
        final int lastLine = NetBootLayout.AGENT_Y + (NetBootLayout.AGENT_LINES - 1) * NetBootLayout.AGENT_STEP + 7;
        assertTrue(lastLine < NetBootLayout.BOX_Y - NetBootLayout.TITLE_RISE,
                "the last address line ends above the menu's title");
    }
}
