/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words every manual prints whatever it holds: its front matter, its index, and the parts of every entry. */
@TextHolder
public final class GuideTexts {

    public static final TextKey CONTENTS = TextKey.of("jscore.guide.contents", "Contents");
    public static final TextKey ABOUT = TextKey.of("jscore.guide.about", "About this manual");
    public static final TextKey INDEX = TextKey.of("jscore.guide.index", "Index");
    public static final TextKey FIGURE = TextKey.of("jscore.guide.figure", "Figure %s.");
    public static final TextKey TABLE = TextKey.of("jscore.guide.table", "Table %s.");
    public static final TextKey PROPERTY = TextKey.of("jscore.guide.property", "Property");
    public static final TextKey VALUE = TextKey.of("jscore.guide.value", "Value");
    public static final TextKey WARNING = TextKey.of("jscore.guide.warning", "Warning");
    public static final TextKey SEE = TextKey.of("jscore.guide.see", "See");
    public static final TextKey AND = TextKey.of("jscore.guide.and", "and");
    public static final TextKey NO_RECIPES = TextKey.of("jscore.guide.no_recipes",
            "No recipe of this kind is loaded in this world.");

    /** The five parts every entry of the series follows, in this order. */
    public static final TextKey WHAT_IT_IS = TextKey.of("jscore.guide.what_it_is", "What it is");
    public static final TextKey WHAT_IT_IS_FOR = TextKey.of("jscore.guide.what_it_is_for", "What it is for");
    public static final TextKey HOW_TO_GET_IT = TextKey.of("jscore.guide.how_to_get_it", "How to get it");
    public static final TextKey HOW_TO_USE_IT = TextKey.of("jscore.guide.how_to_use_it", "How to use it");
    public static final TextKey WHAT_CAN_GO_WRONG = TextKey.of("jscore.guide.what_can_go_wrong",
            "What can go wrong");

    /** The search on the index. */
    public static final TextKey SEARCH = TextKey.of("jscore.guide.search", "Search the index");
    public static final TextKey FOUND = TextKey.of("jscore.guide.found", "%s found");
    public static final TextKey FOUND_NONE = TextKey.of("jscore.guide.found_none", "Nothing by that name");

    /** The key that opens an item's page, and the line its tooltip carries. */
    public static final TextKey OPEN_KEY = TextKey.of("key.jscore.open_in_manual", "Open Its Page in the Manual");
    public static final TextKey HOLD_TO_OPEN = TextKey.of("jscore.guide.hold_to_open",
            "Hold [%s] to open its page in the manual");

    /** The search button at the top of the binder, for a reader who points at it. */
    public static final TextKey TO_SEARCH = TextKey.of("jscore.guide.to_search", "Search");

    private GuideTexts() {
    }
}
