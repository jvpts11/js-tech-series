/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The words and the ink of the chain's pages no recipe holds ({@link ChainRecipePages}): the titles of a wafer's steps
 * and of the Air Separation Unit's gases, and the Mask Writing pages' words and colours.
 */
@TextHolder
@PaletteHolder
final class ChainPagesLook {

    static final TextKey LITHOGRAPHY = TextKey.of("jsc.jei.wafer.lithography", "Lithography");
    static final TextKey ETCH = TextKey.of("jsc.jei.wafer.etch", "Plasma Etching");
    static final TextKey IMPLANT = TextKey.of("jsc.jei.wafer.implant", "Ion Implantation");
    static final TextKey DEPOSITION = TextKey.of("jsc.jei.wafer.deposition", "Deposition");
    static final TextKey PACKAGING = TextKey.of("jsc.jei.wafer.packaging", "Packaging");
    static final TextKey AIR_SEPARATION = TextKey.of("jsc.recipe_view.air_separation", "Air Separation");
    static final TextKey MASK_WRITING = TextKey.of("jsc.jei.mask_writing", "Mask Writing");
    static final TextKey SECONDS = TextKey.of("jsc.jei.mask_writing.seconds", "%s s");
    static final TextKey IN_THE_SLOT = TextKey.of("jsc.jei.mask_writing.in_the_slot", "in the mask slot");
    static final TextKey NEVER_USED_UP = TextKey.of("jsc.jei.mask_writing.never_used_up", "never used up");
    static final TextKey ITS_SLOT = TextKey.of("jsc.jei.mask_writing.its_slot", "its mask slot");
    static final TextKey COMPUTER = TextKey.of("jsc.jei.mask_writing.computer", "A %s computer or newer,");
    static final TextKey LINKED = TextKey.of("jsc.jei.mask_writing.linked", "on, linked to its peripheral port");
    static final TextKey PROGRAM = TextKey.of("jsc.jei.mask_writing.program", "Lithography program:");
    static final TextKey PICK = TextKey.of("jsc.jei.mask_writing.pick", "pick %s, then Write");
    static final TextKey MOULD_CUTTING = TextKey.of("jsc.jei.mould_cutting", "Mould Cutting");
    static final TextKey IN_THE_MOULD_SLOT = TextKey.of("jsc.jei.mould_cutting.in_the_slot", "in the mould slot");
    static final TextKey ITS_MOULD_SLOT = TextKey.of("jsc.jei.mould_cutting.its_slot", "its mould slot");
    static final TextKey PICK_CUT = TextKey.of("jsc.jei.mould_cutting.pick", "pick %s, then Cut Mould");
    static final TextKey FILM_PRINTING = TextKey.of("jsc.jei.film_printing", "Film Printing");
    static final TextKey IN_THE_TRAY = TextKey.of("jsc.jei.film_printing.in_the_tray", "in the printer's tray");
    static final TextKey USED_FOR_ITS_MASK = TextKey.of("jsc.jei.film_printing.used_for_its_mask",
            "used up writing its mask");
    static final TextKey ITS_TRAY = TextKey.of("jsc.jei.film_printing.its_tray", "Blank Film in its tray");
    static final TextKey LINKED_PRINTER = TextKey.of("jsc.jei.film_printing.linked", "on, the printer linked to it");
    static final TextKey PICK_PRINT = TextKey.of("jsc.jei.film_printing.pick", "pick %s, then Print Film");
    static final TextKey FASTER = TextKey.of("jsc.jei.mask_writing.faster", "%s on a %s computer or newer");
    /** A Mask Writing page's ink: what it says, what it says under that, and the line under the mask. */
    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "jei/mask_writing",
            new Colours(0xFF404040, 0xFF646464, 0xFF8B8B8B));

    private ChainPagesLook() {
    }

    /**
     * A Mask Writing page's ink.
     *
     * @param text what it says
     * @param dim  what it says under that
     * @param rule the line under the mask
     */
    record Colours(int text, int dim, int rule) {
    }
}
