/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class CommandPromptLayoutTest {

    private static final int FULL_W = 384;
    private static final int FULL_H = 256;
    /** The smallest a window still leaves the console panel and the input strip room to draw at, one the
     *  glass actually reaches: a monitor's own {@code width(320)}/{@code height(240)}. */
    private static final int COMPACT_W = MonitorGlass.width(320);
    private static final int COMPACT_H = MonitorGlass.height(240);

    @Test
    void layout_isCleanAtFullSize() {
        final GuiLayout l = CommandPromptLayout.layout(FULL_W, FULL_H, false);
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void layout_isCleanAtTheSmallestWindowedSize() {
        assertTrue(CommandPromptLayout.layout(COMPACT_W, COMPACT_H, false).isClean());
    }

    @Test
    void layout_isEmptyForABareTerminal() {
        assertTrue(CommandPromptLayout.layout(FULL_W, FULL_H, true).elements().isEmpty());
    }

    @Test
    void glassTop_isHigherForABareTerminal() {
        assertTrue(CommandPromptLayout.glassTop(true) < CommandPromptLayout.glassTop(false));
    }

    @Test
    void consoleAndInputStrip_endBeforeThePanelBottom() {
        assertTrue(CommandPromptLayout.consoleBottom(FULL_H) < CommandPromptLayout.inputStripTop(FULL_H));
        assertTrue(CommandPromptLayout.inputStripBottom(FULL_H) < FULL_H);
    }
}
