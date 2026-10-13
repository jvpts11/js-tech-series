/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours of the Nextgre Planner Studio beside the system's own: the house's blue on the plan's boxes, its bars
 * and its status bar. A step's box is light whatever the system's look, a card laid on the canvas, so its text is
 * dark on every desktop. Pure, so a test reads every pairing.
 */
@PaletteHolder
public final class NextgreStudioPalette {

    /** The studio's colours: app/nextgre_studio. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/nextgre_studio",
            new Colours(0xFF2F6DB5, 0xFF2F6DB5, 0xFFFFFFFF, 0xFFF7FAFD, 0xFF1B1B1B, 0xFF4F5866, 0xFF9FB4E6,
                    0xFF2F6DB5, 0xFFE2E6EC, 0xFF8C96A3, 0xFF2F6DB5, 0xFFFFFFFF, 0xFFDCE8F7, 0xFFB86E00, 0xFF1E7F34,
                    0xFFB3263A, 0xFFFFF8E5, 0xFF6A5310));

    private NextgreStudioPalette() {
    }

    /** The studio's colours as the palette file sets them. */
    public static Colours colours() {
        return PALETTE.get();
    }

    /**
     * The studio's colours.
     *
     * @param accent     the house's blue: a box's edge, the tab underline
     * @param status     the status bar's ground
     * @param statusText the status bar's text
     * @param box        a step's box
     * @param boxText    the text in a box
     * @param boxDim     a box's second line and its times
     * @param estimate   the bar of the time reckoned
     * @param actual     the bar of the time taken
     * @param track      the ground under the two bars
     * @param line       the lines from a box to the boxes under it
     * @param chip       a hint's chip on a box
     * @param chipText   the text on a chip
     * @param chosen     the ground of the chosen plan in the list of plans weighed
     * @param running    a step running now
     * @param good       a plan that ran to the end
     * @param bad        a plan set aside, or one that fell short
     * @param note       the ground of a note another mod added
     * @param noteText   the text of that note
     */
    public record Colours(int accent, int status, int statusText, int box, int boxText, int boxDim, int estimate,
                          int actual, int track, int line, int chip, int chipText, int chosen, int running, int good,
                          int bad, int note, int noteText) {
    }
}
