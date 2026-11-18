/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.jei;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The words and the ink of the Mask Writing pages ({@link MaskWritingCategory}). Kept apart from the category and free
 * of the viewer's types, so the language and palette generators read them whether or not the viewer is installed.
 */
@TextHolder
@PaletteHolder
final class MaskWritingLook {

    static final TextKey TITLE = TextKey.of("jsc.jei.mask_writing", "Mask Writing");
    static final TextKey SECONDS = TextKey.of("jsc.jei.mask_writing.seconds", "%s s");
    static final TextKey IN_THE_SLOT = TextKey.of("jsc.jei.mask_writing.in_the_slot", "in the mask slot");
    static final TextKey NEVER_USED_UP = TextKey.of("jsc.jei.mask_writing.never_used_up", "never used up");
    static final TextKey ITS_SLOT = TextKey.of("jsc.jei.mask_writing.its_slot", "its mask slot");
    static final TextKey COMPUTER = TextKey.of("jsc.jei.mask_writing.computer", "A %s computer or newer,");
    static final TextKey LINKED = TextKey.of("jsc.jei.mask_writing.linked", "on, linked to its peripheral port");
    static final TextKey PROGRAM = TextKey.of("jsc.jei.mask_writing.program", "Lithography program:");
    static final TextKey PICK = TextKey.of("jsc.jei.mask_writing.pick", "pick %s, then Write");
    static final TextKey FASTER = TextKey.of("jsc.jei.mask_writing.faster", "%s on a %s computer or newer");
    /** The page's ink: what it says, what it says under that, and the line under the mask. */
    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "jei/mask_writing",
            new Colours(0xFF404040, 0xFF646464, 0xFF8B8B8B));

    private MaskWritingLook() {
    }

    /**
     * The page's ink.
     *
     * @param text what it says
     * @param dim  what it says under that
     * @param rule the line under the mask
     */
    record Colours(int text, int dim, int rule) {
    }
}
