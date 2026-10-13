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

class OsInstallLayoutTest {

    @Test
    void workingLayout_isClean() {
        assertTrue(OsInstallLayout.workingLayout().isClean());
    }

    @Test
    void doneLayout_isClean() {
        assertTrue(OsInstallLayout.doneLayout().isClean());
    }

    @Test
    void failedLayout_isCleanWithTheMostWrappedLinesThatFit() {
        final GuiLayout l = OsInstallLayout.failedLayout(OsInstallLayout.mostFailureLines());
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void buttonRow_sitsUnderTheStepsAndTheProgressBar() {
        final int barBottom = OsInstallLayout.CONTENT_TOP
                + OsInstallLayout.STEP_COUNT * OsInstallLayout.STEP_ROW_H + OsInstallLayout.AFTER_STEPS_GAP
                + OsInstallLayout.BAR_H;
        assertTrue(barBottom < OsInstallLayout.buttonY());
    }
}
