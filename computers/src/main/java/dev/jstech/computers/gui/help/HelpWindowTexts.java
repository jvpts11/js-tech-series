/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the help windows on the desktops, form by form, around the manuals' own. */
@TextHolder
public final class HelpWindowTexts {

    /** Frames 95's Help Topics, and the topic window it opens. */
    public static final TextKey TOPICS_TITLE = TextKey.of("jsc.help_window.95.title", "Help Topics: Frames Help");
    public static final TextKey TOPIC_TITLE = TextKey.of("jsc.help_window.95.topic_title", "Frames Help");
    public static final TextKey CONTENTS_TAB = TextKey.of("jsc.help_window.95.contents", "Contents");
    public static final TextKey INDEX_TAB = TextKey.of("jsc.help_window.95.index", "Index");
    public static final TextKey FIND_TAB = TextKey.of("jsc.help_window.95.find", "Find");
    public static final TextKey HINT_CONTENTS = TextKey.of("jsc.help_window.95.hint_contents",
            "Click a book, and then click Open. Or click another tab, such as Index.");
    public static final TextKey HINT_INDEX = TextKey.of("jsc.help_window.95.hint_index",
            "Type the first few letters of the word you are looking for.");
    public static final TextKey HINT_FIND = TextKey.of("jsc.help_window.95.hint_find",
            "Type the words you want to find, then click a topic and Display.");
    public static final TextKey DISPLAY = TextKey.of("jsc.help_window.95.display", "Display");
    public static final TextKey OPEN = TextKey.of("jsc.help_window.95.open", "Open");
    public static final TextKey CLOSE = TextKey.of("jsc.help_window.95.close", "Close");
    public static final TextKey PRINT = TextKey.of("jsc.help_window.95.print", "Print...");
    public static final TextKey CANCEL = TextKey.of("jsc.help_window.95.cancel", "Cancel");
    public static final TextKey HELP_TOPICS = TextKey.of("jsc.help_window.95.help_topics", "Help Topics");
    public static final TextKey BACK = TextKey.of("jsc.help_window.back", "Back");
    public static final TextKey OPTIONS = TextKey.of("jsc.help_window.options", "Options");

    /** Frames XP's Help and Support Center. */
    public static final TextKey XP_TITLE = TextKey.of("jsc.help_window.xp.title", "Help and Support Center");
    public static final TextKey XP_PANE = TextKey.of("jsc.help_window.xp.pane", "Technical Reference");
    public static final TextKey SEARCH = TextKey.of("jsc.help_window.search", "Search");
    public static final TextKey HOME = TextKey.of("jsc.help_window.home", "Home");
    public static final TextKey INDEX = TextKey.of("jsc.help_window.index", "Index");
    public static final TextKey FAVORITES = TextKey.of("jsc.help_window.favorites", "Favorites");
    public static final TextKey HISTORY = TextKey.of("jsc.help_window.history", "History");
    public static final TextKey ADD_FAVORITE = TextKey.of("jsc.help_window.add_favorite",
            "Add this page to Favorites");
    public static final TextKey NO_FAVORITES = TextKey.of("jsc.help_window.no_favorites",
            "There are no favorites yet. Open a page, then add it here.");
    public static final TextKey NO_HISTORY = TextKey.of("jsc.help_window.no_history", "No page has been read yet.");

    /** Frames 7's Help and Support, and the Get Help of 10 and 11. */
    public static final TextKey SEVEN_TITLE = TextKey.of("jsc.help_window.7.title", "Frames Help and Support");
    public static final TextKey SEARCH_HELP = TextKey.of("jsc.help_window.7.search", "Search Help");
    public static final TextKey BROWSE_HELP = TextKey.of("jsc.help_window.7.browse", "Browse Help");
    public static final TextKey GET_HELP_TITLE = TextKey.of("jsc.help_window.get_help.title", "Get Help");
    public static final TextKey SEARCH_MANUALS = TextKey.of("jsc.help_window.get_help.search",
            "Search the manuals and commands");

    /** KDE's Help Center, and GNOME's and Cinnamon's Help. */
    public static final TextKey KDE_TITLE = TextKey.of("jsc.help_window.kde.title", "Help Center");
    public static final TextKey KDE_PAGE_TITLE = TextKey.of("jsc.help_window.kde.page_title", "%s - Help Center");
    public static final TextKey YELP_TITLE = TextKey.of("jsc.help_window.yelp.title", "Help");

    /** CDE's Help Viewer: its menus and its four buttons. */
    public static final TextKey CDE_TITLE = TextKey.of("jsc.help_window.cde.title", "Help Viewer");
    public static final TextKey CDE_FILE = TextKey.of("jsc.help_window.cde.file", "File");
    public static final TextKey CDE_EDIT = TextKey.of("jsc.help_window.cde.edit", "Edit");
    public static final TextKey CDE_SEARCH = TextKey.of("jsc.help_window.cde.search", "Search");
    public static final TextKey CDE_NAVIGATE = TextKey.of("jsc.help_window.cde.navigate", "Navigate");
    public static final TextKey CDE_HELP = TextKey.of("jsc.help_window.cde.help", "Help");
    public static final TextKey CDE_COPY = TextKey.of("jsc.help_window.cde.copy", "Copy");
    public static final TextKey CDE_HOME_TOPIC = TextKey.of("jsc.help_window.cde.home_topic", "Home Topic");
    public static final TextKey CDE_ABOUT = TextKey.of("jsc.help_window.cde.about", "About Help Viewer");
    public static final TextKey BACKTRACK = TextKey.of("jsc.help_window.cde.backtrack", "Backtrack");
    public static final TextKey HISTORY_DOTS = TextKey.of("jsc.help_window.cde.history", "History...");
    public static final TextKey INDEX_DOTS = TextKey.of("jsc.help_window.cde.index", "Index...");
    public static final TextKey TOP_LEVEL = TextKey.of("jsc.help_window.cde.top_level", "Top Level");

    /** What every form says of a search. */
    public static final TextKey RESULTS = TextKey.of("jsc.help_window.results", "%s results for \"%s\"");
    public static final TextKey NO_RESULTS = TextKey.of("jsc.help_window.no_results",
            "Nothing in the manuals or the commands answers to \"%s\".");

    private HelpWindowTexts() {
    }
}
