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

class BootSequenceLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = BootSequenceLayout.layout();
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void noBootLayout_isCleanWithTheMostListedDevices() {
        final GuiLayout l = BootSequenceLayout.noBootLayout(BootSequenceLayout.MOST_LISTED_DEVICES);
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void noBootLayout_isCleanWithNoDevicesAtAll() {
        assertTrue(BootSequenceLayout.noBootLayout(0).isClean());
    }

    @Test
    void noBootDialog_staysOnTheGlassAtItsTallest() {
        final int boxH = BootSequenceLayout.noBootBoxH(BootSequenceLayout.MOST_LISTED_DEVICES);
        final int by = BootSequenceLayout.noBootBoxY(boxH);
        assertTrue(by >= 0);
        assertTrue(by + boxH <= BootSequenceLayout.HEIGHT);
    }

    @Test
    void bar_standsBelowTheCentreOfTheGlass() {
        assertTrue(BootSequenceLayout.barY() > BootSequenceLayout.HEIGHT / 2);
    }
}
