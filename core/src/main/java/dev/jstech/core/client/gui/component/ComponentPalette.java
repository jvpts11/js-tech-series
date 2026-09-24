/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours the toolkit's components keep for themselves, the ones a skin has no role for:
 * {@code jscore:gui/components}.
 */
@PaletteHolder
final class ComponentPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/components",
            new Colours(0xFFFFFFFF, 0xFF000000, 0x66FFFFFF, 0x88000000, 0x30808080, 0xA0909090, 0x663A72B0,
                    0xFF101820, 0xFF40C060, 0x50CDD6E2));

    private ComponentPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The components' colours.
     *
     * @param litText        text on the accent: a lit menu item, an open menu title, a ticked box's mark
     * @param menuShade      the hard line around a context menu
     * @param disabled       the translucent wash over a control that cannot be used right now
     * @param popupDim       what dims a program behind a popup
     * @param barTrack       a text area's scrollbar track
     * @param barThumb       a text area's scrollbar thumb
     * @param selection      the wash over selected text in a field
     * @param consoleGround  a command line's ground
     * @param consolePrompt  a command line's prompt and text
     * @param consolePicked  the wash over a command line's picked suggestion
     */
    record Colours(int litText, int menuShade, int disabled, int popupDim, int barTrack, int barThumb, int selection,
                   int consoleGround, int consolePrompt, int consolePicked) {
    }
}
