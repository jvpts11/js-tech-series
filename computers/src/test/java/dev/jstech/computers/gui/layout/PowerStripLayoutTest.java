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

class PowerStripLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = PowerStripLayout.layout();
        assertTrue(l.isClean(), l.overlaps() + " " + l.outOfBounds());
    }

    @Test
    void stripX_standsLeftOfTheFrameWithAirBetween() {
        assertEquals(100 - PowerStripLayout.GAP - PowerStripLayout.W, PowerStripLayout.stripX(100));
    }

    @Test
    void stripX_neverLeavesTheScreen() {
        assertEquals(PowerStripLayout.EDGE, PowerStripLayout.stripX(5));
    }

    @Test
    void buttons_standOneOverTheOtherInsideTheStrip() {
        assertTrue(PowerStripLayout.RESTART_Y >= PowerStripLayout.POWER_Y + PowerStripLayout.BUTTON);
        assertTrue(PowerStripLayout.RESTART_Y + PowerStripLayout.BUTTON <= PowerStripLayout.H);
        assertTrue(PowerStripLayout.buttonX() >= PowerStripLayout.PAD);
    }
}
