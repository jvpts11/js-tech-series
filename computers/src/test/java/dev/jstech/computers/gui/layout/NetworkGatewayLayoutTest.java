/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class NetworkGatewayLayoutTest {

    @Test
    void layout_hasNoOverlapsOrOverflow() {
        final GuiLayout l = NetworkGatewayLayout.layout();
        assertTrue(l.isClean(), "Network Gateway layout is not clean: overlaps=" + l.overlaps()
                + " outOfBounds=" + l.outOfBounds());
    }

    @Test
    void layout_fitsTheScreenHeightBudget() {
        assertTrue(NetworkGatewayLayout.HEIGHT <= 256,
                "the gateway screen must fit the GUI height budget; got " + NetworkGatewayLayout.HEIGHT);
    }

    @Test
    void lights_sitRightOfTheTitleInsideThePanel() {
        final int titleEnd = NetworkGatewayLayout.TITLE_X + "NETWORK GATEWAY".length() * 6;
        assertTrue(NetworkGatewayLayout.LED_JS_X >= titleEnd + 5);
        assertTrue(NetworkGatewayLayout.LED_CC_LABEL_X + 2 * 6 * NetworkGatewayLayout.INFO_SCALE
                <= NetworkGatewayLayout.WIDTH - 2);
    }

    @Test
    void statusLines_endAboveTheBufferCaption() {
        final int lastLineBottom = NetworkGatewayLayout.INFO_Y + NetworkGatewayLayout.LINE_H * 2 + 7;
        assertTrue(lastLineBottom <= NetworkGatewayLayout.BUFFER_CAPTION_Y,
                "the status lines must end above the buffer caption; end at " + lastLineBottom);
    }

    /*
     * The panel is the vanilla 176 wide, so the nine slot frames run from x=8 to x=170 and leave the
     * same six pixels on the right that every vanilla inventory does.
     */
    @Test
    void buffer_spansTheNineSlotsInsideThePanel() {
        assertTrue(NetworkGatewayLayout.bufferX(8) + NetworkGatewayLayout.SLOT <= NetworkGatewayLayout.WIDTH - 6);
        assertTrue(NetworkGatewayLayout.BUFFER_Y + NetworkGatewayLayout.SLOT <= NetworkGatewayLayout.INV_LABEL_Y - 2);
    }

    @Test
    void inventory_isInsideThePanel() {
        assertTrue(NetworkGatewayLayout.INV_X + 9 * NetworkGatewayLayout.SLOT <= NetworkGatewayLayout.WIDTH - 6);
        assertTrue(NetworkGatewayLayout.INV_Y + 58 + NetworkGatewayLayout.SLOT <= NetworkGatewayLayout.HEIGHT - 4);
    }
}
