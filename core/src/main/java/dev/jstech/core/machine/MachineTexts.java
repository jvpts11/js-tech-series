/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Core says about a machine's work: the line under a recipe in the recipe viewers. */
@TextHolder
public final class MachineTexts {

    /** A recipe's time, when the machine spends what it always does: "10 s". */
    public static final TextKey RECIPE_TIME = TextKey.of("jscore.recipe.time", "%s s");
    /** A recipe's time and the energy it spends each tick: "10 s, 40 FE/t". */
    public static final TextKey RECIPE_TIME_AND_ENERGY = TextKey.of("jscore.recipe.time_and_energy", "%s s, %s");
    /** A catalyst's slot: it must be in the machine, and is not used up. */
    public static final TextKey RECIPE_CATALYST = TextKey.of("jscore.recipe.catalyst", "Not used up");
    /** A byproduct's slot: how likely it comes. */
    public static final TextKey RECIPE_CHANCE = TextKey.of("jscore.recipe.chance", "%s%% chance");
    /** A line of matter a recipe takes from a tank, or makes into one: "Takes 250 mB of Hydrogen". */
    public static final TextKey RECIPE_MATTER_IN = TextKey.of("jscore.recipe.matter_in", "Takes %s mB of %s");
    public static final TextKey RECIPE_MATTER_OUT = TextKey.of("jscore.recipe.matter_out", "Makes %s mB of %s");
    /** The temperature a recipe runs between, in kelvin. */
    public static final TextKey RECIPE_TEMPERATURE = TextKey.of("jscore.recipe.temperature", "From %s K to %s K");
    public static final TextKey RECIPE_TEMPERATURE_ABOVE = TextKey.of("jscore.recipe.temperature_above",
            "At %s K or more");
    public static final TextKey RECIPE_TEMPERATURE_BELOW = TextKey.of("jscore.recipe.temperature_below",
            "At %s K or less");
    /** The pressure a recipe runs between, in kilopascals. */
    public static final TextKey RECIPE_PRESSURE = TextKey.of("jscore.recipe.pressure", "From %s kPa to %s kPa");
    public static final TextKey RECIPE_PRESSURE_ABOVE = TextKey.of("jscore.recipe.pressure_above",
            "At %s kPa or more");
    public static final TextKey RECIPE_PRESSURE_BELOW = TextKey.of("jscore.recipe.pressure_below",
            "At %s kPa or less");
    /** The knowledge a recipe needs; beside a padlock, in the locked colour, while the player's team lacks it. */
    public static final TextKey RECIPE_NEEDS = TextKey.of("jscore.recipe.needs", "Needs %s");

    private MachineTexts() {
    }
}
