/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NewScreenLayoutTest {

    @Test
    void layout_isClean() {
        final GuiLayout layout = NewScreenLayout.layout();
        assertTrue(layout.isClean(), "overlaps " + layout.overlaps() + ", out of bounds " + layout.outOfBounds());
    }

    @Test
    void preview_standsBesideThePanel() {
        assertTrue(NewScreenLayout.previewX() >= NewScreenLayout.PANEL_X + NewScreenLayout.WIDTH);
    }
}
