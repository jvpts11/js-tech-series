/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.printer;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours of a printed sheet, held apart from any screen so that the reading screen and the item-frame
 * renderer draw the same paper without one depending on the other's interface code.
 */
@PaletteHolder
public final class PrintedPaperColours {

    /** The paper, its bands and holes, the print, and the pager under the sheet. */
    private static final Palette<SheetColours> COLOURS = Palettes.declare(JsComputers.MODID, "screen/printed_paper",
            new SheetColours(0xFFFBFBF8, 0xFFF4F4F0, 0xFFD7EBD7, 0xFFCFCFC8, 0xFF23262C, 0xFF6A707A, 0xFFDDDDDD,
                    0xFF6F6F6F));

    private PrintedPaperColours() {
    }

    /** The colours of the paper. */
    public static SheetColours get() {
        return COLOURS.get();
    }

    /**
     * The colours of a sheet being read.
     *
     * @param paper    a plain sheet
     * @param fanfold  the fanfold's paper between its bands
     * @param bar      the fanfold's green bands
     * @param hole     the fanfold's tractor holes
     * @param ink      the print
     * @param faint    the caption under a picture's name
     * @param pager    the page count and an arrow that turns
     * @param pagerDim an arrow at the end of the document
     */
    public record SheetColours(int paper, int fanfold, int bar, int hole, int ink, int faint, int pager, int pagerDim) {
    }
}
