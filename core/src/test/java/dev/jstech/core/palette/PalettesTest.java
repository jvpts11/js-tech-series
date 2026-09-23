/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PalettesTest {

    /** Every palette here is declared under a mod of its own, so these never meet a palette of the series. */
    private static final String MOD = "palettes_test";

    @Test
    void declare_namesThePaletteByItsModAndPath() {
        final Palette<Swatch> palette = Palettes.declare(MOD, "named/swatch", new Swatch(1, 2, 3));
        assertEquals("palettes_test:named/swatch", palette.id());
        assertEquals(MOD, palette.namespace());
        assertEquals("named/swatch", palette.path());
        assertTrue(Palettes.all().contains(palette));
    }

    @Test
    void declare_theSameIdTwice_throws() {
        Palettes.declare(MOD, "twice", new Swatch(1, 2, 3));
        assertThrows(IllegalStateException.class, () -> Palettes.declare(MOD, "twice", new Swatch(4, 5, 6)));
    }

    @Test
    void declare_aRecordHoldingMoreThanColours_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Palettes.declare(MOD, "labelled", new Labelled(1, "one")));
    }

    @Test
    void get_beforeAnyPack_isTheDeclaredColours() {
        final Swatch declared = new Swatch(1, 2, 3);
        final Palette<Swatch> palette = Palettes.declare(MOD, "unloaded", declared);
        assertSame(declared, palette.get());
        assertSame(declared, palette.declared());
    }

    @Test
    void load_keepsTheDeclaredColourOfEveryRoleThePackLeavesOut() {
        final Palette<Swatch> palette = Palettes.declare(MOD, "partial", new Swatch(1, 2, 3));
        Palettes.load(palette, Map.of("accent", 20, "nothing", 99));
        assertEquals(new Swatch(1, 20, 3), palette.get());
        assertEquals(new Swatch(1, 2, 3), palette.declared());
    }

    @Test
    void reset_goesBackToTheDeclaredColours() {
        final Palette<Swatch> palette = Palettes.declare(MOD, "reset", new Swatch(1, 2, 3));
        Palettes.load(palette, Map.of("text", 10, "accent", 20, "edge", 30));
        Palettes.reset(palette);
        assertSame(palette.declared(), palette.get());
    }

    @Test
    void of_listsOnlyThatModsPalettes() {
        final Palette<Swatch> mine = Palettes.declare("palettes_test_mine", "only", new Swatch(1, 2, 3));
        Palettes.declare("palettes_test_theirs", "only", new Swatch(1, 2, 3));
        assertEquals(List.of(mine), Palettes.of("palettes_test_mine"));
    }

    private record Swatch(int text, int accent, int edge) {
    }

    private record Labelled(int colour, String name) {
    }
}
