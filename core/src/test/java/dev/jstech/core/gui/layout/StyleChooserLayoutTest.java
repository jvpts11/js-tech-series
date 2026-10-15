/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StyleChooserLayoutTest {

    @Test
    void layout_hasNoOverlapAndStaysInside() {
        final GuiLayout layout = StyleChooserLayout.layout();

        assertEquals(List.of(), layout.overlaps());
        assertEquals(List.of(), layout.outOfBounds());
    }

    @Test
    void rows_showSeveralStylesAtOnce() {
        assertTrue(StyleChooserLayout.rows() >= 10);
    }

    @Test
    void preview_hasRoomForASlotAButtonAndAText() {
        assertTrue(StyleChooserLayout.previewWidth() >= 90);
    }
}
