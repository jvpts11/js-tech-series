/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextScreenTest {

    private static final int INK = TextScreen.cga(TextScreen.GREY);
    private static final int GROUND = TextScreen.cga(TextScreen.BLACK);

    @Test
    void cga_isTheAdaptersSixteenOpaque() {
        assertEquals(0xFF0000AA, TextScreen.cga(TextScreen.BLUE));
        assertEquals(0xFFAA5500, TextScreen.cga(TextScreen.BROWN));
        assertEquals(0xFFFFFFFF, TextScreen.cga(TextScreen.WHITE));
    }

    @Test
    void put_writesAlongTheRowInItsColours() {
        final TextScreen screen = new TextScreen(10, 3, INK, GROUND);
        screen.put(2, 1, "File", TextScreen.cga(TextScreen.WHITE), TextScreen.cga(TextScreen.BLUE));
        assertEquals("  File    ", screen.text(1));
        assertEquals(TextScreen.cga(TextScreen.WHITE), screen.ink(2, 1));
        assertEquals(TextScreen.cga(TextScreen.BLUE), screen.ground(5, 1));
        assertEquals(GROUND, screen.ground(6, 1));
    }

    @Test
    void put_isCutAtTheEdgesRatherThanRefused() {
        final TextScreen screen = new TextScreen(5, 2, INK, GROUND);
        screen.put(-2, 0, "abcdefgh", INK, GROUND);
        screen.put(0, 5, "off the screen", INK, GROUND);
        assertEquals("cdefg", screen.text(0));
        assertEquals("     ", screen.text(1));
    }

    @Test
    void putKeepingTheGround_changesOnlyTheInk() {
        final TextScreen screen = new TextScreen(6, 1, INK, GROUND);
        screen.fill(0, 0, 6, 1, INK, TextScreen.cga(TextScreen.CYAN));
        screen.put(1, 0, "ok", TextScreen.cga(TextScreen.BLACK));
        assertEquals(TextScreen.cga(TextScreen.CYAN), screen.ground(1, 0));
        assertEquals(TextScreen.cga(TextScreen.BLACK), screen.ink(1, 0));
    }

    @Test
    void box_drawsItsCornersAndSidesInLineCharacters() {
        final TextScreen screen = new TextScreen(6, 4, INK, GROUND);
        screen.box(0, 0, 6, 4, INK, GROUND, false);
        assertEquals("┌────┐", screen.text(0));
        assertEquals("│    │", screen.text(1));
        assertEquals("└────┘", screen.text(3));
        screen.box(0, 0, 3, 2, INK, GROUND, true);
        assertEquals("╔═╗", screen.text(0).substring(0, 3));
    }

    @Test
    void shadow_dimsTwoColumnsBesideAndARowBelowKeepingTheCharacters() {
        final TextScreen screen = new TextScreen(8, 4, INK, TextScreen.cga(TextScreen.GREY));
        screen.put(0, 0, "abcdefgh", INK);
        screen.put(0, 2, "ijklmnop", INK);
        final int dim = TextScreen.cga(TextScreen.DARK_GREY);
        screen.shadow(0, 0, 4, 2, dim, GROUND);
        // The two columns right of the block, from the row under its top down to the row under its bottom.
        assertEquals(GROUND, screen.ground(4, 1));
        assertEquals(GROUND, screen.ground(5, 2));
        // The row under it, from its third column on.
        assertEquals(GROUND, screen.ground(2, 2));
        assertEquals(TextScreen.cga(TextScreen.GREY), screen.ground(1, 2));
        assertEquals('k', screen.glyph(2, 2));
        assertEquals(dim, screen.ink(2, 2));
        // Nothing above the block's second row moves.
        assertEquals(TextScreen.cga(TextScreen.GREY), screen.ground(4, 0));
    }
}
