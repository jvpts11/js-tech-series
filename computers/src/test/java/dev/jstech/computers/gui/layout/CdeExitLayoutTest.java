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

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class CdeExitLayoutTest {

    /** The desktop as wide as the glass itself, and as it is when it is drawn smaller. */
    private static final int[][] DESKTOPS = {{384, 256}, {512, 341}, {640, 427}};

    @Test
    void layout_isCleanOnEveryDesktopSize() {
        for (final int[] size : DESKTOPS) {
            final GuiLayout l = CdeExitLayout.layout(size[0], size[1]);
            assertTrue(l.overlaps().isEmpty(), size[0] + " wide: " + l.overlaps());
            assertTrue(l.outOfBounds().isEmpty(), size[0] + " wide: " + l.outOfBounds());
        }
    }

    @Test
    void dialog_standsInTheMiddleOfTheDesktop() {
        final Rect d = CdeExitLayout.dialog(512, 341);
        assertEquals(512 - (d.x() + d.w()), d.x(), 1);
        assertEquals(341 - (d.y() + d.h()), d.y(), 1);
    }

    @Test
    void linesAndButtons_standInsideTheDialogWithTheButtonsBelow() {
        final Rect d = CdeExitLayout.dialog(512, 341);
        final int lastLineBottom = CdeExitLayout.lineY(CdeExitLayout.LINES - 1, 512, 341) + CdeExitLayout.LINE_H;
        assertTrue(CdeExitLayout.lineY(0, 512, 341) >= d.y() + CdeExitLayout.TITLE_H, "the words clear the title");
        for (int i = 0; i < CdeExitLayout.BUTTONS; i++) {
            final Rect b = CdeExitLayout.button(i, 512, 341);
            assertTrue(b.y() >= lastLineBottom, "button " + i + " stands under the words");
            assertTrue(b.x() >= d.x() && b.x() + b.w() <= d.x() + d.w() && b.y() + b.h() <= d.y() + d.h(),
                    "button " + i + " stands inside the dialog");
        }
    }

    @Test
    void lineY_setsTheWarningApartFromTheLinesAboveIt() {
        final int pitch = CdeExitLayout.lineY(1, 512, 341) - CdeExitLayout.lineY(0, 512, 341);
        final int toWarning = CdeExitLayout.lineY(2, 512, 341) - CdeExitLayout.lineY(1, 512, 341);
        assertTrue(toWarning > pitch);
    }

    @Test
    void buttons_runShutDownRestartCancelFromTheLeft() {
        final Rect shutDown = CdeExitLayout.button(CdeExitLayout.SHUT_DOWN, 512, 341);
        final Rect restart = CdeExitLayout.button(CdeExitLayout.RESTART, 512, 341);
        final Rect cancel = CdeExitLayout.button(CdeExitLayout.CANCEL, 512, 341);
        assertTrue(shutDown.x() < restart.x() && restart.x() < cancel.x());
        assertEquals(shutDown.y(), cancel.y());
    }

    @Test
    void buttonAt_findsEachButtonAtItsMiddleAndNoneBetweenThem() {
        for (int i = 0; i < CdeExitLayout.BUTTONS; i++) {
            final Rect b = CdeExitLayout.button(i, 512, 341);
            assertEquals(i, CdeExitLayout.buttonAt(b.x() + b.w() / 2.0, b.y() + b.h() / 2.0, 512, 341));
        }
        final Rect first = CdeExitLayout.button(0, 512, 341);
        assertEquals(-1, CdeExitLayout.buttonAt(first.x() + first.w() + 2, first.y() + 2, 512, 341));
        assertEquals(-1, CdeExitLayout.buttonAt(first.x() + 2, first.y() - 3, 512, 341));
    }
}
