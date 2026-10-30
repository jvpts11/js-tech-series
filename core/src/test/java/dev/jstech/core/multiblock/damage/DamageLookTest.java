/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DamageLookTest {

    private static final DamageStamp IMPACT = new DamageStamp(0.55, 0.35, DamageStamp.Push.PETALS_IN, 0.55, true,
            DamageStamp.Edge.TORN);
    private static final DamageStamp BURN = new DamageStamp(0.5, 0.25, DamageStamp.Push.SAG, 0.12, false,
            DamageStamp.Edge.SCORCHED);
    private static final DamageStamp DENT = new DamageStamp(0, 0, DamageStamp.Push.DENT, 0.25, false,
            DamageStamp.Edge.SCUFFED);
    /* Opaque colours, one for each part of an edge. */
    private static final DamageLook.Colours COLOURS = new DamageLook.Colours(0xFF000001, 0xFF000002, 0xFF000003,
            0xFF000004, 0xFF000005, 0xFF000006);

    @Test
    void side_isItsReachEitherWaySixteenToABlock() {
        final DamageLook look = look(new Strike(IMPACT, 0, 0, 0, 0, 0, 1, 1, 5));

        assertEquals((int) Math.ceil(2 * Strike.REACH * DamageLook.TEXELS), look.side());
    }

    @Test
    void pixels_tearTheMetalAlongTheRimOnly() {
        final DamageLook look = look(new Strike(IMPACT, 0, 0, 0, 0, 0, 1, 1, 5));
        final int[] pixels = look.pixels();
        final int side = look.side();

        assertEquals(0, alpha(pixels[side / 2 * side + side / 2]), "nothing is drawn in the hole");
        assertEquals(0, alpha(pixels[1 * side + 1]), "nothing is drawn far out");
        boolean torn = false;
        for (final int pixel : pixels) {
            torn |= alpha(pixel) == 0xFF;
        }
        assertTrue(torn, "the rim is torn bare");
    }

    @Test
    void glow_isOnlyABurnsAndAlongItsRim() {
        final DamageLook impact = look(new Strike(IMPACT, 0, 0, 0, 0, 0, 1, 1, 5));
        final DamageLook burn = look(new Strike(BURN, 0, 0, 0, 0, 0, 1, 1, 5));

        assertFalse(impact.glows());
        assertTrue(Arrays.stream(impact.glow()).allMatch(pixel -> pixel == 0));
        assertTrue(burn.glows());
        assertTrue(Arrays.stream(burn.glow()).anyMatch(pixel -> alpha(pixel) == 0xFF));
    }

    @Test
    void pixels_ofADentAreAFewScrapesNearWhereItStruck() {
        final DamageLook look = look(new Strike(DENT, 0, 0, 0, 0, 0, 1, 1, 5));
        final int[] pixels = look.pixels();

        final long scraped = Arrays.stream(pixels).filter(pixel -> alpha(pixel) != 0).count();
        assertTrue(scraped > 0 && scraped < pixels.length / 20, "a few scrapes, not " + scraped);
    }

    @Test
    void uv_putsTheStrikesPointInTheMiddleOfItsPicture() {
        final DamageLook look = look(new Strike(IMPACT, 1, 2, 3, 0, 1, 0, 0.5, 5));
        final double[] uv = new double[2];

        look.uv(1, 2, 3, uv);

        assertArrayEquals(new double[] {0.5, 0.5}, uv, 1.0e-9);
    }

    @Test
    void pixels_areTheSameForTheSameStrike() {
        final Strike strike = new Strike(BURN, 0, 0, 0, 1, 0, 0, 0.7, 42);

        assertArrayEquals(look(strike).pixels(), look(strike).pixels());
    }

    private static DamageLook look(final Strike strike) {
        return new DamageLook(strike, COLOURS);
    }

    private static int alpha(final int argb) {
        return argb >>> 24;
    }
}
