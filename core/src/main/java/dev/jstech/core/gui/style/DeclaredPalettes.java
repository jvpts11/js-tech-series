/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolders;
import dev.jstech.core.palette.Palettes;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.loading.FMLEnvironment;

/** The palettes the installed mods declared, found by id, as the styles that take colours from one read them. */
public final class DeclaredPalettes implements IPaletteSource {

    /** The palettes of the game running. */
    public static final DeclaredPalettes INSTANCE = new DeclaredPalettes();

    private static final Map<String, Palette<?>> BY_ID = new ConcurrentHashMap<>();

    private DeclaredPalettes() {
    }

    @Override
    public Optional<Record> palette(final String id) {
        Palette<?> found = BY_ID.get(id);
        if (found == null) {
            // A palette is declared when its class loads; the styles may be read before anything drew with it. The
            // holders are the player's game's classes, so only there are they all loaded.
            if (FMLEnvironment.dist.isClient()) {
                PaletteHolders.loadAll();
            }
            for (final Palette<?> palette : Palettes.all()) {
                BY_ID.putIfAbsent(palette.id(), palette);
            }
            found = BY_ID.get(id);
        }
        return found == null ? Optional.empty() : Optional.of(found.get());
    }
}
