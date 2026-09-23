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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Every palette the series and its addons declare, by id.
 *
 * <p>A palette is declared once, as a {@code static final} of a class marked {@link PaletteHolder}, with the colours
 * it has in the code. The mod it belongs to and a path that says what it colours make its id, such as
 * {@code jsc:desktop/frames_xp}; the path is also where its file goes under {@code palettes/}. The generator writes
 * that file from the declared colours, and a resource pack can put its own in its place.
 */
public final class Palettes {

    private static final Map<String, Palette<?>> BY_ID = new ConcurrentHashMap<>();
    private static final AtomicInteger GENERATION = new AtomicInteger();

    private Palettes() {
    }

    /**
     * Declares a palette with the colours it has in the code.
     *
     * @param namespace the mod the palette belongs to
     * @param path      what it colours, such as {@code era/standard}: lower case, with {@code /} between the parts
     * @throws IllegalArgumentException when the record holds anything but colours
     * @throws IllegalStateException    when another palette already has that id, which would leave one file to
     *                                  speak for two
     */
    public static <P extends Record> Palette<P> declare(final String namespace, final String path, final P colours) {
        PaletteRoles.roles(colours.getClass());
        final Palette<P> palette = new Palette<>(namespace, path, colours);
        if (BY_ID.putIfAbsent(palette.id(), palette) != null) {
            throw new IllegalStateException("the palette " + palette.id() + " is declared twice");
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
            if (palette.namespace().equals(modid)) {
                out.add(palette);
            }
        }
        return out;
    }

    /**
     * Colours worked out from declared palettes, such as a skin made from a scheme's handful of colours. They are
     * worked out on the first ask and again only after a pack has changed a palette, so what reads them each time
     * it paints costs one comparison, and still never paints with colours a pack has since replaced.
     *
     * @param working how the colours are worked out; it may read any palettes, and nothing else that changes
     */
    public static <P> Supplier<P> derive(final Supplier<P> working) {
        return new DerivedPalette<>(working);
    }

    /** Gives a palette the colours a pack holds for it, over its declared ones. */
    public static void load(final Palette<?> palette, final Map<String, Integer> overrides) {
        palette.load(overrides);
        GENERATION.incrementAndGet();
    }

    /** Puts a palette back to its declared colours. */
    public static void reset(final Palette<?> palette) {
        palette.reset();
        GENERATION.incrementAndGet();
    }

    /** How many times a palette has been given colours since the game started, which is what derived ones watch. */
    static int generation() {
        return GENERATION.get();
    }
}
