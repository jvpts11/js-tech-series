/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Workshop's server side says: the balloon of a finished furnace and the window's state lines. */
@TextHolder
public final class WorkshopTexts {

    public static final TextKey TITLE = TextKey.of("jsc.workshop.title", "Workshop");
    public static final TextKey FURNACE_DONE = TextKey.of("jsc.workshop.furnace_done",
            "The furnace is done: %s %s are waiting in it.");
    public static final TextKey CRAFTED_ONE = TextKey.of("jsc.workshop.crafted_one", "Crafted %s");
    public static final TextKey CRAFTED_MANY = TextKey.of("jsc.workshop.crafted_many", "Crafted %s, %s times");
    public static final TextKey NO_RECIPE = TextKey.of("jsc.workshop.no_recipe", "The grid makes nothing");
    public static final TextKey ENCHANTED = TextKey.of("jsc.workshop.enchanted", "Enchanted for %s levels");
    public static final TextKey CANNOT_ENCHANT = TextKey.of("jsc.workshop.cannot_enchant",
            "Not enough levels for that offer");
    public static final TextKey TOOK = TextKey.of("jsc.workshop.took", "Took %s for %s levels");
    public static final TextKey CANNOT_TAKE = TextKey.of("jsc.workshop.cannot_take",
            "Nothing to take, or not enough levels");

    private WorkshopTexts() {
    }
}
