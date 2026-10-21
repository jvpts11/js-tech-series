/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigControl;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import java.util.List;

/**
 * What the Core makes of other mods' rocks, woods and metals, so a game with many mods is not flooded: whether the
 * materials have ores in other mods' rocks, whether the kinds of variant the mods declare are made, and which mods,
 * rocks, materials and kinds are left out. Read as the game starts, since what is made is registered then; a change
 * takes hold when the game starts again.
 */
public final class VariantSettings {

    private static final String ORES = "ores_in_other_rocks";
    private static final String KINDS = "variants";

    /** Whether the materials have ores in other mods' rocks. */
    public static final ConfigKey<Boolean> ORES_IN_OTHER_ROCKS = ConfigKey.flag(ORES + ".enabled", true)
            .comment("Whether every material with an ore in the game's rocks of the Overworld or the Nether has one "
                    + "in the rocks other mods bring to the same world, found by the common tags (c:stones, "
                    + "minecraft:base_stone_overworld, minecraft:base_stone_nether). Read as the game starts.")
            .named("Ores in other mods' rocks");
    /** The mods whose rocks have no ores. */
    public static final ConfigKey<List<String>> SKIPPED_MODS = ConfigKey.of(ORES + ".skipped_mods",
                    Codec.STRING.listOf(), List.of())
            .comment("The mods, by id, whose rocks have no ores of the materials.")
            .named("Mods left out");
    /** The rocks that have no ores. */
    public static final ConfigKey<List<String>> SKIPPED_ROCKS = ConfigKey.ids(ORES + ".skipped_rocks", List.of(),
                    ConfigControl.BLOCKS)
            .comment("Rocks of other mods, by their block's id, that have no ores of the materials.")
            .named("Rocks left out");
    /** The materials that have no ores in other mods' rocks. */
    public static final ConfigKey<List<String>> SKIPPED_MATERIALS = ConfigKey.of(ORES + ".skipped_materials",
                    Codec.STRING.listOf(), List.of())
            .comment("Materials, by id, that keep their ores to the game's rocks and those a mod declared.")
            .named("Materials left out");

    /** Whether the kinds of variant the mods declare are made for the woods, stones and metals found. */
    public static final ConfigKey<Boolean> VARIANTS = ConfigKey.flag(KINDS + ".enabled", true)
            .comment("Whether the kinds of variant the installed mods declare (a frame of every wood, a casing of "
                    + "every metal) are made for every wood, stone and metal found by the common tags "
                    + "(minecraft:planks, c:stones, c:storage_blocks). Read as the game starts.")
            .named("Variants of other mods' materials");
    /** The mods whose materials have no variants. */
    public static final ConfigKey<List<String>> VARIANTS_SKIPPED_MODS = ConfigKey.of(KINDS + ".skipped_mods",
                    Codec.STRING.listOf(), List.of())
            .comment("The mods, by id, whose woods, stones and metals have no variants; minecraft for the game's own.")
            .named("Mods left out");
    /** The materials that have no variants. */
    public static final ConfigKey<List<String>> VARIANTS_SKIPPED_MATERIALS = ConfigKey.of(KINDS
                    + ".skipped_materials", Codec.STRING.listOf(), List.of())
            .comment("Woods, stones and metals, by id, that have no variants: minecraft:oak for the oak, the stone's "
                    + "block for a stone (create:limestone), c:tin for a metal.")
            .named("Materials left out");
    /** The kinds of variant that are not made. */
    public static final ConfigKey<List<String>> VARIANTS_SKIPPED_KINDS = ConfigKey.of(KINDS + ".skipped_kinds",
                    Codec.STRING.listOf(), List.of())
            .comment("Kinds of variant, by id (mymod:casing), that are not made for any material.")
            .named("Kinds left out");

    /** The file, read as the game starts. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-variants", ConfigSide.COMMON, ConfigFormats.TOML)
            .comment("What the Core makes of other mods' rocks, woods and metals.",
                    "Read as the game starts; a change takes hold when it starts again.")
            .sectionComment(ORES, "The materials' ores in the rocks other mods bring.")
            .sectionNamed(ORES, "Ores in other rocks")
            .key(ORES_IN_OTHER_ROCKS)
            .key(SKIPPED_MODS)
            .key(SKIPPED_ROCKS)
            .key(SKIPPED_MATERIALS)
            .sectionComment(KINDS, "The variants the mods declare for the woods, stones and metals found.")
            .sectionNamed(KINDS, "Variants")
            .key(VARIANTS)
            .key(VARIANTS_SKIPPED_MODS)
            .key(VARIANTS_SKIPPED_MATERIALS)
            .key(VARIANTS_SKIPPED_KINDS)
            .build();

    private VariantSettings() {
    }
}
