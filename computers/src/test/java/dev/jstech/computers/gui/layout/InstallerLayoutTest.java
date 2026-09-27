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

class InstallerLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout l = InstallerLayout.layout();
        assertTrue(l.overlaps().isEmpty(), "overlaps: " + l.overlaps());
        assertTrue(l.outOfBounds().isEmpty(), "out of bounds: " + l.outOfBounds());
    }

    @Test
    void eraseDialog_isCentredOnTheGlass() {
        assertTrue(InstallerLayout.eraseDialogX() > 0);
        assertTrue(InstallerLayout.eraseDialogX() + InstallerLayout.ERASE_DIALOG_W < InstallerLayout.WIDTH);
        assertTrue(InstallerLayout.eraseDialogY() + InstallerLayout.ERASE_DIALOG_H < InstallerLayout.HEIGHT);
    }
}
