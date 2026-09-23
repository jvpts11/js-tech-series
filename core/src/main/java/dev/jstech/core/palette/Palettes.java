/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.palette;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Every palette the series and its addons declare, by id.
 *
 * <p>A palette is declared once, as a {@code static final} of a class marked {@link PaletteHolder}, with the colours
 * it has in the code. Its id says whose it is and what it colours, such as {@code jsc:desktop/frames_xp}: the
 * namespace is the mod, and the path is where its file goes under {@code palettes/}. The generator writes that file
 * from the declared colours, and a resource pack can put its own in its place.
 */
public final class Palettes {

    private static final Map<ResourceLocation, Palette<?>> BY_ID = new ConcurrentHashMap<>();

    private Palettes() {
    }

    /**
     * Declares a palette with the colours it has in the code.
     *
     * @throws IllegalArgumentException when the record holds anything but colours
     * @throws IllegalStateException    when another palette already has that id, which would leave one file to
     *                                  speak for two
     */
    public static <P extends Record> Palette<P> declare(final ResourceLocation id, final P colours) {
        PaletteRoles.roles(colours.getClass());
        final Palette<P> palette = new Palette<>(id, colours);
        if (BY_ID.putIfAbsent(id, palette) != null) {
            throw new IllegalStateException("the palette " + id + " is declared twice");
        }
        return palette;
    }

    /** Every declared palette. */
    public static Collection<Palette<?>> all() {
        return List.copyOf(BY_ID.values());
    }

    /** The palettes of one mod, the ones whose id is in its namespace. */
    public static List<Palette<?>> of(final String modid) {
        final List<Palette<?>> out = new ArrayList<>();
        for (final Palette<?> palette : BY_ID.values()) {
            if (palette.id().getNamespace().equals(modid)) {
                out.add(palette);
            }
        }
        return out;
    }

    /** Gives a palette the colours a pack holds for it, over its declared ones. */
    public static void load(final Palette<?> palette, final Map<String, Integer> overrides) {
        palette.load(overrides);
    }

    /** Puts a palette back to its declared colours. */
    public static void reset(final Palette<?> palette) {
        palette.reset();
    }
}
