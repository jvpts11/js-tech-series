/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the terminal says round interac's full-screen view: the line at the foot and the help page. The command's
 * name and the keys' names, F1 or Tab, are data. Kept apart from the keys so the language generator can read it on
 * a server too, where terminals do not exist.
 */
@TextHolder
final class InteracTuiTexts {

    // The line at the foot.
    static final TextKey PUTS_THIS_AWAY = TextKey.of("jsc.interac_tui.puts_this_away", "Any key puts this away");
    static final TextKey ANSWER_A_NUMBER = TextKey.of("jsc.interac_tui.answer_a_number",
            "Answer with a number, Enter to go on, Escape to leave it");
    /* The command, the heading it is on, and the two keys that matter most. */
    static final TextKey STATUS = TextKey.of("jsc.interac_tui.status",
            "%s  %s   %s headings   type to search   %s quit");
    static final TextKey NO_NETWORK =
            TextKey.of("jsc.interac_tui.no_network", "%s: this machine cannot reach a network");

    // The help page: the keys that move, then the numbered ones and what each does.
    static final TextKey ARROWS = TextKey.of("jsc.interac_tui.help.arrows", "Arrows");
    static final TextKey ARROWS_DO = TextKey.of("jsc.interac_tui.help.arrows_do",
            "move through the list, Page Up and Page Down by a screen");
    static final TextKey TAB = TextKey.of("jsc.interac_tui.help.tab", "Tab");
    static final TextKey TAB_DOES =
            TextKey.of("jsc.interac_tui.help.tab_does", "the next heading, Shift with it the one before");
    static final TextKey LETTERS = TextKey.of("jsc.interac_tui.help.letters", "Letters");
    static final TextKey LETTERS_DO =
            TextKey.of("jsc.interac_tui.help.letters_do", "look for something; Backspace takes a letter off");
    static final TextKey ENTER = TextKey.of("jsc.interac_tui.help.enter", "Enter");
    static final TextKey ENTER_DOES = TextKey.of("jsc.interac_tui.help.enter_does", "the same as Get");
    static final TextKey HELP = TextKey.of("jsc.interac_tui.help.help", "Help");
    static final TextKey HELP_DOES = TextKey.of("jsc.interac_tui.help.help_does", "this page");
    static final TextKey GET = TextKey.of("jsc.interac_tui.help.get", "Get");
    static final TextKey GET_DOES = TextKey.of("jsc.interac_tui.help.get_does", "into your hands");
    static final TextKey PUT = TextKey.of("jsc.interac_tui.help.put", "Put");
    static final TextKey PUT_DOES = TextKey.of("jsc.interac_tui.help.put_does", "out of your hands");
    static final TextKey CRAFT = TextKey.of("jsc.interac_tui.help.craft", "Craft");
    static final TextKey CRAFT_DOES = TextKey.of("jsc.interac_tui.help.craft_does", "ask for some made");
    static final TextKey LOCK = TextKey.of("jsc.interac_tui.help.lock", "Lock");
    static final TextKey LOCK_DOES = TextKey.of("jsc.interac_tui.help.lock_does", "hold some back");
    static final TextKey FREE = TextKey.of("jsc.interac_tui.help.free", "Free");
    static final TextKey FREE_DOES = TextKey.of("jsc.interac_tui.help.free_does", "let a held item go");
    static final TextKey FAV = TextKey.of("jsc.interac_tui.help.fav", "Fav");
    static final TextKey FAV_DOES = TextKey.of("jsc.interac_tui.help.fav_does", "star it, or take the star off");
    static final TextKey STOP = TextKey.of("jsc.interac_tui.help.stop", "Stop");
    static final TextKey STOP_DOES = TextKey.of("jsc.interac_tui.help.stop_does", "call off an operation");
    static final TextKey FIND = TextKey.of("jsc.interac_tui.help.find", "Find");
    static final TextKey FIND_DOES = TextKey.of("jsc.interac_tui.help.find_does", "start the search again");
    static final TextKey QUIT = TextKey.of("jsc.interac_tui.help.quit", "Quit");
    static final TextKey QUIT_DOES = TextKey.of("jsc.interac_tui.help.quit_does", "give the terminal back");
    static final TextKey PUTS_THE_PAGE_AWAY =
            TextKey.of("jsc.interac_tui.help.puts_the_page_away", "Any key puts this page away.");

    private InteracTuiTexts() {
    }
}
