/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.fs.PixImage;
import org.junit.jupiter.api.Test;

class PrintedPictureTest {

    /** A colour of the picture format's palette that is dark. */
    private static final int DARK = 1;

    @Test
    void print_everyInkPutsInkOnADarkPicture() {
        final PixImage picture = filled(16, 12, DARK);
        for (final PrinterModel.Ink ink : PrinterModel.Ink.values()) {
            assertTrue(PrintedPicture.print(picture, ink).inked() > 0, ink + " left the page blank");
        }
    }

    @Test
    void print_leavesAClearPictureAsPaper() {
        final PixImage clear = new PixImage(16, 12);
        for (final PrinterModel.Ink ink : PrinterModel.Ink.values()) {
            assertEquals(0, PrintedPicture.print(clear, ink).inked(), ink + " inked a clear picture");
        }
    }

    @Test
    void print_comesOutTheSameEveryTime() {
        final PixImage picture = filled(20, 10, DARK);
        picture.set(3, 3, 0);
        for (final PrinterModel.Ink ink : PrinterModel.Ink.values()) {
            assertArrayEquals(PrintedPicture.print(picture, ink).pixels(), PrintedPicture.print(picture, ink).pixels());
        }
    }

    @Test
    void print_fitsTheWidestSideAndKeepsTheShape() {
        final PrintedPicture.Raster clean = PrintedPicture.print(new PixImage(64, 32), PrinterModel.Ink.CLEAN);
        assertEquals(192, clean.width());
        assertEquals(96, clean.height());
        final PrintedPicture.Raster tall = PrintedPicture.print(new PixImage(10, 40), PrinterModel.Ink.CLEAN);
        assertEquals(192, tall.height());
    }

    @Test
    void print_dotMatrixPrintsInDotsOfItsHead() {
        final PrintedPicture.Raster dots = PrintedPicture.print(filled(96, 48, DARK), PrinterModel.Ink.DOT_MATRIX);
        assertTrue(dots.width() > 96 * 3, "each pixel of the picture becomes a dot several pixels wide");
    }

    @Test
    void print_laserIsGreyOnly() {
        final PixImage picture = filled(8, 8, DARK);
        final PrintedPicture.Raster toner = PrintedPicture.print(picture, PrinterModel.Ink.LASER);
        for (final int p : toner.pixels()) {
            if ((p >>> 24) != 0) {
                assertEquals((p >> 16) & 0xFF, (p >> 8) & 0xFF, "toner has no colour");
            }
        }
    }

    private static PixImage filled(final int w, final int h, final int colour) {
        final PixImage picture = new PixImage(w, h);
        picture.fillAll(colour);
        return picture;
    }
}
