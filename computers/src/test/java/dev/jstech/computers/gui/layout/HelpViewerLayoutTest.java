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

class HelpViewerLayoutTest {

    @Test
    void layout_hasNothingOverlappingAndNothingOutsideTheWindow() {
        final GuiLayout layout = HelpViewerLayout.layout();

        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void theListAndThePageDoNotMeet() {
        assertTrue(HelpViewerLayout.DOC_X >= HelpViewerLayout.PAD + HelpViewerLayout.LIST_W + HelpViewerLayout.PAD,
                "the page starts past the list and its gap");
        assertTrue(HelpViewerLayout.DOC_X + HelpViewerLayout.PAD < HelpViewerLayout.W,
                "and the page is inside the window");
    }

    @Test
    void theButtonsFitAcrossTheTop() {
        final int wide = HelpViewerLayout.PAD
                + HelpViewerLayout.BUTTONS.size() * (HelpViewerLayout.BUTTON_W + HelpViewerLayout.PAD);

        assertTrue(wide <= HelpViewerLayout.W, "the row of buttons is " + wide + " wide");
    }
}
