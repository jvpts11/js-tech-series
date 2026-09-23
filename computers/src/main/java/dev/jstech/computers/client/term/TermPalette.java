/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.gui.ColorContrast;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
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
 * dark inks, or none of them could be read. Each set is a declared palette, {@code jsc:term/glass} and
 * {@code jsc:term/paper}, which a resource pack can recolour.
 */
@PaletteHolder
public final class TermPalette {

    /** The lights a dark glass writes in; its ground is the bare glass of a monitor. */
    public static final Palette<TermInks> GLASS = Palettes.declare(JsComputers.MODID, "term/glass", new TermInks(
            0xFF000000, 0xFFCDD6E2, 0xFFCDD6E2, 0xFF39D6C4, 0xFF5FE07A, 0xFFEF6A5A, 0xFFF0B23A, 0xFF2AA7E0,
            0xFF7D8A9C, 0xFFE95420, 0xFFDB5FDB, 0xFF5A8FD6, 0xFF2FA6E8, 0xFF9E8FD6, 0xFFD8332C, 0xFF5FD35F,
            0xFFE8D23C, 0xFFFFFFFF, 0x50CDD6E2));

    /** The inks of a terminal window that is paper rather than glass: a warm white, as those windows were. */
    public static final Palette<TermInks> PAPER = Palettes.declare(JsComputers.MODID, "term/paper", new TermInks(
            0xFFF4F1E8, 0xFF111111, 0xFF111111, 0xFF0B5F5A, 0xFF1B6B2A, 0xFFA3211A, 0xFF7A4B00, 0xFF14508F,
            0xFF5C5A52, 0xFF9C3A10, 0xFF8A2A8A, 0xFF2A4F9A, 0xFF0F5C86, 0xFF5A3F9E, 0xFFB0201A, 0xFF2A6B1A,
            0xFF6B5800, 0xFF000000, 0x40111111));

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
        return (lightGround(ground) ? PAPER : GLASS).get().selection();
    }

    /** The light a line of that style is written in on a dark glass. */
    public static int colorOf(final CliStyle style) {
        return GLASS.get().of(style);
    }

    /** The same meanings in inks dark enough to be read on the paper. */
    public static int onPaper(final CliStyle style) {
        return PAPER.get().of(style);
    }
}
