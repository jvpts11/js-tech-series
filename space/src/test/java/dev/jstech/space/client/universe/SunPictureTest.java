/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * The Sun every sky and the space draw, the series' own in place of the game's square: a round disc, half the
 * picture wide in its middle as the game's square is, with a corona round it that fades out before the picture's
 * edge.
 */
class SunPictureTest {

    private static final String PICTURE = "/assets/minecraft/textures/environment/sun.png";
    private static final int OPAQUE = 255;
    /* How far the disc's edge may stand from a true circle, in pixels: a circle laid on a grid steps. */
    private static final double STEP = 1.0;

    @Test
    void sun_isARoundDiscHalfThePictureWide() {
        final BufferedImage sun = picture();
        final double middle = (sun.getWidth() - 1) / 2.0;
        double nearestClear = Double.MAX_VALUE;
        double farthestSolid = 0;
        for (int y = 0; y < sun.getHeight(); y++) {
            for (int x = 0; x < sun.getWidth(); x++) {
                final double r = Math.hypot(x - middle, y - middle);
                if (alpha(sun, x, y) == OPAQUE) {
                    farthestSolid = Math.max(farthestSolid, r);
                } else {
                    nearestClear = Math.min(nearestClear, r);
                }
            }
        }
        assertTrue(farthestSolid - nearestClear <= STEP, "its solid disc is round: solid out to " + farthestSolid
                + ", clear from " + nearestClear);
        assertEquals(sun.getWidth() / 4.0, nearestClear, 1.5, "its disc is half the picture wide");
    }

    @Test
    void sun_leavesTheCornersOfTheGamesSquareToItsCorona() {
        final BufferedImage sun = picture();
        final int quarter = sun.getWidth() / 4;
        final int last = sun.getWidth() - 1;
        for (final int[] corner : new int[][] {{quarter, quarter}, {last - quarter, quarter}, {quarter, last - quarter},
                {last - quarter, last - quarter}}) {
            assertTrue(alpha(sun, corner[0], corner[1]) < OPAQUE, "the game's square's corner at " + corner[0] + ", "
                    + corner[1] + " is only corona");
        }
        assertEquals(0, alpha(sun, 0, 0), "the corona fades out before the picture's corner");
    }

    private static int alpha(final BufferedImage image, final int x, final int y) {
        return image.getRGB(x, y) >>> 24;
    }

    private static BufferedImage picture() {
        try (InputStream in = SunPictureTest.class.getResourceAsStream(PICTURE)) {
            assertNotNull(in, "the Sun's picture is there");
            return ImageIO.read(in);
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }
}
