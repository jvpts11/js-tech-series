/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

/**
 * How a manual's buttons and page arrows look in each state, worked out from the cover's colours alone so a style's
 * palette can be checked in a test. The mark is drawn in whichever of the page's ink and the label's colour reads
 * better on the button's face; under the pointer the face lightens and is framed in the mark's colour; pressed, it
 * darkens, its bevel turns over and the mark sinks a pixel; leading nowhere, the mark fades into the face.
 *
 * @param fill   the button's face
 * @param light  its lit edges, top and left
 * @param dark   its shaded edges, bottom and right
 * @param mark   the colour of its mark
 * @param framed whether a frame in the mark's colour goes round it
 * @param drop   how far down its mark is drawn, one pixel when pressed
 */
public record ManualButtonLook(int fill, int light, int dark, int mark, boolean framed, int drop) {

    /** The least contrast a mark keeps with its face: what body text needs to be read. */
    public static final double LEAST_CONTRAST = 4.5;
    /** How far a face under the pointer goes towards white, a pressed one towards black. */
    private static final double LIT = 0.35;
    private static final double PRESSED = 0.12;
    /** How far a mark that leads nowhere goes into its face. */
    private static final double FADED = 0.6;
    /** How dark a bevel's shaded edge is, and how light its lit one: a share of the way to black, and to white. */
    private static final double SHADE = 0.4;
    private static final double LIGHT = 0.4;
    /** A colour channel at its brightest. */
    private static final int FULL = 255;

    /** The look of a button whose face is {@code face}, its mark chosen between {@code ink} and {@code label}. */
    public static ManualButtonLook of(final int face, final int ink, final int label, final State state) {
        final int mark = markFor(face, ink, label);
        return switch (state) {
            case NORMAL -> new ManualButtonLook(face, lit(face), shaded(face), mark, false, 0);
            case HOVER -> {
                final int fill = lighten(face, LIT);
                yield new ManualButtonLook(fill, lit(fill), shaded(fill), mark, true, 0);
            }
            case PRESSED -> {
                final int fill = darken(face, PRESSED);
                yield new ManualButtonLook(fill, shaded(fill), lit(fill), mark, false, 1);
            }
            case DISABLED -> new ManualButtonLook(face, lit(face), shaded(face), mix(mark, face, FADED), false, 0);
        };
    }

    /** Whichever of the two reads better on the face: the one with the more contrast against it. */
    public static int markFor(final int face, final int ink, final int label) {
        return ColorContrast.ratio(ink, face) >= ColorContrast.ratio(label, face) ? ink : label;
    }

    /** The colour {@code share} of the way from {@code from} to {@code to}, keeping the first's alpha. */
    public static int mix(final int from, final int to, final double share) {
        final int red = (int) Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * share);
        final int green = (int) Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * share);
        final int blue = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * share);
        return from >>> 24 << 24 | red << 16 | green << 8 | blue;
    }

    /** The colour {@code share} of the way to white, keeping its alpha. */
    public static int lighten(final int colour, final double share) {
        final int red = colour >> 16 & 0xFF;
        final int green = colour >> 8 & 0xFF;
        final int blue = colour & 0xFF;
        final int litRed = (int) Math.round(red + (FULL - red) * share);
        final int litGreen = (int) Math.round(green + (FULL - green) * share);
        final int litBlue = (int) Math.round(blue + (FULL - blue) * share);
        return colour >>> 24 << 24 | litRed << 16 | litGreen << 8 | litBlue;
    }

    /** The colour {@code share} of the way to black, keeping its alpha. */
    public static int darken(final int colour, final double share) {
        final double kept = 1.0 - share;
        return colour >>> 24 << 24 | (int) Math.round((colour >> 16 & 0xFF) * kept) << 16
                | (int) Math.round((colour >> 8 & 0xFF) * kept) << 8 | (int) Math.round((colour & 0xFF) * kept);
    }

    /** The colour lightened for the lit edge of a bevel. */
    public static int lit(final int colour) {
        return lighten(colour, LIGHT);
    }

    /** The colour darkened for the shaded edge of a bevel. */
    public static int shaded(final int colour) {
        return darken(colour, SHADE);
    }

    /** What a button is doing. */
    public enum State {
        /** At rest. */
        NORMAL,
        /** Under the pointer. */
        HOVER,
        /** Held down. */
        PRESSED,
        /** Leading nowhere now, as Back does with nothing to go back to. */
        DISABLED
    }
}
