/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.font;

import dev.jstech.core.font.CellFont;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Which characters each declared font draws, as the resource packs loaded now say.
 *
 * <p>The game draws a character a font lacks in the font it falls back to, and cannot be asked which of the two did
 * it. The grid has to know, since its own font's glyphs carry their place in the cell and the game's do not; so the
 * characters each font file lists are read back here every time the packs load ({@link CellFontReloadListener}). A
 * pack that brings its own picture and font file brings its own list with them.
 */
public final class CellFonts {

    private static final Map<ResourceLocation, BitSet> COVERED = new ConcurrentHashMap<>();

    /** Counts the loads, so a painter that remembers measurements knows when to forget them. */
    private static volatile int generation;

    private CellFonts() {
    }

    /** Whether that font has a glyph or a width for that character; nothing is covered before the packs load. */
    public static boolean covers(final CellFont font, final int codePoint) {
        final BitSet covered = COVERED.get(font.id());
        return covered != null && codePoint >= 0 && covered.get(codePoint);
    }

    /** How many characters that font covers. */
    public static int count(final CellFont font) {
        final BitSet covered = COVERED.get(font.id());
        return covered == null ? 0 : covered.cardinality();
    }

    /** Goes up by one each time the packs load. */
    public static int generation() {
        return generation;
    }

    static void load(final Map<ResourceLocation, BitSet> covered) {
        COVERED.clear();
        COVERED.putAll(covered);
        generation++;
    }
}
