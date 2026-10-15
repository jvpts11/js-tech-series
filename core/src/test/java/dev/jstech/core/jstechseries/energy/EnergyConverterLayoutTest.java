/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class EnergyConverterLayoutTest {

    @Test
    void layout_keepsEveryPieceApartAndInside() {
        for (final boolean fixed : new boolean[] {false, true}) {
            final GuiLayout layout = EnergyConverterLayout.layout(fixed);

            assertTrue(layout.isClean(), () -> "overlaps " + layout.overlaps() + ", outside " + layout.outOfBounds());
        }
    }

    @Test
    void picture_holdsTheScreenAndTheSpritesBelowIt() {
        assertTrue(EnergyConverterLayout.WIDTH <= EnergyConverterLayout.PICTURE_WIDTH);
        assertTrue(EnergyConverterLayout.HEIGHT <= EnergyConverterLayout.SPRITES_Y);
        assertTrue(EnergyConverterLayout.SPRITES_Y + EnergyConverterLayout.SWITCH_HEIGHT
                <= EnergyConverterLayout.PICTURE_HEIGHT);
        assertTrue(EnergyConverterLayout.SWITCH_SPRITE_X + EnergyConverterLayout.SWITCH_SPRITE_PITCH
                + EnergyConverterLayout.SWITCH_WIDTH <= EnergyConverterLayout.PICTURE_WIDTH);
    }

    @Test
    void plates_stopShortOfTheDisplay() {
        assertTrue(EnergyConverterLayout.PLATE_RIGHT < EnergyConverterLayout.DISPLAY_X - 3);
    }

    @Test
    void keyAt_findsEachClassKeyAndNothingBetween() {
        for (int k = 0; k < EnergyConverterLayout.KEYS; k++) {
            assertEquals(k, EnergyConverterLayout.keyAt(EnergyConverterLayout.KEY_X + k * EnergyConverterLayout.KEY_PITCH
                    + 2, EnergyConverterLayout.KEY_Y + 2));
        }
        assertEquals(-1, EnergyConverterLayout.keyAt(EnergyConverterLayout.KEY_X + EnergyConverterLayout.KEY_WIDTH,
                EnergyConverterLayout.KEY_Y + 2));
        assertEquals(-1, EnergyConverterLayout.keyAt(EnergyConverterLayout.KEY_X + 2, EnergyConverterLayout.KEY_Y - 1));
    }

    @Test
    void drumAt_findsEachDrumAndItsHalves() {
        for (int d = 0; d < EnergyConverterLayout.DRUMS; d++) {
            assertEquals(d, EnergyConverterLayout.drumAt(EnergyConverterLayout.DRUM_X + d
                    * EnergyConverterLayout.DRUM_PITCH + 1, EnergyConverterLayout.DRUM_Y + 1));
        }
        assertEquals(-1, EnergyConverterLayout.drumAt(EnergyConverterLayout.DRUM_X - 1,
                EnergyConverterLayout.DRUM_Y + 1));
        assertTrue(EnergyConverterLayout.upperHalf(EnergyConverterLayout.DRUM_Y + 1));
        assertFalse(EnergyConverterLayout.upperHalf(EnergyConverterLayout.DRUM_Y + EnergyConverterLayout.DRUM_HEIGHT
                - 1));
    }

    @Test
    void switchKnobAndWays_answerTheirClicks() {
        assertTrue(EnergyConverterLayout.onSwitch(EnergyConverterLayout.SWITCH_X + 5,
                EnergyConverterLayout.SWITCH_Y + 5));
        assertFalse(EnergyConverterLayout.onSwitch(EnergyConverterLayout.SWITCH_X - 1,
                EnergyConverterLayout.SWITCH_Y + 5));
        assertTrue(EnergyConverterLayout.onKnob(EnergyConverterLayout.KNOB_X + 3, EnergyConverterLayout.KNOB_Y - 3));
        assertFalse(EnergyConverterLayout.onKnob(EnergyConverterLayout.KNOB_X + 20, EnergyConverterLayout.KNOB_Y));
        assertTrue(EnergyConverterLayout.onToFeWord(EnergyConverterLayout.TO_FE_WORD_X + 4,
                EnergyConverterLayout.WAY_WORD_Y + 2));
        assertTrue(EnergyConverterLayout.onToJeWord(EnergyConverterLayout.TO_JE_WORD_X + 4,
                EnergyConverterLayout.WAY_WORD_Y + 2));
        assertFalse(EnergyConverterLayout.onToJeWord(EnergyConverterLayout.TO_FE_WORD_X + 4,
                EnergyConverterLayout.WAY_WORD_Y + 2));
    }
}
