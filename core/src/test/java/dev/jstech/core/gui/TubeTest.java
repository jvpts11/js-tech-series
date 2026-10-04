/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TubeTest {

    @Test
    void apply_leavesAColourMonitorsPictureAlone() {
        for (final int colour : new int[] {0xFFEF6A5A, 0x8039D6C4, 0xFF000000, 0xFFFFFFFF}) {
            assertEquals(colour, Tube.COLOUR.apply(colour));
        }
    }

    @Test
    void apply_lightsWhiteAtTheFullGlowOfEachPhosphor() {
        for (final Tube tube : new Tube[] {Tube.WHITE, Tube.GREEN, Tube.AMBER}) {
            assertEquals(tube.glow(), tube.apply(0xFFFFFFFF), tube + " shows white as its own full glow");
            assertEquals(0xFF000000, tube.apply(0xFF000000), tube + " leaves black unlit");
        }
    }

    @Test
    void apply_turnsEveryColourIntoTheAmberPhosphor() {
        // The amber is red over green over blue at every brightness, whatever colour went in.
        for (final int colour : new int[] {0xFF39D6C4, 0xFF2AA7E0, 0xFFEF6A5A, 0xFF5FE07A}) {
            final int lit = Tube.AMBER.apply(colour);
            final int r = (lit >> 16) & 0xFF;
            final int g = (lit >> 8) & 0xFF;
            final int b = lit & 0xFF;
            assertTrue(r >= g && g >= b, "amber for " + Integer.toHexString(colour));
        }
    }

    @Test
    void apply_keepsAlphaOnEveryTube() {
        for (final Tube tube : Tube.values()) {
            assertEquals(0x80, tube.apply(0x80FFFFFF) >>> 24, tube + " keeps the alpha");
        }
    }

    @Test
    void apply_putsEveryColourOnOneOfTheSixteen() {
        final Set<Integer> sixteen = new HashSet<>();
        for (final int colour : Tube.sixteenColours()) {
            sixteen.add(colour);
        }
        assertEquals(16, sixteen.size(), "sixteen different colours");
        for (int i = 0; i < 4096; i++) {
            final int colour = 0xFF000000 | (i * 4099) & 0xFFFFFF;
            assertTrue(sixteen.contains(Tube.SIXTEEN.apply(colour) & 0xFFFFFF),
                    Integer.toHexString(colour) + " lands on one of the sixteen");
        }
    }

    @Test
    void apply_takesTheNearestOfTheSixteen() {
        assertEquals(0xFF0000AA, Tube.SIXTEEN.apply(0xFF0000A0), "a dark blue is the adapter's blue");
        assertEquals(0xFFAA5500, Tube.SIXTEEN.apply(0xFFA05008), "a brown is the brown");
        assertEquals(0xFFFFFFFF, Tube.SIXTEEN.apply(0xFFF8F8F8), "near white is white");
        assertEquals(0xFF555555, Tube.SIXTEEN.apply(0xFF505050), "a dark grey is the dark grey");
    }

    @Test
    void monochrome_isTrueForTheThreePhosphorsOnly() {
        assertTrue(Tube.WHITE.monochrome());
        assertTrue(Tube.GREEN.monochrome());
        assertTrue(Tube.AMBER.monochrome());
        assertFalse(Tube.SIXTEEN.monochrome());
        assertFalse(Tube.COLOUR.monochrome());
        assertFalse(Tube.COLOUR.filters());
        assertTrue(Tube.SIXTEEN.filters());
    }
}
