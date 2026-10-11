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

class ServerRackLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = ServerRackLayout.layout();
        assertTrue(l.isClean(), () -> "overlaps " + l.overlaps() + ", out of bounds " + l.outOfBounds());
        assertTrue(l.zeroSizedSolids().isEmpty(), () -> "zero-sized " + l.zeroSizedSolids());
    }

    @Test
    void rows_startBelowTheHeaderAndEndAboveTheInventory() {
        assertTrue(ServerRackLayout.ROW_Y0 >= ServerRackLayout.HEADER_Y + ServerRackLayout.HEADER_H,
                "the first rack unit must clear the header bar");
        final int lastRowBottom = ServerRackLayout.rowY(ServerRackLayout.ROWS - 1) + ServerRackLayout.ROW_PITCH;
        assertTrue(lastRowBottom <= ServerRackLayout.INV_Y,
                "the last rack unit (" + lastRowBottom + ") must end above the player inventory");
    }

    @Test
    void units_standBetweenThePosts() {
        // The unit's own slot frame starts after the left post, and its power button ends before the right one.
        assertTrue(ServerRackLayout.serverItemX() - 1 >= ServerRackLayout.RAIL_L, "server slot clears the left post");
        assertTrue(ServerRackLayout.PWR_X + ServerRackLayout.PWR_W <= ServerRackLayout.RAIL_R - 2,
                "power button clears the right post");
    }

    @Test
    void frontSlots_endBeforeThePowerButton() {
        final int lastSlotEnd = ServerRackLayout.frontItemX(ServerRackLayout.FRONT_SLOTS - 1) + 17;
        assertTrue(lastSlotEnd <= ServerRackLayout.PWR_X,
                "front slots (" + lastSlotEnd + ") must not run into the power button");
    }

    @Test
    void card_standsRightOfTheCabinetAndHoldsEveryLine() {
        assertTrue(ServerRackLayout.CARD_X >= ServerRackLayout.RAIL_R + 2, "the card clears the right post");
        final int lastValueBottom = ServerRackLayout.cardLineY(ServerRackLayout.CARD_LINES - 1) + 7 + 9;
        assertTrue(lastValueBottom <= ServerRackLayout.CARD_Y + ServerRackLayout.CARD_H,
                "the last line (" + lastValueBottom + ") must end inside the card");
    }

    @Test
    void pwrY_centresTheButtonOnItsUnit() {
        assertEquals(ServerRackLayout.rowY(0) + 3, ServerRackLayout.pwrY(0, 1));
        assertEquals(ServerRackLayout.rowY(2) + 12, ServerRackLayout.pwrY(2, 2));
    }

    @Test
    void rowY_advancesByThePitch() {
        assertEquals(ServerRackLayout.ROW_Y0, ServerRackLayout.rowY(0));
        assertEquals(ServerRackLayout.ROW_Y0 + ServerRackLayout.ROW_PITCH, ServerRackLayout.rowY(1));
    }
}
