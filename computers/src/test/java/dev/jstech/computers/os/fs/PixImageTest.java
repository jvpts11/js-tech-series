/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class PixImageTest {

    private PixImage image;

    @BeforeEach
    void setUp() {
        image = new PixImage(16, 12);
    }

    @Test
    void newImage_startsEntirelyTransparent() {
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                assertEquals(0, image.get(x, y));
            }
        }
        assertTrue(PixImage.isTransparent(0));
        assertFalse(PixImage.isTransparent(1));
    }

    @Test
    void set_thenGet_givesTheColourBack() {
        image.set(3, 4, 7);
        assertEquals(7, image.get(3, 4));
    }

    @Test
    void set_outsideTheCanvasIsIgnoredRatherThanThrowing() {
        image.set(-1, 0, 5);
        image.set(0, -1, 5);
        image.set(image.width(), 0, 5);
        image.set(0, image.height(), 5);
        assertEquals(0, image.get(0, 0));
    }

    @Test
    void get_outsideTheCanvasIsNothing() {
        assertEquals(0, image.get(-1, -1));
        assertEquals(0, image.get(100, 100));
    }

    @Test
    void aCanvas_hasToBeWithinTheSizeAMachineCanHold() {
        assertThrows(IllegalArgumentException.class, () -> new PixImage(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new PixImage(5, 0));
        assertThrows(IllegalArgumentException.class, () -> new PixImage(PixImage.MAX_SIDE + 1, 5));
        assertThrows(IllegalArgumentException.class, () -> new PixImage(5, PixImage.MAX_SIDE + 1));
    }

    @Test
    void line_paintsBothEnds() {
        image.line(0, 0, 10, 6, 4);
        assertEquals(4, image.get(0, 0));
        assertEquals(4, image.get(10, 6));
    }

    @Test
    void line_paintsAContinuousRunWithNoGaps() {
        image.line(0, 0, 15, 11, 9);
        // Every row the line crosses has at least one painted pixel, which is what no gaps means here.
        for (int y = 0; y <= 11; y++) {
            boolean any = false;
            for (int x = 0; x < image.width() && !any; x++) {
                any = image.get(x, y) == 9;
            }
            assertTrue(any, "row " + y + " has a gap in the line");
        }
    }

    @Test
    void rectangle_paintsTheEdgesAndLeavesTheMiddle() {
        image.rectangle(2, 2, 8, 6, 3);
        assertEquals(3, image.get(2, 2));
        assertEquals(3, image.get(8, 6));
        assertEquals(3, image.get(5, 2));
        assertEquals(0, image.get(5, 4), "a rectangle is an outline, not a block");
    }

    @Test
    void ellipse_paintsSomethingAndStaysInsideItsBox() {
        image.ellipse(1, 1, 11, 9, 6);
        boolean any = false;
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                if (image.get(x, y) == 6) {
                    any = true;
                    assertTrue(x >= 0 && x <= 12 && y >= 0 && y <= 10,
                            "the ellipse ran outside its box at " + x + "," + y);
                }
            }
        }
        assertTrue(any, "the ellipse painted nothing");
    }

    @Test
    void fill_floodsTheAreaAPointSitsIn() {
        image.rectangle(0, 0, 15, 11, 2);
        image.fill(5, 5, 8);
        assertEquals(8, image.get(5, 5));
        assertEquals(8, image.get(1, 1), "the fill should reach the whole inside");
        assertEquals(2, image.get(0, 0), "the fill should stop at the outline");
    }

    @Test
    void fill_doesNothingWhenTheColourIsAlreadyThere() {
        image.fillAll(5);
        image.fill(0, 0, 5);
        assertEquals(5, image.get(8, 8));
    }

    @Test
    void fill_ofAWholeLargeCanvasDoesNotRunOutOfRoom() {
        // The whole canvas in one go is the case a fixed stack used to overflow on.
        final PixImage big = new PixImage(PixImage.MAX_SIDE, PixImage.MAX_SIDE);
        big.fill(0, 0, 3);
        assertEquals(3, big.get(PixImage.MAX_SIDE - 1, PixImage.MAX_SIDE - 1));
        assertEquals(3, big.get(64, 64));
    }

    @Test
    void fill_outsideTheCanvasDoesNothing() {
        image.fill(-1, -1, 5);
        assertEquals(0, image.get(0, 0));
    }

    @Test
    void encode_thenDecode_givesTheSamePictureBack() {
        image.fillAll(4);
        image.rectangle(2, 2, 10, 8, 9);
        image.set(5, 5, 200);
        final PixImage back = PixImage.decode(image.encode());
        assertNotNull(back);
        assertEquals(image.width(), back.width());
        assertEquals(image.height(), back.height());
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                assertEquals(image.get(x, y), back.get(x, y), "pixel " + x + "," + y + " came back wrong");
            }
        }
    }

    @Test
    void aFlatPicture_costsAlmostNothing() {
        final PixImage big = new PixImage(PixImage.MAX_SIDE, PixImage.MAX_SIDE);
        big.fillAll(7);
        final int bytes = big.encode().getBytes(StandardCharsets.UTF_8).length;
        // Sixteen thousand pixels of one colour, written in under a hundred characters.
        assertTrue(bytes < 100, "a picture of one colour should be a handful of bytes, was " + bytes);
    }

    @Test
    void aDrawnPicture_staysAroundAKilobyte() {
        /*
         * The claim the program is built on: a full canvas of ordinary drawing, bands and shapes rather
         * than noise, fits in about a kilobyte where a colour per pixel would be twenty-four.
         */
        final PixImage big = new PixImage(PixImage.MAX_SIDE, 96);
        for (int y = 0; y < big.height(); y++) {
            big.line(0, y, big.width() - 1, y, 16 + y / 8);
        }
        big.ellipse(20, 20, 90, 70, 4);
        big.rectangle(10, 10, 110, 80, 8);
        final int bytes = big.encode().getBytes(StandardCharsets.UTF_8).length;
        assertTrue(bytes < 3000, "an ordinary drawing should stay small, was " + bytes);
    }

    @Test
    void theBusiestPossiblePicture_stillFitsInAFile() {
        /*
         * The one that matters: a file is handed across to a screen with a cap on it, and a picture that
         * would not fit cannot be saved at all. The worst case is a canvas where no two neighbours match,
         * which is what this builds, and it has to stay under that cap whatever is drawn.
         */
        final PixImage noise = new PixImage(PixImage.MAX_SIDE, PixImage.MAX_SIDE);
        int next = 0;
        for (int y = 0; y < noise.height(); y++) {
            for (int x = 0; x < noise.width(); x++) {
                next = (next * 1103515245 + 12345) & 0x7fffffff;
                noise.set(x, y, 1 + (next >> 16) % (PixImage.COLOURS - 1));
            }
        }
        final int bytes = noise.encode().getBytes(StandardCharsets.UTF_8).length;
        assertTrue(bytes < 32768,
                "the busiest picture must still fit what a file may carry, was " + bytes);
        final PixImage back = PixImage.decode(noise.encode());
        assertNotNull(back, "and it has to come back");
        assertEquals(noise.get(64, 64), back.get(64, 64));
        assertEquals(noise.get(127, 127), back.get(127, 127));
    }

    @Test
    void decode_refusesSomethingThatIsNotAPicture() {
        assertNull(PixImage.decode("hello"));
        assertNull(PixImage.decode(null));
        assertNull(PixImage.decode(PixImage.MAGIC));
        assertNull(PixImage.decode(PixImage.MAGIC + "\n16 12\n"));
    }

    @Test
    void decode_refusesABodyThatIsNotWhatItClaims() {
        assertNull(PixImage.decode(PixImage.MAGIC + "\n4 4\nnot base64 at all!!!"));
    }

    @Test
    void decode_refusesAPictureOfTheWrongSizeForItsHeader() {
        final PixImage small = new PixImage(4, 4);
        final String body = small.encode().split("\n", 3)[2];
        assertNull(PixImage.decode(PixImage.MAGIC + "\n8 8\n" + body),
                "a header promising more pixels than the body holds is not a picture");
    }

    @Test
    void decode_refusesASizeThatIsNotACanvas() {
        assertNull(PixImage.decode(PixImage.MAGIC + "\n0 4\nAAAA"));
        assertNull(PixImage.decode(PixImage.MAGIC + "\n999 999\nAAAA"));
        assertNull(PixImage.decode(PixImage.MAGIC + "\nnonsense\nAAAA"));
    }

    @Test
    void copy_isIndependentOfWhatItCameFrom() {
        image.set(1, 1, 5);
        final PixImage other = image.copy();
        other.set(1, 1, 9);
        assertEquals(5, image.get(1, 1), "the copy wrote back into the original");
        assertEquals(9, other.get(1, 1));
    }

    @Test
    void copyFrom_takesAnotherPicturesPixels() {
        final PixImage other = new PixImage(16, 12);
        other.fillAll(6);
        image.copyFrom(other);
        assertEquals(6, image.get(0, 0));
    }

    @Test
    void copyFrom_refusesAPictureOfAnotherSize() {
        final PixImage other = new PixImage(8, 8);
        assertThrows(IllegalArgumentException.class, () -> image.copyFrom(other));
    }

    @Test
    void thePalette_isOpaqueEverywhereButTheFirstColour() {
        assertEquals(0, PixImage.colourOf(0) >>> 24, "index 0 is the absence of colour");
        for (int i = 1; i < PixImage.COLOURS; i++) {
            assertEquals(0xFF, PixImage.colourOf(i) >>> 24, "colour " + i + " is not opaque");
        }
    }

    @Test
    void thePalette_wrapsRatherThanThrowingOnAnIndexOutOfRange() {
        assertEquals(PixImage.colourOf(1), PixImage.colourOf(PixImage.COLOURS + 1));
        assertEquals(PixImage.colourOf(PixImage.COLOURS - 1), PixImage.colourOf(-1));
    }

    @Test
    void pix_isAFileTypeOfItsOwn() {
        assertEquals(FileType.PIX, FileType.of(PixImage.EXTENSION));
    }
}
