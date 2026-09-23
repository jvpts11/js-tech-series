/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.theme;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.gui.theme.EraPalette;
import dev.jstech.core.client.gui.theme.EraTheme;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The Network Management Studio's color palette, kept apart from {@link NmsThemes} so it stays pure (an
 * {@link EraPalette} is plain data, while {@link NmsThemes} drags in Minecraft through {@link EraTheme}).
 * That lets the palette's text/background contrast be unit-tested, since the Studio shipped unreadable-text bugs
 * twice, so the readable-contrast guarantee now lives in a test over these exact values.
 */
@PaletteHolder
public final class NmsPalette {

    /** The classic SSMS light skin: light-grey window, white panels, grey separators, system-blue accents. */
    public static final Palette<EraPalette> SSMS_LIGHT = Palettes.declare(JsComputers.MODID, "nms/ssms_light",
            new EraPalette(
                    0xFF6E6E6E, 0xFFECECEC, 0xFFEEEEEE, 0xFFFFFFFF, 0xFFC4C4C4, 0xFFD4D4D4,
                    0xFFFFFFFF, 0xFFC4C4C4,
                    0xFF007ACC, 0xFF0A6CBA,
                    0xFF2E8B2E, 0xFFE6B800, 0xFFC0392B,
                    0xFF1E1E1E, 0xFF8A8A8A,
                    0xFFFFFFFF, 0xFF1E1E1E, 0xFFCCE8FF,
                    0, 0, 0, 0));

    private NmsPalette() {
    }
}
