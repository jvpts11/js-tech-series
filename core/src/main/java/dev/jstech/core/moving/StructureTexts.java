/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What players read of the moving structures: why one could not be made or set down, and the commands' answers. */
@TextHolder
public final class StructureTexts {

    public static final TextKey NOTHING = TextKey.of("jscore.moving.refused.nothing",
            "There are no blocks there to make a structure of");
    public static final TextKey TOO_MANY = TextKey.of("jscore.moving.refused.too_many",
            "A structure is made of at most %s blocks; that is %s");
    public static final TextKey IMMOVABLE = TextKey.of("jscore.moving.refused.immovable",
            "The %s at %s cannot be moved");
    public static final TextKey IN_YARD = TextKey.of("jscore.moving.refused.in_yard",
            "Those blocks are already a moving structure's");
    public static final TextKey YARD_FULL = TextKey.of("jscore.moving.refused.yard_full",
            "This dimension has no room left for another moving structure");
    public static final TextKey TOO_TALL = TextKey.of("jscore.moving.refused.too_tall",
            "The structure is %s blocks tall, and %s has room for %s");
    public static final TextKey TILTED = TextKey.of("jscore.moving.refused.tilted",
            "The structure leans too far to be set down on the ground; stand it upright first");
    public static final TextKey BLOCKED = TextKey.of("jscore.moving.refused.blocked",
            "The structure cannot be set down: the %s at %s is in the way");
    public static final TextKey OUT_OF_WORLD = TextKey.of("jscore.moving.refused.out_of_world",
            "The structure cannot be set down out of the world's height");
    public static final TextKey BORN_OUT_OF_WORLD = TextKey.of("jscore.moving.refused.born_out_of_world",
            "The structure would stand out of the world's height");
    public static final TextKey NOT_LOADED = TextKey.of("jscore.moving.refused.not_loaded",
            "Those blocks are not all loaded");
    public static final TextKey LAND_NOT_LOADED = TextKey.of("jscore.moving.refused.land_not_loaded",
            "The structure cannot be set down where the land is not loaded");
    public static final TextKey NOT_READY = TextKey.of("jscore.moving.refused.not_ready",
            "The structure's blocks are not loaded yet");
    public static final TextKey TRAVELLING = TextKey.of("jscore.moving.refused.travelling",
            "The structure is on its way to another dimension");
    public static final TextKey MOUNT_LOOP = TextKey.of("jscore.moving.refused.mount_loop",
            "A structure cannot be held by itself, nor by a structure it holds");
    public static final TextKey NOT_MOUNTED = TextKey.of("jscore.moving.refused.not_mounted",
            "That structure is not held by another");
    public static final TextKey NOT_ALIGNED = TextKey.of("jscore.moving.refused.not_aligned",
            "The two structures are not square to each other, so their blocks cannot be joined for good");
    public static final TextKey NO_ROOM = TextKey.of("jscore.moving.refused.no_room",
            "The two structures' blocks would overlap at %s");
    public static final TextKey TOO_WIDE = TextKey.of("jscore.moving.refused.too_wide",
            "Joined, the structure would be too big to move, or out of the world's height");
    public static final TextKey REFUSED_BY = TextKey.of("jscore.moving.refused.by_mod", "%s");
    public static final TextKey FOREIGN = TextKey.of("jscore.moving.refused.foreign",
            "Those blocks belong to a moving structure of %s; a moving structure is never made of another's blocks");
    public static final TextKey ASSEMBLED = TextKey.of("jscore.command.structure.assembled",
            "Made a moving structure of %s blocks");
    public static final TextKey DISASSEMBLED = TextKey.of("jscore.command.structure.disassembled",
            "Set the structure of %s blocks down on the ground");
    public static final TextKey MOVING = TextKey.of("jscore.command.structure.moving", "The structure is on its way");
    public static final TextKey STOPPED = TextKey.of("jscore.command.structure.stopped", "The structure has stopped");
    public static final TextKey PHYSICAL = TextKey.of("jscore.command.structure.physical",
            "The structure now moves by its physics: its weight, its engines, the wind and what it meets");
    public static final TextKey NO_RAILS = TextKey.of("jscore.command.structure.no_rails",
            "There are no rails running that way under the structure");
    public static final TextKey NOT_A_STRUCTURE = TextKey.of("jscore.command.structure.not_one",
            "That is not a moving structure");
    public static final TextKey LISTED = TextKey.of("jscore.command.structure.listed",
            "%s: %s blocks at %s, %s");
    public static final TextKey AT_REST = TextKey.of("jscore.command.structure.at_rest", "at rest");
    public static final TextKey UNDER_WAY = TextKey.of("jscore.command.structure.under_way", "moving");
    public static final TextKey NONE_LOADED = TextKey.of("jscore.command.structure.none",
            "No moving structure is loaded in this dimension");

    private StructureTexts() {
    }
}
