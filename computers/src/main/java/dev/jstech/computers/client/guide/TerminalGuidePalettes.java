/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.guide;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.guide.GuidePalettes;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * How the manuals print a session at each family of the mod's terminals, so a reader sees the screen they will see:
 * MC-DOS white on black, a Linux console with its green prompt, the Arch Linux media's red root prompt under zsh, and
 * FreeBSD's sh on its warm dark ground.
 */
@PaletteHolder
public final class TerminalGuidePalettes {

    public static final Palette<GuidePalettes.Terminal> DOS = Palettes.declare(JsComputers.MODID,
            "guide/terminal/dos", new GuidePalettes.Terminal(0xFF000000, 0xFFC0C0C0, 0xFFFFFFFF, 0xFFFFFFFF));
    public static final Palette<GuidePalettes.Terminal> BASH = Palettes.declare(JsComputers.MODID,
            "guide/terminal/bash", new GuidePalettes.Terminal(0xFF14161C, 0xFFCDD6E2, 0xFF5FE07A, 0xFFFFFFFF));
    public static final Palette<GuidePalettes.Terminal> ZSH = Palettes.declare(JsComputers.MODID,
            "guide/terminal/zsh", new GuidePalettes.Terminal(0xFF101317, 0xFFCDD6E2, 0xFFEF6A5A, 0xFFFFFFFF));
    public static final Palette<GuidePalettes.Terminal> SH = Palettes.declare(JsComputers.MODID,
            "guide/terminal/sh", new GuidePalettes.Terminal(0xFF1A1010, 0xFFE2D6D6, 0xFFF0B23A, 0xFFFFFFFF));

    private TerminalGuidePalettes() {
    }
}
