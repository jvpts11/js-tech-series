/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.Direction;

/** The words a player reads of a wire's ends: which end, and how it is set, as a wrench says it when it sets one. */
@TextHolder
public final class EndTexts {

    public static final TextKey END_SET = TextKey.of("jscore.cable.end.set", "%s end: %s");
    public static final TextKey NORTH = TextKey.of("jscore.cable.end.north", "North");
    public static final TextKey SOUTH = TextKey.of("jscore.cable.end.south", "South");
    public static final TextKey EAST = TextKey.of("jscore.cable.end.east", "East");
    public static final TextKey WEST = TextKey.of("jscore.cable.end.west", "West");
    public static final TextKey UP = TextKey.of("jscore.cable.end.up", "Top");
    public static final TextKey DOWN = TextKey.of("jscore.cable.end.down", "Bottom");

    private EndTexts() {
    }

    /** The word for the end of a wire that faces {@code face}. */
    public static TextKey face(final Direction face) {
        return switch (face) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    /** The line saying how the end that faces {@code face} is set: "East end: Push". */
    public static Text set(final Direction face, final CableEnd end) {
        return END_SET.with(face(face).text(), end.mode().text().text());
    }
}
