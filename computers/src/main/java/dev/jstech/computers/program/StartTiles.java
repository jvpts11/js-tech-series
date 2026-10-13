/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * The tiles a machine keeps on Frames 10's Start menu, in the order the player arranged them, each at the size the
 * player gave it. The machine keeps them, so every monitor on it shows the same Start, and a fresh machine comes with
 * the tiles the system put there itself.
 *
 * <p>A tile is kept as {@code <program>:<size>}, the program by its id path and the size by one letter, which is how
 * the list is stored and sent; a tile whose program the machine does not have is kept but not shown, so installing
 * the program again brings it back where it was.
 */
public final class StartTiles {

    private final List<Tile> tiles = new ArrayList<>();

    /** The most tiles one Start menu keeps. */
    public static final int MAX = 24;

    /** What a fresh machine's Start carries: the everyday programs, the System Monitor and the network wide. */
    private static final List<String> DEFAULTS = List.of("files:m", "calculator:m", "system_monitor:w",
            "settings:m", "editor:s", "command_prompt:s", "paint:s", "minesweeper:s", "network_manager:w");

    private StartTiles() {
    }

    /** The tiles a fresh machine comes with. */
    public static StartTiles defaults() {
        final StartTiles out = new StartTiles();
        out.setEncoded(DEFAULTS);
        return out;
    }

    /** Puts the tiles back to what a fresh machine comes with. */
    public void restoreDefaults() {
        setEncoded(DEFAULTS);
    }

    /** The tile kept as {@code <program>:<size>}, or null for text that names none. */
    @Nullable
    public static Tile parse(final String encoded) {
        if (encoded == null) {
            return null;
        }
        final int colon = encoded.lastIndexOf(':');
        final String id = ComputerSettings.normalizeId(colon < 0 ? encoded : encoded.substring(0, colon));
        final Size size = colon < 0 ? Size.MEDIUM : Size.byLetter(encoded.substring(colon + 1));
        return id.isEmpty() || size == null ? null : new Tile(id, size);
    }

    /** The tiles in the player's order. */
    public List<Tile> tiles() {
        return Collections.unmodifiableList(tiles);
    }

    /** The tiles as they are stored and sent, one {@code <program>:<size>} each. */
    public List<String> encoded() {
        final List<String> out = new ArrayList<>(tiles.size());
        for (final Tile tile : tiles) {
            out.add(tile.encoded());
        }
        return out;
    }

    /** Replaces the tiles (on load): what names no tile is dropped, a program's second tile too, and past the cap. */
    public void setEncoded(final List<String> encoded) {
        tiles.clear();
        if (encoded == null) {
            return;
        }
        for (final String each : encoded) {
            final Tile tile = parse(each);
            if (tile != null && indexOf(tile.id()) < 0 && tiles.size() < MAX) {
                tiles.add(tile);
            }
        }
    }

    /**
     * Puts the program's tile at {@code index} in the order, at that size: pinning it when it is not on Start, moving
     * or resizing it when it is. False when the program is not named or Start is full.
     */
    public boolean place(final String program, final Size size, final int index) {
        final String id = ComputerSettings.normalizeId(program);
        if (id.isEmpty() || size == null) {
            return false;
        }
        final int at = indexOf(id);
        if (at >= 0) {
            tiles.remove(at);
        } else if (tiles.size() >= MAX) {
            return false;
        }
        tiles.add(Math.max(0, Math.min(tiles.size(), index)), new Tile(id, size));
        return true;
    }

    /** Takes the program's tile off Start; false when it had none. */
    public boolean remove(final String program) {
        final int at = indexOf(ComputerSettings.normalizeId(program));
        if (at < 0) {
            return false;
        }
        tiles.remove(at);
        return true;
    }

    /** Where the program's tile stands in the order, or -1 when it has none. */
    public int indexOf(final String id) {
        for (int i = 0; i < tiles.size(); i++) {
            if (tiles.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * How big a tile is, in the cells of the grid: a small one is one cell, a medium one two by two (four small ones
     * fill it, as Frames 10 packed them), a wide one four across and two down.
     */
    public enum Size {
        SMALL("s", 1, 1),
        MEDIUM("m", 2, 2),
        WIDE("w", 4, 2);

        private final String letter;
        private final int across;
        private final int down;

        Size(final String letter, final int across, final int down) {
            this.letter = letter;
            this.across = across;
            this.down = down;
        }

        /** The size a letter stands for, or null. */
        @Nullable
        public static Size byLetter(final String letter) {
            final String wanted = letter == null ? "" : letter.trim().toLowerCase(Locale.ROOT);
            for (final Size size : values()) {
                if (size.letter.equals(wanted)) {
                    return size;
                }
            }
            return null;
        }

        /** The letter the size is kept by. */
        public String letter() {
            return this.letter;
        }

        /** How many cells across it takes. */
        public int across() {
            return this.across;
        }

        /** How many cells down. */
        public int down() {
            return this.down;
        }
    }

    /** One tile: its program by id path, and its size. */
    public record Tile(String id, Size size) {

        /** The tile as it is kept: {@code <program>:<size>}. */
        public String encoded() {
            return this.id + ":" + this.size.letter();
        }
    }
}
