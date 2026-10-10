/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SplashLogosTest {

    /*
     * Every lockup, mark and emblem a screen draws centred on a point. The drawing centres the picture, so whatever
     * the picture shows has to stand in its middle: the lockups were once drawn flush left in their pictures, and
     * every self-test and start showed its maker's mark well left of the middle of the glass.
     */
    private static final List<String> CENTRED = List.of(
            "splash/jsc_logo.png", "splash/frames_95_logo.png", "splash/frames_xp_logo.png",
            "splash/frames_95_mark.png", "splash/frames_xp_mark.png", "splash/frames_7_mark.png",
            "splash/frames_10_mark.png", "splash/frames_11_mark.png", "splash/cinnamon_mark.png",
            "splash/kde_plasma_mark.png", "splash/freebsd_orb.png",
            "emblem/frames_95.png", "emblem/frames_xp.png", "emblem/frames_7.png", "emblem/frames_7_orb.png",
            "emblem/frames_10.png", "emblem/frames_10_start.png", "emblem/frames_11.png");
    /** The opacity above which a pixel is part of what the picture shows rather than its soft edge. */
    private static final int INK = 24;

    @Test
    void draw_hasTheInkOfEveryLockupAndMarkInTheMiddleOfItsPicture() throws IOException {
        for (final String name : CENTRED) {
            final BufferedImage picture = read(name);
            final int width = picture.getWidth();
            int from = width;
            int to = -1;
            for (int y = 0; y < picture.getHeight(); y++) {
                for (int x = 0; x < width; x++) {
                    if ((picture.getRGB(x, y) >>> 24) > INK) {
                        from = Math.min(from, x);
                        to = Math.max(to, x);
                    }
                }
            }
            final int left = from;
            final int right = width - 1 - to;
            // A few pixels either way is a shadow or a lean; a lockup flush to one side is hundreds.
            final int slack = Math.max(2, width / 32);
            assertTrue(to >= from, name + " shows something");
            assertTrue(Math.abs(left - right) <= slack,
                    () -> name + " has " + left + " clear pixels to the left of what it shows and " + right
                            + " to the right");
        }
    }

    private static BufferedImage read(final String name) throws IOException {
        try (InputStream in = SplashLogosTest.class.getResourceAsStream("/assets/jsc/textures/gui/" + name)) {
            assertNotNull(in, name + " is among the mod's pictures");
            return ImageIO.read(in);
        }
    }
}
