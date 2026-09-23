/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;

/**
 * The eight colour schemes CDE's Style Manager offers, under the names the real ones had, in the order it lists
 * them. Each is a declared palette, {@code jsc:cde/<scheme>}, so a resource pack can recolour it.
 *
 * <p>Pure, with no Minecraft types, so a scheme can be named and read without a game running.
 */
@PaletteHolder
public enum CdeScheme {

    DEFAULT("Default", Palettes.declare(JsComputers.MODID, "cde/default", new CdePalette(
            0xFFAEB2C3, 0xFFE3E5EC, 0xFF5E6272, 0xFF7C86B0, 0xFFC7CAD6, 0xFF4C6F82, 0xFF45677A, 0xFF1A1A1A,
            0xFFFFFFFF))),
    ALPINE("Alpine", Palettes.declare(JsComputers.MODID, "cde/alpine", new CdePalette(
            0xFFA9BDB3, 0xFFDDE8E2, 0xFF56675F, 0xFF3F7F6A, 0xFFC4D4CC, 0xFF2F5D50, 0xFF2A5448, 0xFF1A1A1A,
            0xFFFFFFFF))),
    BROICA("Broica", Palettes.declare(JsComputers.MODID, "cde/broica", new CdePalette(
            0xFFB9A9B4, 0xFFE6DCE3, 0xFF675A63, 0xFF8C4F78, 0xFFD2C5CE, 0xFF5B3F56, 0xFF52384D, 0xFF1A1A1A,
            0xFFFFFFFF))),
    CHARCOAL("Charcoal", Palettes.declare(JsComputers.MODID, "cde/charcoal", new CdePalette(
            0xFF8C8F96, 0xFFC4C7CE, 0xFF3F4148, 0xFF4A5E7C, 0xFFA6A9B0, 0xFF2E3138, 0xFF282B31, 0xFF101010,
            0xFFFFFFFF))),
    DESERT("Desert", Palettes.declare(JsComputers.MODID, "cde/desert", new CdePalette(
            0xFFC9B99A, 0xFFEFE6D2, 0xFF6F6350, 0xFFA9603C, 0xFFDDD0B6, 0xFF8C6F4E, 0xFF836746, 0xFF1A1A1A,
            0xFFFFFFFF))),
    NEPTUNE("Neptune", Palettes.declare(JsComputers.MODID, "cde/neptune", new CdePalette(
            0xFF9FB2CC, 0xFFD8E2F0, 0xFF4F6078, 0xFF2F5FA8, 0xFFBCCBE0, 0xFF1F3F6E, 0xFF1B3862, 0xFF1A1A1A,
            0xFFFFFFFF))),
    SEA_FOAM("SeaFoam", Palettes.declare(JsComputers.MODID, "cde/sea_foam", new CdePalette(
            0xFFA8C8C0, 0xFFDCEDE8, 0xFF53706A, 0xFF2E7C70, 0xFFC3DCD6, 0xFF2C6A66, 0xFF27605C, 0xFF1A1A1A,
            0xFFFFFFFF))),
    URCHIN("Urchin", Palettes.declare(JsComputers.MODID, "cde/urchin", new CdePalette(
            0xFFB0A8C8, 0xFFE0DCEE, 0xFF5C5674, 0xFF6A4FA8, 0xFFCAC4DC, 0xFF3E3470, 0xFF372E64, 0xFF1A1A1A,
            0xFFFFFFFF)));

    private final String label;
    private final Palette<CdePalette> palette;

    /** Every scheme, in the order the Style Manager lists them, the default first. */
    public static final List<CdeScheme> ALL = List.of(values());

    CdeScheme(final String label, final Palette<CdePalette> palette) {
        this.label = label;
        this.palette = palette;
    }

    /** The scheme listed under that name, whatever case it is asked for in, or the default for a name nobody has. */
    public static CdeScheme named(final String name) {
        for (final CdeScheme each : ALL) {
            if (each.label.equalsIgnoreCase(name)) {
                return each;
            }
        }
        return DEFAULT;
    }

    /** The name the Style Manager lists the scheme under, which is also how a workstation keeps its choice. */
    public String label() {
        return this.label;
    }

    /** The colours to paint with now; asked for each time, since a resource pack can change them. */
    public CdePalette colours() {
        return this.palette.get();
    }

    public Palette<CdePalette> palette() {
        return this.palette;
    }
}
