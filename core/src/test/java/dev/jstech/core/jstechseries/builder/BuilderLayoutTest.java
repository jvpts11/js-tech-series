/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import org.junit.jupiter.api.Test;

class BuilderLayoutTest {

    @Test
    void layout_hasNothingOverlappingNorOutside() {
        final GuiLayout layout = BuilderLayout.layout();
        assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
    }

    @Test
    void pages_endAboveTheTabsAndInsideTheDisplay() {
        assertTrue(BuilderLayout.READY_Y <= BuilderLayout.pageFoot(), "the ready line stands above the tabs");
        assertTrue(BuilderLayout.TO_PLACE_Y + BuilderLayout.GROUP_ROWS * BuilderLayout.GROUP_PITCH
                <= BuilderLayout.pageFoot() + BuilderLayout.LINE, "every group of choices fits the page");
        assertTrue(BuilderLayout.TO_PLACE_Y + BuilderLayout.SOURCE_ROWS * BuilderLayout.SOURCE_PITCH
                <= BuilderLayout.pageFoot(), "every source fits the page");
        assertTrue(BuilderLayout.CHOICE_WIDTH * BuilderLayout.CHOICE_OPTIONS
                <= ProbeLayout.TEXT_RIGHT - ProbeLayout.TEXT_LEFT, "the blocks of a choice fit side by side");
    }
}
