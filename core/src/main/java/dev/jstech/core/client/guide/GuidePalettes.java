/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours of the Core's binder style: navy vinyl covers, cream pages, dark ink and a deep blue for headings and
 * links, after the ring binders technical manuals came in. A mod's own manual declares a palette of the same record
 * under its own id, and its style names it.
 */
@PaletteHolder
public final class GuidePalettes {

    public static final Palette<Binder> BINDER = Palettes.declare(JsCore.MODID, "guide/binder", new Binder(
            0xFF1F2B44, 0xFF2E3D5C, 0xFFEBE5D3, 0xFFD3CCB6, 0xFF23252A, 0xFF6E6A5E, 0xFF1F3F8A, 0xFF1F3F8A,
            0xFFA39C86, 0xFFE1DAC4, 0xFFF3E6C8, 0xFFC9CCD1, 0xFFF4F1E6, 0xFF23252A, 0xFFB8BCC2, 0xFF23252A,
            0xFF8A4F12, 0xFF1F3F8A, 0xFF23252A, 0xFF39D6C4, 0xFFD3CCB6, 0xFF121A2A, 0xFF1F3F8A,
            0xFFF5F1E4, 0xFFD8CEB2, 0xFF1F3F8A, 0xFF0F6E66, 0xFF9A3B16, 0xFF7A2D8A, 0xFF6E6A5E, 0xFFCFE8CF));

    /** The look of a terminal a session names when it names none of its own: a dark console with a green prompt. */
    public static final Palette<Terminal> TERMINAL = Palettes.declare(JsCore.MODID, "guide/terminal",
            new Terminal(0xFF14161C, 0xFFCDD6E2, 0xFF5FE07A, 0xFFF4F1E6));

    private GuidePalettes() {
    }

    /**
     * The colours of a terminal a manual's session is printed in, as one system draws its console. The strip with
     * the terminal's name is its ground lightened, in the colour of what is typed.
     *
     * @param ground the console's ground
     * @param text   what the system says
     * @param prompt the prompt
     * @param typed  what the player types, and the terminal's name on its strip
     */
    public record Terminal(int ground, int text, int prompt, int typed) {
    }

    /**
     * Every colour a manual's style fills, each named as the style's role is.
     *
     * @param cover     the binder's vinyl, or the folder's board
     * @param coverEdge the light edge of the cover and of its buttons
     * @param paper     the pages
     * @param gutter    the paper's shade where it goes into the spine
     * @param ink       running text
     * @param faint     the heads and feet of pages, dots, things said in passing
     * @param heading   headings and titles
     * @param link      what leads to another page
     * @param rule      lines across the page, frames
     * @param shade     the ground of a figure and of a table's head
     * @param highlight the ground of a warning
     * @param ring      the binder's rings and the rivets on its spine
     * @param label     the cover's label
     * @param labelInk  what the label says
     * @param tab       a chapter's tab when its chapter names no colour
     * @param tabInk    the number on a tab
     * @param warning   a warning's word and frame
     * @param accent    notes, what can go wrong, a link pointed at on a drawing
     * @param number    the numbers of steps and of a legend's balloons
     * @param band      the stripe across a cover near its foot, or the folder's elastic
     * @param grid      a drawing's grid
     * @param spine       a binder's spine
     * @param coverLine   the cover's first line, which names the series or the mod
     * @param codePaper   the paper a listing's code is printed on
     * @param codeHead    the strip over a listing, with its label, its file's name and its button
     * @param codeKeyword the words of a language's own in a listing
     * @param codeName    the names of types and of what a program calls
     * @param codeText    text in quotes
     * @param codeNumber  numbers
     * @param codeComment comments, which the program does not run
     * @param codeCopied  a listing's Copy button for a moment after it has copied
     */
    public record Binder(int cover, int coverEdge, int paper, int gutter, int ink, int faint, int heading, int link,
                         int rule, int shade, int highlight, int ring, int label, int labelInk, int tab, int tabInk,
                         int warning, int accent, int number, int band, int grid, int spine, int coverLine,
                         int codePaper, int codeHead, int codeKeyword, int codeName, int codeText, int codeNumber,
                         int codeComment, int codeCopied) {
    }
}
