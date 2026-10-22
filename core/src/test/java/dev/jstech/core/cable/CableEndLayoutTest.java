/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class CableEndLayoutTest {

    @Test
    void layout_keepsEveryPieceApartAndInside() {
        final GuiLayout layout = CableEndLayout.layout();

        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void modeKeys_endInsideTheWindow() {
        final int last = CableEndLayout.MODES - 1;

        assertTrue(CableEndLayout.modeX(last) + CableEndLayout.modeWidth(last) <= CableEndLayout.WIDTH - 4);
    }

    @Test
    void ghostAt_findsEachFilterSlotAndNothingBeside() {
        for (int k = 0; k < CableEndLayout.GHOSTS; k++) {
            assertEquals(k, CableEndLayout.ghostAt(CableEndLayout.ghostX(k) + 8, CableEndLayout.GHOST_Y + 8));
        }
        assertEquals(-1, CableEndLayout.ghostAt(CableEndLayout.ghostX(0) + 8, CableEndLayout.GHOST_Y - 3));
        assertEquals(-1, CableEndLayout.ghostAt(CableEndLayout.ghostX(CableEndLayout.GHOSTS - 1) + 20,
                CableEndLayout.GHOST_Y + 8));
    }

    @Test
    void channelAt_findsEachChannelAndNothingBetween() {
        for (int k = 0; k < CableEndLayout.CHANNELS; k++) {
            assertEquals(k, CableEndLayout.channelAt(CableEndLayout.channelX(k) + 1, CableEndLayout.channelY(k) + 1));
        }
        assertEquals(-1, CableEndLayout.channelAt(CableEndLayout.channelX(0) + CableEndLayout.CHANNEL_WIDTH,
                CableEndLayout.channelY(0) + 1), "the gap between two channels is neither's");
    }

    @Test
    void onRedstone_findsTheRedstoneKeyBesideTheFilterKey() {
        assertTrue(CableEndLayout.onRedstone(CableEndLayout.REDSTONE_X + 8, CableEndLayout.REDSTONE_Y + 8));
        assertTrue(!CableEndLayout.onRedstone(CableEndLayout.FILTER_X + 8, CableEndLayout.FILTER_Y + 8),
                "the filter's key is not the redstone key");
        assertTrue(CableEndLayout.REDSTONE_X > CableEndLayout.FILTER_X + CableEndLayout.FILTER_WIDTH
                && CableEndLayout.REDSTONE_X + CableEndLayout.KEY_HEIGHT <= CableEndLayout.WIDTH - 4);
    }

    @Test
    void inventory_standsInItsOwnWindow() {
        assertTrue(CableEndLayout.INVENTORY_TOP > CableEndLayout.PANEL_HEIGHT);
        assertTrue(CableEndLayout.INVENTORY_Y + 58 + 16 < CableEndLayout.HEIGHT);
        assertTrue(CableEndLayout.HINT_Y + 7 < CableEndLayout.PANEL_HEIGHT);
    }
}
