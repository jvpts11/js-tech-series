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
 * links, after the ring binders technical manuals came in.
 */
@PaletteHolder
public final class GuidePalettes {

    public static final Palette<Binder> BINDER = Palettes.declare(JsCore.MODID, "guide/binder", new Binder(
            0xFF1F2B44, 0xFF2E3D5C, 0xFFEBE5D3, 0xFFD3CCB6, 0xFF23252A, 0xFF6E6A5E, 0xFF1F3F8A, 0xFF1F3F8A,
            0xFFA39C86, 0xFFE1DAC4, 0xFFF3E6C8, 0xFFC9CCD1, 0xFFF4F1E6, 0xFF23252A, 0xFFB8BCC2, 0xFF23252A,
            0xFF8A4F12));

    private GuidePalettes() {
    }

    /**
     * Every colour a manual's style fills, each named as the style's role is.
     *
     * @param cover     the binder's vinyl
     * @param coverEdge the light edge of the vinyl and of its buttons
     * @param paper     the pages
     * @param gutter    the paper's shade where it goes into the spine
     * @param ink       running text
     * @param faint     the heads and feet of pages, dots, things said in passing
     * @param heading   headings and titles
     * @param link      what leads to another page
     * @param rule      lines across the page, frames
     * @param shade     the ground of a figure and of a table's head
     * @param highlight the ground of a warning
     * @param ring      the binder's rings
     * @param label     the cover's label
     * @param labelInk  what the label says
     * @param tab       a chapter's tab when its chapter names no colour
     * @param tabInk    the number on a tab
     * @param warning   a warning's word and frame
     */
    public record Binder(int cover, int coverEdge, int paper, int gutter, int ink, int faint, int heading, int link,
                         int rule, int shade, int highlight, int ring, int label, int labelInk, int tab, int tabInk,
                         int warning) {
    }
}
