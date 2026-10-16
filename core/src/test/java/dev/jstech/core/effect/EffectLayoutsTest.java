/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import org.junit.jupiter.api.Test;

class EffectLayoutsTest {

    @Test
    void editor_isCleanForEveryCountOfEmittersOnScreensLargeAndSmall() {
        for (int emitters = 1; emitters <= 8; emitters++) {
            for (final int[] screen : new int[][] {{640, 360}, {480, 320}, {427, 240}}) {
                final GuiLayout layout = EffectEditorLayout.of(emitters, screen[0], screen[1]).layout();
                assertTrue(layout.isClean(), emitters + " emitters on " + screen[0] + "x" + screen[1] + ": "
                        + layout.overlaps() + " " + layout.outOfBounds());
            }
        }
    }

    @Test
    void editor_keepsTheDesignsPlacesWhereItFits() {
        final EffectEditorLayout card = EffectEditorLayout.of(3, 640, 360);

        assertEquals(444, card.x());
        assertEquals(EffectEditorLayout.PITCH, card.pitch());
        assertEquals(3, card.shown());
        assertEquals(card.y() + 110, card.heading());
        assertEquals(card.y() + 122, card.slider(0));
        assertEquals(card.y() + 214, card.movedBy());
        assertEquals(card.y() + 300, card.buttons());
        assertEquals(332, card.height());
    }

    @Test
    void editor_drawsRowsCloserAndShowsFewerOnAShortScreen() {
        final EffectEditorLayout card = EffectEditorLayout.of(8, 427, 240);

        assertTrue(card.pitch() < EffectEditorLayout.PITCH, "closer rows: " + card.pitch());
        assertTrue(card.shown() >= 1 && card.shown() < 8, "fewer shown: " + card.shown());
        assertTrue(card.y() + card.height() <= 240 || card.pitch() == EffectEditorLayout.LEAST_PITCH);
    }

    @Test
    void newEffect_isClean() {
        final GuiLayout layout = NewEffectLayout.layout();

        assertTrue(layout.isClean(), layout.overlaps() + " " + layout.outOfBounds());
        assertEquals(444, NewEffectLayout.x(640));
    }
}
