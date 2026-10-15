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

/**
 * The words a player reads of a wire's ends: which end, and how it is set, as a wrench says it when it sets one, and
 * the words of an end's screen.
 */
@TextHolder
public final class EndTexts {

    public static final TextKey END_SET = TextKey.of("jscore.cable.end.set", "%s end: %s");
    public static final TextKey NORTH = TextKey.of("jscore.cable.end.north", "North");
    public static final TextKey SOUTH = TextKey.of("jscore.cable.end.south", "South");
    public static final TextKey EAST = TextKey.of("jscore.cable.end.east", "East");
    public static final TextKey WEST = TextKey.of("jscore.cable.end.west", "West");
    public static final TextKey UP = TextKey.of("jscore.cable.end.up", "Top");
    public static final TextKey DOWN = TextKey.of("jscore.cable.end.down", "Bottom");

    /** An end's screen: its title, as "Copper Pipe, east end", and the face in the middle of a sentence. */
    public static final TextKey TITLE = TextKey.of("jscore.cable.end.title", "%s, %s end");
    public static final TextKey NORTH_INLINE = TextKey.of("jscore.cable.end.north.inline", "north");
    public static final TextKey SOUTH_INLINE = TextKey.of("jscore.cable.end.south.inline", "south");
    public static final TextKey EAST_INLINE = TextKey.of("jscore.cable.end.east.inline", "east");
    public static final TextKey WEST_INLINE = TextKey.of("jscore.cable.end.west.inline", "west");
    public static final TextKey UP_INLINE = TextKey.of("jscore.cable.end.up.inline", "top");
    public static final TextKey DOWN_INLINE = TextKey.of("jscore.cable.end.down.inline", "bottom");

    /** The rows' words. */
    public static final TextKey MODE = TextKey.of("jscore.cable.end.mode", "Mode");
    public static final TextKey PRIORITY = TextKey.of("jscore.cable.end.priority", "Priority");
    public static final TextKey RATE = TextKey.of("jscore.cable.end.rate", "Rate");
    public static final TextKey CHANNEL = TextKey.of("jscore.cable.end.channel", "Channel");
    public static final TextKey FILTER = TextKey.of("jscore.cable.end.filter", "Filter");
    public static final TextKey ONLY = TextKey.of("jscore.cable.end.only", "Only");
    public static final TextKey ALL_BUT = TextKey.of("jscore.cable.end.all_but", "All but");
    public static final TextKey UNIT_FLUID = TextKey.of("jscore.cable.end.unit.fluid", "mB/t");
    public static final TextKey UNIT_ITEMS = TextKey.of("jscore.cable.end.unit.items", "items/t");
    public static final TextKey STEPS = TextKey.of("jscore.cable.end.steps", "Shift: ten at a time");
    public static final TextKey CHANNEL_TIP = TextKey.of("jscore.cable.end.channel.tip",
            "Channel %s: %s. Ends meet only ends of their channel.");

    /** The line under the filter, saying what the end does. */
    public static final TextKey HINT_OFF = TextKey.of("jscore.cable.end.hint.off",
            "Off: this end joins and plugs into nothing.");
    public static final TextKey HINT_KEEPS_FLUID = TextKey.of("jscore.cable.end.hint.keeps_fluid",
            "Keeps %s mB of %s in the block, no more.");
    public static final TextKey HINT_KEEPS_ITEMS = TextKey.of("jscore.cable.end.hint.keeps_items",
            "Keeps %s %s in the block, no more.");
    public static final TextKey HINT_ALL_BUT = TextKey.of("jscore.cable.end.hint.all_but",
            "All but: what the filter names stays out.");
    public static final TextKey HINT_FREE = TextKey.of("jscore.cable.end.hint.free",
            "Rate 0: as fast as the line carries.");
    public static final TextKey HINT_RATE_FLUID = TextKey.of("jscore.cable.end.hint.rate_fluid",
            "At most %s mB a tick through this end.");
    public static final TextKey HINT_RATE_ITEMS = TextKey.of("jscore.cable.end.hint.rate_items",
            "At most %s items a tick through this end.");

    /** A filter slot's tooltip. */
    public static final TextKey GHOST_EMPTY = TextKey.of("jscore.cable.end.ghost.empty",
            "Click with a stack to filter what it holds");
    public static final TextKey GHOST_TAG = TextKey.of("jscore.cable.end.ghost.tag", "Anything tagged %s");
    public static final TextKey GHOST_MOD = TextKey.of("jscore.cable.end.ghost.mod", "Anything from %s");
    public static final TextKey GHOST_KEEP_FLUID = TextKey.of("jscore.cable.end.ghost.keep_fluid",
            "Keep %s mB in the block");
    public static final TextKey GHOST_KEEP_ITEMS = TextKey.of("jscore.cable.end.ghost.keep_items",
            "Keep %s in the block");
    public static final TextKey GHOST_NO_KEEP = TextKey.of("jscore.cable.end.ghost.no_keep", "Keep: no limit");
    public static final TextKey GHOST_USE = TextKey.of("jscore.cable.end.ghost.use",
            "Click with a stack to set it, with nothing to clear it");
    public static final TextKey GHOST_CYCLE = TextKey.of("jscore.cable.end.ghost.cycle",
            "Right-click: the item, its tags, its mod");
    public static final TextKey GHOST_SCROLL = TextKey.of("jscore.cable.end.ghost.scroll",
            "Scroll: how much to keep, Shift for more");
    public static final TextKey NOT_CARRIED = TextKey.of("jscore.cable.end.not_carried",
            "This line does not carry what that holds");

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

    /** The same word in the middle of a sentence. */
    public static TextKey faceInline(final Direction face) {
        return switch (face) {
            case NORTH -> NORTH_INLINE;
            case SOUTH -> SOUTH_INLINE;
            case EAST -> EAST_INLINE;
            case WEST -> WEST_INLINE;
            case UP -> UP_INLINE;
            case DOWN -> DOWN_INLINE;
        };
    }

    /** The line saying how the end that faces {@code face} is set: "East end: Push". */
    public static Text set(final Direction face, final CableEnd end) {
        return END_SET.with(face(face).text(), end.mode().text().text());
    }
}
