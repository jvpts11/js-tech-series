/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LuaTerminalTest {

    private LuaTerminal screen;

    @BeforeEach
    void setUp() {
        this.screen = new LuaTerminal();
    }

    @Test
    void write_stopsAtTheEdgeAndMovesTheCursorPastIt() {
        this.screen.setCursor(49, 1);
        this.screen.write("abcd");
        assertEquals("abc", this.screen.row(0).substring(48));
        assertEquals(53, this.screen.cursorX());
    }

    @Test
    void print_wrapsWholeWordsToTheNextRow() {
        this.screen.setCursor(45, 1);
        final int rows = this.screen.print("hello world");
        assertTrue(this.screen.row(0).substring(44).startsWith("hello "));
        assertTrue(this.screen.row(1).startsWith("world"));
        assertEquals(1, rows);
    }

    @Test
    void print_breaksAWordWiderThanTheScreen() {
        this.screen.print("x".repeat(60));
        assertEquals("x".repeat(51), this.screen.row(0));
        assertEquals("x".repeat(9), this.screen.row(1).substring(0, 9));
        assertEquals(10, this.screen.cursorX());
    }

    @Test
    void print_scrollsWhenItReachesTheBottom() {
        this.screen.print("top");
        for (int i = 0; i < LuaTerminal.HEIGHT; i++) {
            this.screen.print("\n");
        }
        assertTrue(this.screen.row(0).isBlank(), "the first row scrolled away");
        assertEquals(LuaTerminal.HEIGHT, this.screen.cursorY());
    }

    @Test
    void blit_paintsEachCellItsOwnColours() {
        this.screen.setCursor(1, 1);
        this.screen.blit("ab", "0e", "f1");
        assertEquals("0e", this.screen.rowText(0).substring(0, 2));
        assertEquals("f1", this.screen.rowGround(0).substring(0, 2));
    }

    @Test
    void scroll_bringsRowsUpAndBlanksWhatItUncovers() {
        this.screen.setCursor(1, 2);
        this.screen.write("second");
        this.screen.scroll(1);
        assertTrue(this.screen.row(0).startsWith("second"));
        assertTrue(this.screen.row(LuaTerminal.HEIGHT - 1).isBlank());
        this.screen.scroll(-1);
        assertTrue(this.screen.row(1).startsWith("second"));
        assertTrue(this.screen.row(0).isBlank());
    }

    @Test
    void setRow_putsBackWhatWasWrittenDown() {
        this.screen.setCursor(1, 3);
        this.screen.blit("kept", "0123", "fedc");
        final LuaTerminal copy = new LuaTerminal();
        copy.setRow(2, this.screen.row(2), this.screen.rowText(2), this.screen.rowGround(2));
        assertEquals(this.screen.row(2), copy.row(2));
        assertEquals(this.screen.rowText(2), copy.rowText(2));
        assertEquals(this.screen.rowGround(2), copy.rowGround(2));
    }

    @Test
    void revision_movesWithEveryChange() {
        final long before = this.screen.revision();
        this.screen.write("a");
        assertTrue(this.screen.revision() > before);
    }
}
