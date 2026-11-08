/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.os.defrag.DefragLook;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefragLayoutTest {

    @Test
    void layout_everyFramesWindowIsClean() {
        for (final DefragLook look : DefragLook.values()) {
            if (!look.window()) {
                continue;
            }
            final GuiLayout layout = DefragLayout.layout(look);
            assertTrue(layout.isClean(), look + ": " + layout.overlaps() + " " + layout.outOfBounds());
        }
    }
}
