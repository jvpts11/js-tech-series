/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartTilesTest {

    @Test
    void defaults_comeWithTheEverydayProgramsAndTheLiveOnesWide() {
        final StartTiles tiles = StartTiles.defaults();
        assertFalse(tiles.tiles().isEmpty());
        assertEquals(StartTiles.Size.WIDE, tiles.tiles().get(tiles.indexOf("system_monitor")).size());
        assertEquals(StartTiles.Size.WIDE, tiles.tiles().get(tiles.indexOf("network_manager")).size());
        assertTrue(tiles.indexOf("files") >= 0);
    }

    @Test
    void parse_readsTheProgramAndTheSizeLetter() {
        assertEquals(new StartTiles.Tile("calculator", StartTiles.Size.SMALL), StartTiles.parse("calculator:s"));
        assertEquals(new StartTiles.Tile("calculator", StartTiles.Size.WIDE), StartTiles.parse("jsc:calculator:W"));
        assertEquals(new StartTiles.Tile("calculator", StartTiles.Size.MEDIUM), StartTiles.parse("calculator"));
    }

    @Test
    void parse_refusesWhatNamesNoTile() {
        assertNull(StartTiles.parse(null));
        assertNull(StartTiles.parse(":m"));
        assertNull(StartTiles.parse("calculator:x"));
    }

    @Test
    void encoded_readsBackThroughSetEncoded() {
        final StartTiles tiles = StartTiles.defaults();
        final StartTiles copy = StartTiles.defaults();
        tiles.place("paint", StartTiles.Size.WIDE, 0);
        copy.setEncoded(tiles.encoded());
        assertEquals(tiles.tiles(), copy.tiles());
    }

    @Test
    void setEncoded_dropsASecondTileOfOneProgramAndWhatIsPastTheCap() {
        final StartTiles tiles = StartTiles.defaults();
        final List<String> many = new ArrayList<>();
        many.add("files:m");
        many.add("files:s");
        for (int i = 0; i < StartTiles.MAX + 5; i++) {
            many.add("program_" + i + ":s");
        }
        tiles.setEncoded(many);
        assertEquals(StartTiles.MAX, tiles.tiles().size());
        assertEquals(StartTiles.Size.MEDIUM, tiles.tiles().get(0).size());
    }

    @Test
    void place_pinsMovesAndResizes() {
        final StartTiles tiles = StartTiles.defaults();
        tiles.setEncoded(List.of("files:m", "calculator:m"));
        assertTrue(tiles.place("paint", StartTiles.Size.SMALL, 1));
        assertEquals(List.of("files:m", "paint:s", "calculator:m"), tiles.encoded());
        assertTrue(tiles.place("files", StartTiles.Size.WIDE, 99));
        assertEquals(List.of("paint:s", "calculator:m", "files:w"), tiles.encoded());
    }

    @Test
    void place_refusesANewTileOnAFullStartButStillMovesAnOldOne() {
        final StartTiles tiles = StartTiles.defaults();
        final List<String> full = new ArrayList<>();
        for (int i = 0; i < StartTiles.MAX; i++) {
            full.add("program_" + i + ":s");
        }
        tiles.setEncoded(full);
        assertFalse(tiles.place("paint", StartTiles.Size.SMALL, 0));
        assertTrue(tiles.place("program_5", StartTiles.Size.MEDIUM, 0));
        assertEquals("program_5:m", tiles.encoded().get(0));
    }

    @Test
    void remove_takesTheTileOffOnlyWhenThereIsOne() {
        final StartTiles tiles = StartTiles.defaults();
        assertTrue(tiles.remove("jsc:files"));
        assertEquals(-1, tiles.indexOf("files"));
        assertFalse(tiles.remove("files"));
    }

    @Test
    void size_byLetterReadsEveryLetterBackAndRefusesOthers() {
        for (final StartTiles.Size size : StartTiles.Size.values()) {
            assertEquals(size, StartTiles.Size.byLetter(size.letter()));
        }
        assertNull(StartTiles.Size.byLetter("q"));
        assertNull(StartTiles.Size.byLetter(null));
    }
}
