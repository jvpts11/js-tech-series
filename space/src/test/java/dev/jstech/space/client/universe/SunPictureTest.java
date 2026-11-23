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
 * The Sun every sky and the space draw, the series' own in place of the game's square: a round disc holding as much
 * light as the game's square, which is a quarter of its picture wide, with a corona round it no brighter than the
 * game's halo, fading out before the picture's edge.
 */
class SunPictureTest {

    private static final String PICTURE = "/assets/minecraft/textures/environment/sun.png";
    private static final int OPAQUE = 255;
    /* How far the disc's edge may stand from a true circle, in pixels: a circle laid on a grid steps. */
    private static final double STEP = 1.0;
    /*
     * The game's square, a quarter of its picture wide, as a share of the picture's width; and the most its halo
     * adds, of 255.
     */
    private static final double GAMES_SQUARE = 0.25;
    private static final int GAMES_HALO = 40;

    @Test
    void sun_isARoundDiscAsBrightAsTheGamesSquare() {
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
        // A disc of the same area as the game's square: its radius is the square's side over the root of pi.
        final double square = sun.getWidth() * GAMES_SQUARE;
        assertEquals(square / Math.sqrt(Math.PI), nearestClear, 1.0, "its disc holds the light of the game's square");
    }

    @Test
    void sun_leavesTheCornersOfTheGamesSquareToItsCorona() {
        final BufferedImage sun = picture();
        final int first = (int) Math.round(sun.getWidth() * (1 - GAMES_SQUARE) / 2);
        final int last = sun.getWidth() - 1 - first;
        for (final int[] corner : new int[][] {{first, first}, {last, first}, {first, last}, {last, last}}) {
            assertTrue(alpha(sun, corner[0], corner[1]) < OPAQUE, "the game's square's corner at " + corner[0] + ", "
                    + corner[1] + " is only corona");
        }
    }

    @Test
    void sun_coronaIsNoBrighterThanTheGamesHaloAndFadesBeforeTheEdge() {
        final BufferedImage sun = picture();
        final int edge = sun.getWidth() - 1;
        for (int y = 0; y < sun.getHeight(); y++) {
            for (int x = 0; x < sun.getWidth(); x++) {
                final int alpha = alpha(sun, x, y);
                assertTrue(alpha == OPAQUE || alpha <= GAMES_HALO, "the corona at " + x + ", " + y + " adds " + alpha
                        + ", the game's halo no more than " + GAMES_HALO);
                if (x == 0 || y == 0 || x == edge || y == edge) {
                    assertEquals(0, alpha, "the corona is gone by the picture's edge, at " + x + ", " + y);
                }
            }
        }
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
