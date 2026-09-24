/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.screen;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours of the toolkit's vanilla screens, the container, the modal dialog and the tabs:
 * {@code jscore:gui/screens}.
 */
@PaletteHolder
final class ScreenPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/screens",
            new Colours(0xF0202020, 0x90000000, 0xFFFFFFFF, 0xFFBBBBBB, 0xFF3A3A3A, 0xFF1E1E1E, 0xFFFFFFFF));

    private ScreenPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The screens' colours.
     *
     * @param containerGround the ground a container screen is drawn on
     * @param dialogDim       what dims the screen behind a modal dialog
     * @param dialogTitle     a modal dialog's title
     * @param dialogMessage   a modal dialog's message
     * @param tabActive       the tab that is open
     * @param tabIdle         the other tabs
     * @param tabText         a tab's title
     */
    record Colours(int containerGround, int dialogDim, int dialogTitle, int dialogMessage, int tabActive, int tabIdle,
                   int tabText) {
    }
}
