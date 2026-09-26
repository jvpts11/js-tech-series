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

class SpeakerLayoutTest {

    @Test
    void layout_nothingOverlapsOrLeavesTheScreen() {
        final GuiLayout l = SpeakerLayout.layout();
        assertTrue(l.overlaps().isEmpty(), () -> "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), () -> "out of bounds: " + l.outOfBounds());
        assertTrue(l.isClean());
    }
}
