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

    private MachineTexts() {
    }
}
