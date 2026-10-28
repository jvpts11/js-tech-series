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
    /** A figure's number written short, as a mission's documents wrote it. */
    public static final TextKey FIGURE_SHORT = TextKey.of("jscore.guide.figure_short", "Fig. %s");
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
    public static final TextKey CAUTION = TextKey.of("jscore.guide.caution", "Caution");
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

    /** A scene's strip: what the pointer and the wheel do to it, its Reset, and how many of its layers are shown. */
    public static final TextKey SCENE_HINT = TextKey.of("jscore.guide.scene_hint", "Drag to turn - wheel to zoom");
    public static final TextKey SCENE_RESET = TextKey.of("jscore.guide.scene_reset", "Reset");
    public static final TextKey SCENE_LAYER = TextKey.of("jscore.guide.scene_layer", "Layer %s of %s");
    public static final TextKey SCENE_ALL_LAYERS = TextKey.of("jscore.guide.scene_all_layers", "All layers");
    public static final TextKey SCENE_FEWER = TextKey.of("jscore.guide.scene_fewer", "Hide the top layer");
    public static final TextKey SCENE_MORE = TextKey.of("jscore.guide.scene_more", "Show the next layer");
    /** The reader's marks: the bookmarks and their list, notes in the margin, and what the contents say beside. */
    public static final TextKey BOOKMARKS = TextKey.of("jscore.guide.bookmarks", "Bookmarks");
    public static final TextKey BOOKMARKS_TIP = TextKey.of("jscore.guide.bookmarks_tip",
            "Right click a page to write a note in its margin");
    public static final TextKey NO_BOOKMARKS = TextKey.of("jscore.guide.no_bookmarks", "No bookmarks yet");
    public static final TextKey MARK_PAGE = TextKey.of("jscore.guide.mark_page", "+ Mark this page");
    public static final TextKey NOTE_EMPTY = TextKey.of("jscore.guide.note_empty", "Write a note...");
    public static final TextKey MARK_NEW = TextKey.of("jscore.guide.mark_new", "NEW");
    public static final TextKey MARK_NOT_YET = TextKey.of("jscore.guide.mark_not_yet", "not yet");
    /** A scene shown step by step: the step over its picture, its number read as text, and its timeline's words. */
    public static final TextKey SCENE_STEP = TextKey.of("jscore.guide.scene_step", "%s of %s: %s");
    public static final TextKey SCENE_STEP_NUMBER = TextKey.of("jscore.guide.scene_step_number", "Step %s");
    public static final TextKey SCENE_STEP_SHORT = TextKey.of("jscore.guide.scene_step_short", "%s/%s");
    public static final TextKey SCENE_BACK = TextKey.of("jscore.guide.scene_back", "The step before");
    public static final TextKey SCENE_PAUSE = TextKey.of("jscore.guide.scene_pause", "Pause");
    public static final TextKey SCENE_PLAY = TextKey.of("jscore.guide.scene_play", "Play");
    public static final TextKey SCENE_FORWARD = TextKey.of("jscore.guide.scene_forward", "The next step");
    /** Shows a scene's blocks in the world, in front of the reader, through the Structure Projector's ghosts. */
    public static final TextKey SCENE_IN_WORLD = TextKey.of("jscore.guide.scene_in_world", "In world");
    public static final TextKey SCENE_IN_WORLD_TIP = TextKey.of("jscore.guide.scene_in_world_tip",
            "Shows these blocks in the world in front of you, as the Structure Projector shows a multiblock");

    /** The search of the whole manual: its index and every line of its pages. */
    public static final TextKey SEARCH = TextKey.of("jscore.guide.search", "Search the manual");
    public static final TextKey FOUND = TextKey.of("jscore.guide.found", "%s found");
    public static final TextKey FOUND_NONE = TextKey.of("jscore.guide.found_none", "Nothing by that name");
    /** How many were found when recipes are among them: on pages, in recipes made and in recipes it goes into. */
    public static final TextKey FOUND_SPLIT = TextKey.of("jscore.guide.found_split",
            "%s found: pages %s, recipes %s, uses %s");
    /** What a recipe found stands under: one making what was looked for, or one it goes into. */
    public static final TextKey FOUND_RECIPE = TextKey.of("jscore.guide.found_recipe", "Recipe");
    public static final TextKey FOUND_USE = TextKey.of("jscore.guide.found_use", "Used in");

    /** The button that tears a copy of a page out of the book, and what it says of the copy. */
    public static final TextKey TEAR = TextKey.of("jscore.guide.tear", "Tear out a copy of this page");
    public static final TextKey TEAR_TIP = TextKey.of("jscore.guide.tear_tip",
            "The page on the left, as a sheet to frame or hand on; the manual keeps it");
    /** The strip under a manual read from a lectern: where the lectern is, and its two buttons. */
    public static final TextKey LECTERN_AT = TextKey.of("jscore.guide.lectern_at", "On the lectern at %s, %s, %s");
    public static final TextKey LECTERN_TAKE = TextKey.of("jscore.guide.lectern_take", "Take");
    public static final TextKey LECTERN_CLOSE = TextKey.of("jscore.guide.lectern_close", "Close");

    /** The manual as a website: the links between its pages, and a recipe written out as words. */
    public static final TextKey SITE_PREVIOUS = TextKey.of("jscore.guide.site.previous", "Previous: %s");
    public static final TextKey SITE_NEXT = TextKey.of("jscore.guide.site.next", "Next: %s");
    public static final TextKey SITE_RECIPE = TextKey.of("jscore.guide.site.recipe", "%s, made from %s");
    public static final TextKey SITE_RECIPE_BY = TextKey.of("jscore.guide.site.recipe_by", "%s, made from %s (%s)");
    /** What {@code /jstech manual site} answers. */
    public static final TextKey SITE_WRITTEN = TextKey.of("commands.jscore.manual.site.written",
            "The manual was written as a website: %s");
    public static final TextKey SITE_NO_MANUAL = TextKey.of("commands.jscore.manual.site.no_manual",
            "No manual by that name: %s");
    public static final TextKey SITE_NOT_WRITTEN = TextKey.of("commands.jscore.manual.site.not_written",
            "The website could not be written to %s");

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
    public static final TextKey CAUTION_LINE = TextKey.of("jscore.guide.caution_line", "Caution: %s");
    /** A checklist's line read as text: what to look at, then what it should be. */
    public static final TextKey CHECK_LINE = TextKey.of("jscore.guide.check_line", "%s: %s");
    public static final TextKey OPTIONAL_PART = TextKey.of("jscore.guide.optional_part", "%s (optional)");
    /** The items an entry shows under its title, named when there is no room to draw them. */
    public static final TextKey ITEMS_LINE = TextKey.of("jscore.guide.items_line", "Items: %s");
    public static final TextKey ITEMS_MORE = TextKey.of("jscore.guide.items_more", "Items: %s and %s more");
    /** A link whose target another book holds, said after its words: "(in The Book of Sigma)". */
    public static final TextKey IN_BOOK = TextKey.of("jscore.guide.in_book", "in %s");

    private GuideTexts() {
    }
}
