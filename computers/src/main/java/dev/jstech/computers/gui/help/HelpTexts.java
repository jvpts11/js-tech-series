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

/** The words of the help programs that run at a terminal: the DOS family's HELP, info, and man reading a manual. */
@TextHolder
public final class HelpTexts {

    /** What every viewer says of the manuals and the commands it shows beside them. */
    public static final TextKey COMMANDS = TextKey.of("jsc.help.commands", "Commands");
    public static final TextKey COMMANDS_PAGES = TextKey.of("jsc.help.commands_pages", "Commands (man pages)");
    public static final TextKey OTHER_MANUALS = TextKey.of("jsc.help.other_manuals", "Other manuals:");
    public static final TextKey CONTENTS = TextKey.of("jsc.help.contents", "Contents");
    public static final TextKey INDEX = TextKey.of("jsc.help.index", "Index");
    public static final TextKey NOTHING_YET = TextKey.of("jsc.help.nothing_yet", "Asking the machine...");
    public static final TextKey NO_RECIPES = TextKey.of("jsc.help.no_recipes",
            "No recipe of this kind is loaded in this world.");
    public static final TextKey RECIPE = TextKey.of("jsc.help.recipe", "%s -> %s");
    public static final TextKey RECIPE_COST = TextKey.of("jsc.help.recipe_cost", "%s -> %s (%s)");

    /** MC-DOS's and MC-NET's HELP, a whole screen in the sixteen colours. */
    public static final TextKey DOS_TITLE = TextKey.of("jsc.help.dos.title", "%s Help: %s");
    public static final TextKey DOS_NEXT = TextKey.of("jsc.help.dos.next", "Next");
    public static final TextKey DOS_BACK = TextKey.of("jsc.help.dos.back", "Back");
    public static final TextKey DOS_FILE = TextKey.of("jsc.help.dos.file", "File");
    public static final TextKey DOS_SEARCH = TextKey.of("jsc.help.dos.search", "Search");
    public static final TextKey DOS_EXIT = TextKey.of("jsc.help.dos.exit", "Exit");
    public static final TextKey DOS_FIND = TextKey.of("jsc.help.dos.find", "Find...");
    public static final TextKey DOS_REPEAT = TextKey.of("jsc.help.dos.repeat", "Repeat Last Find");
    public static final TextKey DOS_KEYS = TextKey.of("jsc.help.dos.keys",
            "<Alt+C=Contents> <Alt+N=Next> <Alt+B=Back>");
    public static final TextKey DOS_FIND_PROMPT = TextKey.of("jsc.help.dos.find_prompt", "Find what: %s");
    public static final TextKey DOS_NOT_FOUND = TextKey.of("jsc.help.dos.not_found", "Match not found.");

    /** info, the reader of the GNU manuals, on the Linux distributions. */
    public static final TextKey INFO_NEXT = TextKey.of("jsc.help.info.next", "Next: %s");
    public static final TextKey INFO_PREV = TextKey.of("jsc.help.info.prev", "Prev: %s");
    public static final TextKey INFO_UP = TextKey.of("jsc.help.info.up", "Up: %s");
    public static final TextKey INFO_WELCOME = TextKey.of("jsc.help.info.welcome",
            "Welcome to Info version 6.8.  Type h for help, m for menu item.");
    public static final TextKey INFO_MODE = TextKey.of("jsc.help.info.mode", "Info: (%s)%s, %s lines --%s");
    public static final TextKey INFO_TOP = TextKey.of("jsc.help.info.top", "Top");
    public static final TextKey INFO_BOTTOM = TextKey.of("jsc.help.info.bottom", "Bot");
    public static final TextKey INFO_ALL = TextKey.of("jsc.help.info.all", "All");
    public static final TextKey INFO_MENU = TextKey.of("jsc.help.info.menu", "* Menu:");
    public static final TextKey INFO_SEE = TextKey.of("jsc.help.info.see", "See also:");
    public static final TextKey INFO_DIR = TextKey.of("jsc.help.info.dir",
            "This is the top of the INFO tree. It lists the manuals of this machine and its commands.");
    public static final TextKey INFO_DIR_HINT = TextKey.of("jsc.help.info.dir_hint",
            "Type q to quit, h for the keys, or move to a line below and press Enter.");
    public static final TextKey INFO_NO_ITEM = TextKey.of("jsc.help.info.no_item", "No menu item '%s' in node '%s'.");
    public static final TextKey INFO_NO_NEXT = TextKey.of("jsc.help.info.no_next",
            "No 'Next' pointer for this node.");
    public static final TextKey INFO_NO_PREV = TextKey.of("jsc.help.info.no_prev",
            "No 'Prev' pointer for this node.");
    public static final TextKey INFO_NO_UP = TextKey.of("jsc.help.info.no_up", "No 'Up' pointer for this node.");
    public static final TextKey INFO_NO_LAST = TextKey.of("jsc.help.info.no_last", "No previous nodes.");
    public static final TextKey INFO_MENU_PROMPT = TextKey.of("jsc.help.info.menu_prompt", "Menu item: %s");
    public static final TextKey INFO_SEARCH_PROMPT = TextKey.of("jsc.help.info.search_prompt",
            "Search for string: %s");
    public static final TextKey INFO_SEARCH_FAILED = TextKey.of("jsc.help.info.search_failed",
            "Search failed: \"%s\"");
    public static final TextKey INFO_HELP_TITLE = TextKey.of("jsc.help.info.help_title", "Info keys");
    public static final TextKey INFO_HELP_KEYS = TextKey.of("jsc.help.info.help_keys", "q quits. n, p and u go to"
            + " the next node, the previous one and the one above; t goes to the top of the manual, d to the directory"
            + " of manuals and l back to the last node. Space and Backspace move a screen, the arrows a line. Tab goes"
            + " to the next link and Enter follows it. m goes to a menu item by its name, s searches the manual.");

    /** man, reading an entry of a manual the way it reads a command's page. */
    public static final TextKey MAN_NAME = TextKey.of("jsc.help.man.name", "NAME");
    public static final TextKey MAN_SEE_ALSO = TextKey.of("jsc.help.man.see_also", "SEE ALSO");
    public static final TextKey MAN_STATUS = TextKey.of("jsc.help.man.status",
            "Manual page %s(7) line %s/%s %s%% (press q to quit)");
    public static final TextKey MAN_NO_ENTRY = TextKey.of("jsc.help.man.no_entry", "No manual entry for %s");

    private HelpTexts() {
    }
}
