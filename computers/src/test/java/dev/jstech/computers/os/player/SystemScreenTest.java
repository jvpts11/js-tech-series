/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.player;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SystemScreenTest {

    @Test
    void writeLine_writesAtTheCursorAndGoesToTheNextRow() {
        final SystemScreen screen = new SystemScreen();

        screen.writeLine("Lantern");
        screen.write("up");

        assertTrue(screen.rows().get(0).startsWith("Lantern"));
        assertTrue(screen.rows().get(1).startsWith("up"));
        assertArrayEquals(new int[] {2, 1}, screen.cursorAt());
    }

    @Test
    void write_goesOnToTheNextRowAtTheEdge() {
        final SystemScreen screen = new SystemScreen();

        screen.write("x".repeat(SystemScreen.COLUMNS + 3));

        assertEquals("x".repeat(SystemScreen.COLUMNS), screen.rows().get(0));
        assertTrue(screen.rows().get(1).startsWith("xxx "));
    }

    @Test
    void writeLine_movesEveryRowUpPastTheLast() {
        final SystemScreen screen = new SystemScreen();

        for (int i = 0; i <= SystemScreen.ROWS; i++) {
            screen.writeLine("row " + i);
        }

        assertTrue(screen.rows().get(0).startsWith("row 2"), screen.rows().get(0));
        assertTrue(screen.rows().get(SystemScreen.ROWS - 2).startsWith("row " + SystemScreen.ROWS));
    }

    @Test
    void cursor_isHeldToTheScreen() {
        final SystemScreen screen = new SystemScreen();

        screen.cursor(500, -3);

        assertArrayEquals(new int[] {SystemScreen.COLUMNS - 1, 0}, screen.cursorAt());
    }

    @Test
    void colours_takeANameOrAColourAndRefuseAnythingElse() {
        final SystemScreen screen = new SystemScreen();

        assertTrue(screen.colours("#FFD27A", "#241733"));
        screen.write("a");
        assertFalse(screen.colours("lantern", "black"), "a name no colour has");

        assertEquals(0xFFD27A, screen.foregrounds()[0]);
        assertEquals(0x241733, screen.backgrounds()[0]);
        assertEquals(-1, SystemScreen.colour("#12345"));
    }

    @Test
    void restore_putsTheScreenBackAsItWasSaved() {
        final SystemScreen saved = new SystemScreen();
        saved.colours("#FFD27A", "#241733");
        saved.writeLine("Lantern OS 1.2");
        saved.write("> ");
        final SystemScreen read = new SystemScreen();

        read.restore(saved.rows(), saved.foregrounds(), saved.backgrounds(), saved.cursorAt());

        assertEquals(saved.rows(), read.rows());
        assertArrayEquals(saved.foregrounds(), read.foregrounds());
        assertArrayEquals(saved.cursorAt(), read.cursorAt());
    }

    @Test
    void clear_emptiesEveryRow() {
        final SystemScreen screen = new SystemScreen();
        screen.writeLine("gone");

        screen.clear();

        assertEquals(List.of(" ".repeat(SystemScreen.COLUMNS)), screen.rows().subList(0, 1));
        assertArrayEquals(new int[] {0, 0}, screen.cursorAt());
    }
}
