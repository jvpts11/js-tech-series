/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteRoles;
import dev.jstech.core.palette.Palettes;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Reads a manual's colours as its style and its chapters name them: a declared palette by its id, whose roles a
 * resource pack can recolour, or a colour written out as {@code #AARRGGBB}.
 */
final class PaletteLookup {

    private PaletteLookup() {
    }

    /** Every role of the declared palette of that id, as it is now; nothing when no palette has that id. */
    static Map<String, Integer> roles(final String paletteId) {
        for (final Palette<?> palette : Palettes.all()) {
            if (palette.id().equals(paletteId)) {
                return PaletteRoles.read(palette.get());
            }
        }
        return Map.of();
    }

    /** A colour written {@code #AARRGGBB} or {@code #RRGGBB}, or null when it is not one. */
    @Nullable
    static Integer parse(final String written) {
        return PaletteRoles.parse(written);
    }

    /** A colour named either way: written out, or a palette's id whose role it is; null for neither. */
    @Nullable
    static Integer colourOf(final String named, final String role) {
        if (named.isEmpty()) {
            return null;
        }
        return named.startsWith("#") ? parse(named) : roles(named).get(role);
    }
}
