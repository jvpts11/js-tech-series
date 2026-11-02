/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.sky;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GalaxyPictureTest {

    private static final int WIDTH = 720;
    private static final int HEIGHT = 96;
    private static final int RIM = 0xC8D0FF;
    private static final int HEART = 0xFFEDD1;

    @Test
    void light_isBrightestAlongTheMiddleOfTheBandAtItsHeart() {
        final double heart = GalaxyPicture.light(0, 8, 0.7, 0.7);
        assertTrue(heart > GalaxyPicture.light(120, 8, 0.7, 0.7), "brighter at its heart than along its arms");
        assertTrue(heart > GalaxyPicture.light(0, 20, 0.7, 0.7), "brighter in the band than off it");
        assertTrue(GalaxyPicture.light(0, 8, 0.9, 0.7) > GalaxyPicture.light(0, 8, 0.2, 0.7),
                "brighter in a cloud of stars than between them");
    }

    @Test
    void light_isDarkenedByTheRiftAlongTheInnerBandOnly() {
        // The rift runs a little north of the middle line where the coarse noise is even.
        final double rift = GalaxyPicture.light(30, 1.2, 0.5, 0.0);
        final double beside = GalaxyPicture.light(30, -4.0, 0.5, 0.0);
        assertTrue(rift < beside * 0.5, "the rift is dark against the band beside it: " + rift + " against " + beside);
        assertTrue(GalaxyPicture.light(150, 1.2, 0.5, 0.0) > GalaxyPicture.light(150, -4.0, 0.5, 0.0) * 0.5,
                "no rift along the outer band");
    }

    @Test
    void draw_isBlackAwayFromTheBandAndWarmAtItsHeart() {
        final int[] picture = GalaxyPicture.draw(7L, WIDTH, HEIGHT, RIM, HEART);
        assertEquals(WIDTH * HEIGHT, picture.length);
        assertEquals(0, picture[0], "nothing far north of the band's end");
        int warmest = 0;
        for (int y = 0; y < HEIGHT; y++) {
            final int c = picture[y * WIDTH + WIDTH / 2];
            warmest = Math.max(warmest, (c >> 16 & 0xFF) - (c & 0xFF));
        }
        assertTrue(warmest > 0, "its heart is warmer, redder than blue");
        int coolest = Integer.MAX_VALUE;
        for (int y = 0; y < HEIGHT; y++) {
            final int c = picture[y * WIDTH + WIDTH / 20];
            if (c != 0) {
                coolest = Math.min(coolest, (c >> 16 & 0xFF) - (c & 0xFF));
            }
        }
        assertTrue(coolest <= 0, "its ends are cool, bluer than red");
    }

    @Test
    void draw_isTheSameForTheSameSeed() {
        assertArrayEquals(GalaxyPicture.draw(7L, WIDTH, HEIGHT, RIM, HEART),
                GalaxyPicture.draw(7L, WIDTH, HEIGHT, RIM, HEART));
    }

    @Test
    void draw_comesInFewLevels() {
        final int[] picture = GalaxyPicture.draw(7L, WIDTH, HEIGHT, 0xFFFFFF, 0xFFFFFF);
        final Set<Integer> greys = new HashSet<>();
        for (final int c : picture) {
            greys.add(c & 0xFF);
        }
        assertTrue(greys.size() < 40, "its light in a dozen levels and the nebulae's few: " + greys.size());
    }
}
