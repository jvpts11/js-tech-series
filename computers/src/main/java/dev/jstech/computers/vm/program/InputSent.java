/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * What came from the monitor to an operating system a player wrote (computers/docs/design/player-systems.md), as its
 * {@code OnInput} reads it in an {@code InputEvent}.
 *
 * @param kind      "key" for a key pressed, "char" for a character typed, "move" for the mouse moving, "click" for
 *                  a button pressed
 * @param key       the key's code, for a key; 0 otherwise
 * @param character the character typed, for a character; empty otherwise
 * @param x         where the mouse is across the screen, in its text columns, for a move or a click
 * @param y         where the mouse is down the screen, in its text rows, for a move or a click
 * @param button    the mouse button, 0 for the left one, for a click
 */
public record InputSent(String kind, int key, String character, int x, int y, int button) {

    /** The kinds of input there are, as {@link #kind} names them. */
    public static final String KEY = "key";
    public static final String CHAR = "char";
    public static final String MOVE = "move";
    public static final String CLICK = "click";

    public InputSent {
        kind = kind == null ? KEY : kind;
        character = character == null ? "" : character;
    }
}
