/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the physics tool's panel and beam: {@code jscore:physics_tool}. */
@PaletteHolder
final class PhysicsToolPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "physics_tool", new Colours(
            0xD0202428, 0xFFE8ECF2, 0xFFFFFFFF, 0xFFE8E8E8, 0xFFA0A4AC, 0xD0283444, 0xFFE8ECF2, 0xFF40E0E0,
            0xFFE8FCFF, 0x7040C8F0, 0xFF40E0E0, 0xFFFFFFFF));

    private PhysicsToolPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The tool's colours.
     *
     * @param panel     the panel's ground
     * @param title     the tool's name on it
     * @param chosen    the frame round the mode chosen
     * @param text      a mode's name and the strength
     * @param hint      what the wheel does
     * @param label     the ground of the label by what is held
     * @param labelName the structure's name and mass on it
     * @param labelHeld how far it is held
     * @param beamCore  the bright core of the beam
     * @param beamGlow  the glow round it
     * @param ring      the ring where the beam holds, and the edge round what it holds
     * @param dot       the dots along the beam
     */
    record Colours(int panel, int title, int chosen, int text, int hint, int label, int labelName, int labelHeld,
                   int beamCore, int beamGlow, int ring, int dot) {
    }
}
