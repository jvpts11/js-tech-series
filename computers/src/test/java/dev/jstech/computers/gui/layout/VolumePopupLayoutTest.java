/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.layout.VolumePopupLayout.Geometry;
import dev.jstech.computers.gui.layout.VolumePopupLayout.Look;
import dev.jstech.computers.gui.layout.VolumePopupLayout.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VolumePopupLayoutTest {

    /** Wider than the game's font on average, so a word that fits here fits in the game. */
    private static final ToIntFunction<String> WIDTH = s -> s.length() * 6;
    private static final VolumePopupLayout.Labels ENGLISH = new VolumePopupLayout.Labels("Audio Volume", "Mute output",
            "Sound output", List.of("Monitor", "Speakers", "Monitor and speakers"), "Desk left, Desk right",
            "Configure Audio Devices...");
    /** The longest words the Portuguese gives these places. */
    private static final VolumePopupLayout.Labels LONG = new VolumePopupLayout.Labels("Volume de audio do sistema",
            "Silenciar a saida", "Saida de som", List.of("Monitor", "Alto-falantes", "Monitor e alto-falantes"),
            "Mesa esquerda, Mesa direita", "Configurar dispositivos de audio...");

    @Test
    void of_everyLookIsCleanOpenOrFoldedInEitherLanguage() {
        for (final Look look : Look.values()) {
            for (final boolean expanded : new boolean[] {true, false}) {
                for (final VolumePopupLayout.Labels labels : List.of(ENGLISH, LONG)) {
                    final Geometry g = VolumePopupLayout.of(look, expanded, labels, WIDTH);
                    assertTrue(g.toGuiLayout().isClean(), look + (expanded ? " open" : " folded") + " overlaps or"
                            + " spills: " + g.toGuiLayout().overlaps() + g.toGuiLayout().outOfBounds());
                }
            }
        }
    }

    @Test
    void of_growsToItsLongestWords() {
        for (final Look look : Look.values()) {
            final Geometry g = VolumePopupLayout.of(look, true, LONG, WIDTH);
            for (final Rect row : g.outputs()) {
                assertTrue(row.x() + 6 + WIDTH.applyAsInt("Mesa esquerda, Mesa direita") <= g.width(),
                        look + ": the speakers' names run past the control");
            }
            if (g.footer().present()) {
                assertTrue(g.footer().right() <= g.width(), look + ": the foot runs past the control");
            }
        }
    }

    @Test
    void of_foldsTheOutputsBehindTheArrowOnlyWhereTheLookHasOne() {
        assertTrue(VolumePopupLayout.of(Look.QUICK, false, ENGLISH, WIDTH).outputs().isEmpty());
        assertEquals(3, VolumePopupLayout.of(Look.QUICK, true, ENGLISH, WIDTH).outputs().size());
        assertEquals(3, VolumePopupLayout.of(Look.PLASMA, false, ENGLISH, WIDTH).outputs().size(),
                "Plasma lists them always");
        assertTrue(VolumePopupLayout.of(Look.CLASSIC, true, ENGLISH, WIDTH).outputs().isEmpty(),
                "the classic popup has no list of outputs");
    }

    @Test
    void volumeAt_readsTheSliderBothWaysAndAgreesWithTheThumb() {
        for (final Look look : Look.values()) {
            final Geometry g = VolumePopupLayout.of(look, true, ENGLISH, WIDTH);
            for (final int volume : new int[] {0, 25, 80, 100}) {
                final int at = VolumePopupLayout.thumbAt(g, volume);
                final int back = g.upright()
                        ? VolumePopupLayout.volumeAt(g, g.track().x(), at)
                        : VolumePopupLayout.volumeAt(g, at, g.track().y());
                assertTrue(Math.abs(back - volume) <= 3, look + ": the thumb at " + volume + " reads back " + back);
            }
        }
    }

    @Test
    void volumeAt_holdsPastTheEndsOfTheSlider() {
        final Geometry upright = VolumePopupLayout.of(Look.CLASSIC, false, ENGLISH, WIDTH);
        assertEquals(100, VolumePopupLayout.volumeAt(upright, 0, -50), "above the top is full");
        assertEquals(0, VolumePopupLayout.volumeAt(upright, 0, 500), "below the bottom is silent");
        final Geometry across = VolumePopupLayout.of(Look.QUICK, false, ENGLISH, WIDTH);
        assertEquals(0, VolumePopupLayout.volumeAt(across, -20, 0));
        assertEquals(100, VolumePopupLayout.volumeAt(across, 900, 0));
    }
}
