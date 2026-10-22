/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the studio's view of unification says, on the Recipes page. */
@TextHolder
public final class UnificationTexts {

    public static final TextKey OPEN = TextKey.of("gui.jscore.studio.unify.open", "What unification changes");
    public static final TextKey BACK = TextKey.of("gui.jscore.studio.unify.back", "Back to the recipes");
    public static final TextKey WOULD_CHANGE = TextKey.of("gui.jscore.studio.unify.would_change",
            "what unification would change");
    public static final TextKey CHANGED = TextKey.of("gui.jscore.studio.unify.changed", "what unification changed");
    public static final TextKey OFF = TextKey.of("gui.jscore.studio.unify.off",
            "Unification is off: this is what turning it on would do");
    public static final TextKey ON = TextKey.of("gui.jscore.studio.unify.on",
            "Unification is on: this is what it changed as the data loaded");
    public static final TextKey ASIDE = TextKey.of("gui.jscore.studio.unify.aside",
            "Unification steps aside for Almost Unified: this is what it would do");
    public static final TextKey TURN_ON = TextKey.of("gui.jscore.studio.unify.turn_on", "Turn it on");
    public static final TextKey TURNED_ON = TextKey.of("gui.jscore.studio.unify.turned_on",
            "Unification is on, and the world's data loaded again");
    public static final TextKey BY_TAG = TextKey.of("gui.jscore.studio.unify.by_tag",
            "By tag, the one kept and those replaced");
    public static final TextKey NOTHING = TextKey.of("gui.jscore.studio.unify.nothing",
            "No tag has more than one item: there is nothing to unify.");
    /** How many recipes name a tag's items. */
    public static final TextKey RECIPES = TextKey.of("gui.jscore.studio.unify.recipes", "%s recipes");
    public static final TextKey ONE_RECIPE = TextKey.of("gui.jscore.studio.unify.one_recipe", "1 recipe");
    /** The item kept, by id. */
    public static final TextKey KEPT = TextKey.of("gui.jscore.studio.unify.kept", "kept: %s");
    /** The items replaced, by id. */
    public static final TextKey REPLACED = TextKey.of("gui.jscore.studio.unify.replaced", "replaced: %s");
    /** The mods preferred, first to last. */
    public static final TextKey ORDER = TextKey.of("gui.jscore.studio.unify.order", "Kept by the order: %s");
    /** How many recipes the tag changes, one of them shown. */
    public static final TextKey ONE_OF = TextKey.of("gui.jscore.studio.unify.one_of",
            "One of the %s recipes, before and after");
    public static final TextKey THE_ONE = TextKey.of("gui.jscore.studio.unify.the_one",
            "The one recipe, before and after");
    public static final TextKey NO_RECIPE = TextKey.of("gui.jscore.studio.unify.no_recipe",
            "No recipe names these items");
    public static final TextKey BEFORE = TextKey.of("gui.jscore.studio.unify.before", "Before");
    public static final TextKey AFTER = TextKey.of("gui.jscore.studio.unify.after", "After");
    public static final TextKey DROPS = TextKey.of("gui.jscore.studio.unify.drops",
            "Drops: what blocks, creatures and chests drop comes out as the item kept too.");
    public static final TextKey NO_DROPS = TextKey.of("gui.jscore.studio.unify.no_drops",
            "Drops are left as they are.");
    public static final TextKey STEPS_ASIDE = TextKey.of("gui.jscore.studio.unify.steps_aside",
            "Steps aside if Almost Unified is installed.");
    public static final TextKey STEPPED_ASIDE = TextKey.of("gui.jscore.studio.unify.stepped_aside",
            "Almost Unified is installed and unifies instead.");

    private UnificationTexts() {
    }
}
