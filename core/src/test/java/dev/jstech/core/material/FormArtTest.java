/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * The generator draws exactly the art approved for the material items: the texels below were read from the approved
 * generator, so a port that drifts by one shade fails here.
 */
class FormArtTest {

    private static final int IRON = 0xA8A9AB;
    private static final int COPPER = 0xD9773F;
    private static final int SILICON = 0x56606F;
    private static final int DARK = 0x353735;
    /* The light of a template's lit edge, the lightest an outline is drawn in. */
    private static final double LIT_EDGE = 0.34;

    @Test
    void ramp_putsTheMaterialsOwnColourAtTheMiddleLight() {
        assertEquals(0xFFA8A9AB, FormArt.ramp(IRON, 0.6));
        assertEquals(0xFF242427, FormArt.ramp(IRON, 0.06));
        assertEquals(0xFFE8E9EA, FormArt.ramp(IRON, 1.0));
        assertEquals(0xFF7C2616, FormArt.ramp(COPPER, 0.3));
    }

    @Test
    void drawn_coloursTheCoresOwnTemplates() {
        assertEquals(0xFFF0D3A2, texel(FormArt.drawn("bolt", COPPER), 5, 2));
        assertEquals(0xFFE2AF76, texel(FormArt.drawn("bolt", COPPER), 6, 9));
        assertEquals(0xFFAEAFB1, texel(FormArt.drawn("plate", IRON), 8, 7));
    }

    @Test
    void drawn_outlinesThePlateTheSheetAndTheFoilAtBothTheirEnds() {
        final int outline = red(FormArt.ramp(IRON, LIT_EDGE));
        for (final String form : new String[] {"plate", "sheet", "foil"}) {
            final int[] drawn = FormArt.drawn(form, IRON);
            for (int y = 0; y < FormArt.SIZE; y++) {
                int first = -1;
                int last = -1;
                for (int x = 0; x < FormArt.SIZE; x++) {
                    if (texel(drawn, x, y) != 0) {
                        first = first < 0 ? x : first;
                        last = x;
                    }
                }
                if (first >= 0) {
                    assertTrue(red(texel(drawn, first, y)) <= outline, form + "'s left end is outlined in row " + y);
                    assertTrue(red(texel(drawn, last, y)) <= outline, form + "'s right end is outlined in row " + y);
                }
            }
        }
    }

    @Test
    void drawn_windsTheCoilsWireRoundASteelSpool() {
        final int[] coil = FormArt.drawn("coil", COPPER);
        assertEquals(0xFFF0D3A2, texel(coil, 5, 5));
        assertEquals(0xFF26282C, texel(coil, 1, 3));
    }

    @Test
    void drawn_givesTheWaferItsDiesItsRainbowAndItsEdge() {
        final int[] wafer = FormArt.drawn("wafer", SILICON);
        assertEquals(0xFF7B7A70, texel(wafer, 6, 6));
        assertEquals(0xFFB2C0C6, texel(wafer, 4, 4));
        assertEquals(0xFF11111C, texel(wafer, 8, 13));
    }

    @Test
    void recolour_keepsEachTexelsLightAndChangesItsColour() {
        final int[] coloured = FormArt.recolour(diagonal(), FormArt.SIZE, COPPER);
        assertEquals(0xFF852C1A, texel(coloured, 3, 4));
        assertEquals(0xFFF6DEB0, texel(coloured, 15, 15));
        assertEquals(0, texel(coloured, 0, 0), "an empty texel stays empty");
    }

    @Test
    void recolour_keepsADarkMaterialsGlintsDark() {
        assertEquals(0xFF626462, texel(FormArt.recolour(diagonal(), FormArt.SIZE, DARK), 15, 15));
    }

    @Test
    void gameTexture_drawsTheFormsTheGameHasFromItsOwn() {
        assertEquals(Optional.of("textures/item/iron_ingot.png"), FormArt.gameTexture("ingot", false));
        assertEquals(Optional.of("textures/block/emerald_block.png"), FormArt.gameTexture("block", true));
        assertEquals(Optional.of("textures/block/gold_block.png"), FormArt.gameTexture("block", false));
        assertTrue(FormArt.gameTexture("gear", false).isEmpty());
    }

    @Test
    void forms_areEachDrawnOneWayOrTheOther() {
        for (final String form : FormArt.forms()) {
            final boolean fromTheGame = FormArt.gameTexture(form, false).isPresent();
            assertTrue(fromTheGame != FormArt.drawnByTheCore(form), form + " is drawn exactly one way");
            if (!fromTheGame) {
                assertFalse(empty(FormArt.drawn(form, IRON)), form + " has texels");
            }
        }
        assertEquals(20, FormArt.forms().size());
    }

    @Test
    void drawn_refusesAFormTheCoreDoesNotDraw() {
        assertThrows(IllegalArgumentException.class, () -> FormArt.drawn("ingot", IRON));
    }

    @Test
    void specks_areTheTexelsWhereTheOreLeavesItsRock() {
        final int[] rock = flat(grey(120));
        final int[] ore = flat(grey(120));
        ore[3 * FormArt.SIZE + 4] = grey(200);
        // Nearly the rock's own grey, under what tells a speck apart.
        ore[5 * FormArt.SIZE + 5] = grey(130);
        final int[] specks = FormArt.specks(ore, FormArt.SIZE, rock, FormArt.SIZE);
        assertEquals(grey(200), texel(specks, 4, 3));
        assertEquals(0, texel(specks, 5, 5));
        assertEquals(0, texel(specks, 0, 0));
    }

    @Test
    void ore_laysTheSpecksOnTheRockInTheMaterialsColour() {
        final int[] rock = diagonal();
        final int[] specks = new int[FormArt.SIZE * FormArt.SIZE];
        specks[2 * FormArt.SIZE + 2] = grey(90);
        specks[9 * FormArt.SIZE + 7] = grey(200);
        final int[] ore = FormArt.ore(rock, FormArt.SIZE, specks, COPPER, OptionalInt.empty(), 0, 0);
        final int[] recoloured = FormArt.recolour(specks, FormArt.SIZE, COPPER);
        assertEquals(texel(recoloured, 2, 2), texel(ore, 2, 2));
        assertEquals(texel(recoloured, 7, 9), texel(ore, 7, 9));
        assertEquals(texel(rock, 5, 5), texel(ore, 5, 5), "the rock stays where there is no speck");
    }

    @Test
    void ore_givesTheSecondColourOnlyToSpecksOfItsHues() {
        final int[] specks = new int[FormArt.SIZE * FormArt.SIZE];
        // A green speck, as copper's patina, and a grey one.
        specks[2 * FormArt.SIZE + 2] = 0xFF << 24 | 60 << 16 | 170 << 8 | 90;
        specks[4 * FormArt.SIZE + 4] = grey(150);
        final int second = 0x5B5BB8;
        final int[] ore = FormArt.ore(diagonal(), FormArt.SIZE, specks, COPPER, OptionalInt.of(second), 0.25, 0.55);
        assertEquals(texel(FormArt.recolour(specks, FormArt.SIZE, second), 2, 2), texel(ore, 2, 2));
        assertEquals(texel(FormArt.recolour(specks, FormArt.SIZE, COPPER), 4, 4), texel(ore, 4, 4));
    }

    private static int red(final int texel) {
        return texel >> 16 & 0xFF;
    }

    private static int grey(final int value) {
        return 0xFF << 24 | value << 16 | value << 8 | value;
    }

    private static int[] flat(final int colour) {
        final int[] out = new int[FormArt.SIZE * FormArt.SIZE];
        Arrays.fill(out, colour);
        return out;
    }

    private static int texel(final int[] image, final int x, final int y) {
        return image[y * FormArt.SIZE + x];
    }

    private static boolean empty(final int[] image) {
        for (final int texel : image) {
            if (texel != 0) {
                return false;
            }
        }
        return true;
    }

    /* A diagonal ramp of greys, its top left texel empty: the same source the approved generator was read with. */
    private static int[] diagonal() {
        final int[] out = new int[FormArt.SIZE * FormArt.SIZE];
        for (int y = 0; y < FormArt.SIZE; y++) {
            for (int x = 0; x < FormArt.SIZE; x++) {
                if (x != 0 || y != 0) {
                    final int g = 20 + (x + y) * 7;
                    out[y * FormArt.SIZE + x] = 0xFF << 24 | g << 16 | g << 8 | g;
                }
            }
        }
        return out;
    }
}
