/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class RecipeChooserLayoutTest {

    private static final int INVENTORY_RESULT_X = 154;

    @Test
    void layout_keepsTheChooserApartFromTheResultInBothWindows() {
        for (final int resultX : new int[] {RecipeChooserLayout.CRAFTING_RESULT_X,
                RecipeChooserLayout.FURNACE_RESULT_X}) {
            final GuiLayout layout = RecipeChooserLayout.layout(resultX, RecipeChooserLayout.RESULT_Y);

            assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
        }
    }

    @Test
    void chooser_standsWhereTheMockPutsIt() {
        assertEquals(151, RecipeChooserLayout.x(RecipeChooserLayout.CRAFTING_RESULT_X));
        assertEquals(143, RecipeChooserLayout.x(RecipeChooserLayout.FURNACE_RESULT_X));
        assertEquals(31, RecipeChooserLayout.y(RecipeChooserLayout.RESULT_Y));
    }

    @Test
    void fits_leavesOutTheInventorysSmallGrid() {
        assertTrue(RecipeChooserLayout.fits(RecipeChooserLayout.CRAFTING_RESULT_X, RecipeChooserLayout.WINDOW_WIDTH));
        assertFalse(RecipeChooserLayout.fits(INVENTORY_RESULT_X, RecipeChooserLayout.WINDOW_WIDTH));
    }
}
