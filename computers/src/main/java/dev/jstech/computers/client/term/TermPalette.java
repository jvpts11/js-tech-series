/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.gui.ColorContrast;
import java.util.function.ToIntFunction;

/**
 * The colour each style has on a terminal that can show colour.
 *
 * <p>One palette for both terminals, since they are two windows onto one console and a line that is green in
 * one had better be the same green in the other. A glass that cannot show colour, a one-colour tube, maps
 * what this answers onto the one colour it has.
 *
 * <p>There are two sets of inks because there are two kinds of ground. Most terminals are a dark glass and their
 * colours are lights; a workstation's terminal window was paper, and on paper the same meanings are written in
 * dark inks, or none of them could be read.
 */
public final class TermPalette {

    /** The ground of a terminal window that is paper rather than glass: a warm white, as those windows were. */
    public static final int PAPER = 0xFFF4F1E8;

    /** How light a ground has to be before its inks are the dark ones. */
    private static final double LIGHT_GROUND = 0.5;

    private static final ToIntFunction<CliStyle> ON_GLASS = TermPalette::colorOf;
    private static final ToIntFunction<CliStyle> ON_PAPER = TermPalette::onPaper;

    private TermPalette() {
    }

    /** Whether that ground is paper rather than glass, which is what decides the inks written on it. */
    public static boolean lightGround(final int ground) {
        return ColorContrast.luminance(ground) > LIGHT_GROUND;
    }

    /** The inks that read on that ground: the dark ones on a light ground, the lights on a dark one. */
    public static ToIntFunction<CliStyle> inksFor(final int ground) {
        return lightGround(ground) ? ON_PAPER : ON_GLASS;
    }

    /**
     * What picked-out text is drawn on: a wash of the ink the glass writes in, laid under the letters.
     *
     * <p>Not the reverse video a hardware terminal did, because these letters carry meaning in their colour and
     * a selection that swapped ink for ground would have to throw that away. A wash keeps every colour on the
     * glass readable and still says plainly where the selection begins and ends.
     */
    public static int selectionOn(final int ground) {
        return lightGround(ground) ? 0x40111111 : 0x50CDD6E2;
    }

    public static int colorOf(final CliStyle style) {
        return switch (style) {
            case PROMPT -> 0xFFCDD6E2;         // light gray-white for the echoed command line
            case ACCENT, HEADER -> 0xFF39D6C4; // cyan for system messages
            case OK -> 0xFF5FE07A;             // green for success
            case ERROR -> 0xFFEF6A5A;          // red for errors
            case WARN -> 0xFFF0B23A;           // amber for warnings
            case INFO -> 0xFF2AA7E0;           // blue for informational output
            case DIM -> 0xFF7D8A9C;            // dim gray for hints and secondary output
            // The extended palette: brand-tinted terminal colours (screenfetch logos and the like).
            case ORANGE -> 0xFFE95420;
            case MAGENTA -> 0xFFDB5FDB;        // a terminal's magenta, the one Gentoo's logo is drawn in
            case BLUE -> 0xFF5A8FD6;
            case CYAN -> 0xFF2FA6E8;
            case PURPLE -> 0xFF9E8FD6;
            case RED -> 0xFFD8332C;
            case GREEN -> 0xFF5FD35F;
            case YELLOW -> 0xFFE8D23C;
            case BRIGHT -> 0xFFFFFFFF;         // the terminal's bold: what a tool wants read first
            default -> 0xFFCDD6E2;             // plain = light gray
        };
    }

    /** The same meanings in inks dark enough to be read on {@link #PAPER}. */
    public static int onPaper(final CliStyle style) {
        return switch (style) {
            case ACCENT, HEADER -> 0xFF0B5F5A; // deep teal for system messages
            case OK -> 0xFF1B6B2A;             // deep green for success
            case ERROR -> 0xFFA3211A;          // deep red for errors
            case WARN -> 0xFF7A4B00;           // brown amber for warnings
            case INFO -> 0xFF14508F;           // deep blue for informational output
            case DIM -> 0xFF5C5A52;            // warm gray for hints and secondary output
            case ORANGE -> 0xFF9C3A10;
            case MAGENTA -> 0xFF8A2A8A;
            case BLUE -> 0xFF2A4F9A;
            case CYAN -> 0xFF0F5C86;
            case PURPLE -> 0xFF5A3F9E;
            case RED -> 0xFFB0201A;
            case GREEN -> 0xFF2A6B1A;
            case YELLOW -> 0xFF6B5800;
            case BRIGHT -> 0xFF000000;         // bold on paper is the blackest ink there is
            default -> 0xFF111111;             // plain and the echoed command line = near black
        };
    }
}
