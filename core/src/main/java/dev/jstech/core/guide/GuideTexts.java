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
    public static final TextKey GLOSSARY = TextKey.of("jscore.guide.glossary", "Glossary");
    public static final TextKey FIGURE = TextKey.of("jscore.guide.figure", "Figure %s.");
    public static final TextKey TABLE = TextKey.of("jscore.guide.table", "Table %s.");
    /** The caption of a table again over the part of it that runs on to the next page. */
    public static final TextKey TABLE_CONTINUED = TextKey.of("jscore.guide.table_continued", "%s (continued)");
    /** A listing of code, its button that copies it, what the button says once it has, and how it is put away. */
    public static final TextKey LISTING = TextKey.of("jscore.guide.listing", "Listing %s.");
    public static final TextKey COPY = TextKey.of("jscore.guide.copy", "Copy");
    public static final TextKey COPIED = TextKey.of("jscore.guide.copied", "Copied");
    public static final TextKey CLOSE = TextKey.of("jscore.guide.close", "Close");
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
    /** The same troubles at the end of an entry written as running text rather than in the five parts. */
    public static final TextKey IF_SOMETHING_GOES_WRONG = TextKey.of("jscore.guide.if_something_goes_wrong",
            "If something goes wrong");

    /**
     * The parts of a command's page: how it is typed, its options and its examples with what each does, the systems
     * it runs on, and the commands to read next; and what is printed in its place when no mod answers for it.
     */
    public static final TextKey HOW_TO_TYPE_IT = TextKey.of("jscore.guide.how_to_type_it", "How to type it");
    public static final TextKey OPTIONS = TextKey.of("jscore.guide.options", "Options");
    public static final TextKey OPTION = TextKey.of("jscore.guide.option", "Option");
    public static final TextKey EXAMPLES = TextKey.of("jscore.guide.examples", "Examples");
    public static final TextKey EXAMPLE = TextKey.of("jscore.guide.example", "Example");
    public static final TextKey WHAT_IT_DOES = TextKey.of("jscore.guide.what_it_does", "What it does");
    public static final TextKey RUNS_ON = TextKey.of("jscore.guide.runs_on", "Runs on %s.");
    public static final TextKey READ_NEXT = TextKey.of("jscore.guide.read_next", "Read next: %s.");
    public static final TextKey NO_COMMAND = TextKey.of("jscore.guide.no_command",
            "This page is missing: no mod answers for the command %s.");

    /** The search of the whole manual: its index and every line of its pages. */
    public static final TextKey SEARCH = TextKey.of("jscore.guide.search", "Search the manual");
    public static final TextKey FOUND = TextKey.of("jscore.guide.found", "%s found");
    public static final TextKey FOUND_NONE = TextKey.of("jscore.guide.found_none", "Nothing by that name");

    /** The key that opens an item's page, and the line its tooltip carries. */
    public static final TextKey OPEN_KEY = TextKey.of("key.jscore.open_in_manual", "Open Its Page in the Manual");
    public static final TextKey HOLD_TO_OPEN = TextKey.of("jscore.guide.hold_to_open",
            "Hold [%s] to open its page in the manual");

    /** The search button at the top of the binder, for a reader who points at it. */
    public static final TextKey TO_SEARCH = TextKey.of("jscore.guide.to_search", "Search");
    /** The buttons that go back to where the reader came from and forward again, and where each leads. */
    public static final TextKey BACK = TextKey.of("jscore.guide.back", "Back");
    public static final TextKey FORWARD = TextKey.of("jscore.guide.forward", "Forward");
    public static final TextKey LEADS_TO = TextKey.of("jscore.guide.leads_to", "%s, page %s");
    public static final TextKey NOWHERE_BACK = TextKey.of("jscore.guide.nowhere_back",
            "Nowhere yet: follow a link, a tab or the contents first");

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

    /** A manual read as text, in a help program or at a terminal: links, warnings and optional parts in words. */
    public static final TextKey SEE_ALSO = TextKey.of("jscore.guide.see_also", "See also:");
    public static final TextKey WARNING_LINE = TextKey.of("jscore.guide.warning_line", "Warning: %s");
    public static final TextKey OPTIONAL_PART = TextKey.of("jscore.guide.optional_part", "%s (optional)");
    /** The items an entry shows under its title, named when there is no room to draw them. */
    public static final TextKey ITEMS_LINE = TextKey.of("jscore.guide.items_line", "Items: %s");
    public static final TextKey ITEMS_MORE = TextKey.of("jscore.guide.items_more", "Items: %s and %s more");
    /** A link whose target another book holds, said after its words: "(in The Book of Sigma)". */
    public static final TextKey IN_BOOK = TextKey.of("jscore.guide.in_book", "in %s");

    private GuideTexts() {
    }
}
