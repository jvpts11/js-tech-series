/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.camera;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the camera: {@code jscore:camera}. */
@PaletteHolder
final class CameraPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "camera", new Colours(0xFF000000));

    private CameraPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The camera's colours.
     *
     * @param outsideLens the screen around a lens looked through, as the spyglass leaves it
     */
    record Colours(int outsideLens) {
    }
}
