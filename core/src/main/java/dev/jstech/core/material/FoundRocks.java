/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The rocks of other mods, found by the game's common tags with no list kept by hand: every block the mods' own files
 * put in the game's tags of the stones of the Overworld and of the Nether ({@code minecraft:base_stone_overworld},
 * {@code c:stones}, the rocks ores and dripstone take the place of, {@code minecraft:base_stone_nether}). They are
 * read from the mods' jars as the game starts ({@link ModTagFiles}), before the game reads any tag, since the ores made
 * in them are registered then; a data pack's tags come too late for that.
 */
public final class FoundRocks {

    /** The tags read, each with the world its rocks are of. */
    private static final Map<String, Realm> TAGS = tags();

    private FoundRocks() {
    }

    /** Every block the mods' tags name as a rock, by id, with the world it is of; the game's own among them. */
    public static Map<ResourceLocation, Realm> inTags() {
        final Map<ResourceLocation, Realm> out = new TreeMap<>();
        TAGS.forEach((tag, realm) -> {
            for (final String member : ModTagFiles.blocks(tag)) {
                final ResourceLocation id = ResourceLocation.tryParse(member);
                if (id != null) {
                    out.putIfAbsent(id, realm);
                }
            }
        });
        return out;
    }

    private static Map<String, Realm> tags() {
        final Map<String, Realm> out = new LinkedHashMap<>();
        out.put("minecraft:base_stone_overworld", Realm.OVERWORLD);
        out.put("c:stones", Realm.OVERWORLD);
        // The rocks ores and dripstone take the place of, which mods file their natural stones under too.
        out.put("minecraft:stone_ore_replaceables", Realm.OVERWORLD);
        out.put("minecraft:deepslate_ore_replaceables", Realm.OVERWORLD);
        out.put("minecraft:dripstone_replaceable_blocks", Realm.OVERWORLD);
        out.put("minecraft:base_stone_nether", Realm.NETHER);
        return out;
    }

    /** The world a rock is of, which tells which materials have their ores in it. */
    public enum Realm {
        /** A rock of the Overworld: the materials with an ore in the game's Overworld rocks have one in it. */
        OVERWORLD,
        /** A rock of the Nether: the materials with an ore in the game's Nether rocks have one in it. */
        NETHER
    }
}
