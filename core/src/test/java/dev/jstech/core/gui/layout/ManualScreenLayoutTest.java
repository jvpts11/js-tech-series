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

import org.junit.jupiter.api.Test;

class ManualScreenLayoutTest {

    @Test
    void of_givesTheBinderOfTheApprovedSizeForTwoPages() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);

        assertEquals(364, binder.width());
        assertEquals(218, binder.height());
        assertEquals(181, binder.right().x());
    }

    @Test
    void layout_isCleanForTwoPagesAndForOne() {
        for (final boolean spread : new boolean[] {true, false}) {
            final GuiLayout layout = ManualScreenLayout.layout(ManualScreenLayout.of(166, 201, spread), 10);

            assertTrue(layout.isClean(), () -> "spread " + spread + ": " + layout.overlaps() + " "
                    + layout.outOfBounds());
        }
    }

    @Test
    void of_fitsTheTabsAboveTheArrow() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);

        final ManualScreenLayout.Rect last = ManualScreenLayout.tab(binder, binder.tabs() - 1);
        assertTrue(last.y() + last.height() < binder.next().y(), () -> "the last tab ends at "
                + (last.y() + last.height()));
        assertEquals(6, binder.tabs());
    }

    @Test
    void of_fitsTheBinderOnAFullHdScreenAtTheLargestGuiScale() {
        final ManualScreenLayout.Geometry binder = ManualScreenLayout.of(166, 201, true);

        assertTrue(binder.width() <= 480 && binder.height() <= 270, () -> binder.width() + " by " + binder.height());
    }
}
