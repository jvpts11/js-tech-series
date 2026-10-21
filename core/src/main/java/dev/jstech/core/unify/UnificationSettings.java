/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.unify;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.config.ConfigControl;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import java.util.List;

/**
 * A world's settings of unification: whether one item stands for each material's copies made by different mods, which
 * mods' items stand first, which tags and items are left out, and whether it steps aside for Almost Unified. They are
 * read as the world's data loads, so a change takes hold at the next {@code /reload}.
 */
public final class UnificationSettings {

    private static final String SECTION = "unification";

    /** Whether one item stands for each material's copies. */
    public static final ConfigKey<Boolean> ENABLED = ConfigKey.flag(SECTION + ".enabled", true)
            .comment("Whether one item stands for each material's copies made by different mods: what recipes make "
                    + "and what drops comes out as that item, and recipes that ask for a copy ask for it. Read as the "
                    + "world's data loads, so a change takes hold at the next /reload.")
            .named("Unify materials");
    /** The mods whose items stand first. */
    public static final ConfigKey<List<String>> PREFER = ConfigKey.of(SECTION + ".prefer", Codec.STRING.listOf(),
                    List.of("minecraft", JsCore.MODID))
            .comment("The mods whose item stands for a material, by their ids, first to last; a material none of "
                    + "them makes takes the first of its items by id.")
            .named("Mods preferred");
    /** Tags unified besides the common tags of the materials' forms. */
    public static final ConfigKey<List<String>> EXTRA_TAGS = ConfigKey.of(SECTION + ".extra_tags",
                    Codec.STRING.listOf(), List.of())
            .comment("Item tags unified besides the common tags of every form of every material (c:ingots/tin, "
                    + "c:dusts/tin and the rest; never the ores, which differ by the rock they are in), written as "
                    + "c:gems/ruby.")
            .named("More tags");
    /** Tags left as they are. */
    public static final ConfigKey<List<String>> SKIPPED_TAGS = ConfigKey.of(SECTION + ".skipped_tags",
                    Codec.STRING.listOf(), List.of())
            .comment("Item tags left as they are, written as c:ingots/tin.")
            .named("Tags left alone");
    /** Items never replaced. */
    public static final ConfigKey<List<String>> KEPT_ITEMS = ConfigKey.ids(SECTION + ".kept_items", List.of(),
                    ConfigControl.ITEMS)
            .comment("Items never replaced by another, though another stands for their material.")
            .named("Items kept");
    /** Recipes left as they are. */
    public static final ConfigKey<List<String>> SKIPPED_RECIPES = ConfigKey.of(SECTION + ".skipped_recipes",
                    Codec.STRING.listOf(), List.of())
            .comment("Recipes left as they are, by id (mymod:tin_from_ore) or all of a mod's (mymod:*).")
            .named("Recipes left alone");
    /** Whether what drops is unified too. */
    public static final ConfigKey<Boolean> DROPS = ConfigKey.flag(SECTION + ".drops", true)
            .comment("Whether what blocks, creatures and chests drop comes out as the item that stands for it, as "
                    + "what recipes make does.")
            .named("Unify drops");
    /** Whether unification is left to Almost Unified. */
    public static final ConfigKey<Boolean> STEP_ASIDE = ConfigKey.flag(SECTION + ".step_aside", true)
            .comment("Whether the Core leaves unification to Almost Unified when it is installed. Off unifies with "
                    + "both, and the two may then disagree on which item stands for a material.")
            .named("Leave unification to Almost Unified");

    /** The world's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-unification", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .comment("Which item stands for a material made by several mods, in recipes and in what drops.")
            .sectionComment(SECTION, "One item for each material's copies, and what is left out.")
            .sectionNamed(SECTION, "Unification")
            .key(ENABLED)
            .key(PREFER)
            .key(EXTRA_TAGS)
            .key(SKIPPED_TAGS)
            .key(KEPT_ITEMS)
            .key(SKIPPED_RECIPES)
            .key(DROPS)
            .key(STEP_ASIDE)
            .build();

    private UnificationSettings() {
    }
}
