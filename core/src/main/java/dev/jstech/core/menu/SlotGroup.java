/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

/**
 * A run of a menu's slots added together (a machine's input, its output, the player's inventory), from
 * {@code start} up to but not including {@code end}. Stacks shift-clicked into the player's own slots fill them from
 * the end, as the game's containers do.
 *
 * @param start      the first slot's index in the menu
 * @param end        one past the last slot's index
 * @param playerSide whether the slots are the player's own inventory
 */
public record SlotGroup(int start, int end, boolean playerSide) {

    public boolean contains(final int index) {
        return index >= start && index < end;
    }
}
