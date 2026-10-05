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

    /** The part number at the foot of a binder's cover. */
    public static final TextKey PART_NUMBER = TextKey.of("jscore.guide.part_number", "Part No. %s");

    /** A chapter's opening pages, and the page left blank so a chapter opens on a left page. */
    public static final TextKey CHAPTER = TextKey.of("jscore.guide.chapter_word", "Chapter");
    public static final TextKey IN_THIS_CHAPTER = TextKey.of("jscore.guide.in_this_chapter", "In this chapter");
    public static final TextKey BLANK = TextKey.of("jscore.guide.blank", "This page is intentionally left blank.");

    /** A set of drawings: its list, and the title block in the corner of every sheet. */
    public static final TextKey DRAWING_LIST = TextKey.of("jscore.guide.drawing_list", "Drawing list");
    public static final TextKey DRAWING = TextKey.of("jscore.guide.drawing", "Dwg");
    public static final TextKey TITLE = TextKey.of("jscore.guide.title", "Title");
    public static final TextKey SHEETS = TextKey.of("jscore.guide.sheets", "Sheets");
    public static final TextKey SHEET = TextKey.of("jscore.guide.sheet", "Sheet");
    public static final TextKey SHEET_OF = TextKey.of("jscore.guide.sheet_of", "%s of %s");
    public static final TextKey REVISION = TextKey.of("jscore.guide.revision", "Rev");

    /** A block seen from three sides, and the size of one block drawn under it. */
    public static final TextKey VIEW_TOP = TextKey.of("jscore.guide.view_top", "Top");
    public static final TextKey VIEW_FRONT = TextKey.of("jscore.guide.view_front", "Front");
    public static final TextKey VIEW_SIDE = TextKey.of("jscore.guide.view_side", "Side");
    public static final TextKey ONE_BLOCK = TextKey.of("jscore.guide.one_block", "1 block");

    private GuideTexts() {
    }
}
