/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;

/**
 * A world's settings of choosing between recipes that overlap: whether a player chooses, at the crafting table and the
 * furnace, when more than one recipe fits, and whether that steps aside for Polymorph.
 */
public final class RecipeChoiceSettings {

    private static final String SECTION = "recipe_choice";

    /** Whether the player chooses between recipes that overlap. */
    public static final ConfigKey<Boolean> ENABLED = ConfigKey.flag(SECTION + ".enabled", true)
            .comment("Whether a player chooses which recipe is used when more than one fits what is in a crafting "
                    + "grid or a furnace, and the choice is kept: the player's for the grid, the furnace's for what "
                    + "it smelts.")
            .named("Choose between recipes that overlap");
    /** Whether choosing is left to Polymorph. */
    public static final ConfigKey<Boolean> STEP_ASIDE = ConfigKey.flag(SECTION + ".step_aside", true)
            .comment("Whether the Core leaves choosing between recipes to Polymorph when it is installed. Off keeps "
                    + "the Core's chooser as well, and both may then show.")
            .named("Leave choosing between recipes to Polymorph");

    /** The world's file. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-recipe-choice", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .comment("Choosing between recipes that overlap, at the crafting table and the furnace.")
            .sectionComment(SECTION, "Whether the player chooses, and whether that is left to Polymorph.")
            .sectionNamed(SECTION, "Choosing between recipes")
            .key(ENABLED)
            .key(STEP_ASIDE)
            .build();

    private RecipeChoiceSettings() {
    }
}
