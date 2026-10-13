/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextKey;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The MC-DOS Shell's menu bar: File, Options, View, Tree and Help, each with its items in the shell's own order, the
 * key that does the same beside an item, and the line the bottom of the screen says while the item is picked. Items
 * the shell had that this machine has nothing behind are kept, greyed, as the real one greyed what it could not do.
 */
final class DosShellMenus {

    /** The menus, left to right. */
    static final List<Menu> MENUS = List.of(
            new Menu(DosShellTexts.FILE, List.of(
                    item(DosShellTexts.OPEN, "Enter", DosShellTexts.OPEN_HELP, Action.OPEN),
                    item(DosShellTexts.RUN, "", DosShellTexts.RUN_HELP, Action.RUN),
                    item(DosShellTexts.PRINT, "", DosShellTexts.PRINT_HELP, Action.PRINT),
                    Item.RULE,
                    item(DosShellTexts.SEARCH, "", DosShellTexts.SEARCH_HELP, Action.SEARCH),
                    item(DosShellTexts.VIEW_CONTENTS, "F9", DosShellTexts.VIEW_CONTENTS_HELP, Action.VIEW_CONTENTS),
                    Item.RULE,
                    item(DosShellTexts.MOVE, "F7", DosShellTexts.MOVE_HELP, Action.MOVE),
                    item(DosShellTexts.COPY, "F8", DosShellTexts.COPY_HELP, Action.COPY),
                    item(DosShellTexts.DELETE, "Del", DosShellTexts.DELETE_HELP, Action.DELETE),
                    item(DosShellTexts.RENAME, "", DosShellTexts.RENAME_HELP, Action.RENAME),
                    item(DosShellTexts.MAKE_FOLDER, "", DosShellTexts.MAKE_FOLDER_HELP, Action.MAKE_FOLDER),
                    Item.RULE,
                    item(DosShellTexts.EXIT, "Alt+F4", DosShellTexts.EXIT_HELP, Action.EXIT))),
            new Menu(DosShellTexts.OPTIONS, List.of(
                    item(DosShellTexts.CONFIRMATION, "", DosShellTexts.CONFIRMATION_HELP, Action.CONFIRMATION),
                    item(DosShellTexts.DISPLAY_OPTIONS, "", DosShellTexts.DISPLAY_OPTIONS_HELP,
                            Action.DISPLAY_OPTIONS),
                    item(DosShellTexts.ACROSS, "", DosShellTexts.NOT_HERE, Action.NONE),
                    item(DosShellTexts.INFORMATION, "", DosShellTexts.INFORMATION_HELP, Action.INFORMATION),
                    item(DosShellTexts.SWAPPER, "", DosShellTexts.SWAPPER_HELP, Action.SWAPPER),
                    item(DosShellTexts.DISPLAY, "", DosShellTexts.NOT_HERE, Action.NONE),
                    item(DosShellTexts.COLORS, "", DosShellTexts.NOT_HERE, Action.NONE))),
            new Menu(DosShellTexts.VIEW, List.of(
                    item(DosShellTexts.SINGLE, "", DosShellTexts.SINGLE_HELP, Action.VIEW_FILES),
                    item(DosShellTexts.DUAL, "", DosShellTexts.NOT_HERE, Action.NONE),
                    item(DosShellTexts.ALL_FILES, "", DosShellTexts.NOT_HERE, Action.NONE),
                    item(DosShellTexts.BOTH, "", DosShellTexts.BOTH_HELP, Action.VIEW_BOTH),
                    item(DosShellTexts.PROGRAM_LIST, "", DosShellTexts.PROGRAM_LIST_HELP, Action.VIEW_PROGRAMS),
                    Item.RULE,
                    item(DosShellTexts.REPAINT, "Shift+F5", DosShellTexts.REPAINT_HELP, Action.REPAINT),
                    item(DosShellTexts.REFRESH, "F5", DosShellTexts.REFRESH_HELP, Action.REFRESH))),
            new Menu(DosShellTexts.TREE, List.of(
                    item(DosShellTexts.EXPAND_ONE, "+", DosShellTexts.EXPAND_ONE_HELP, Action.EXPAND_ONE),
                    item(DosShellTexts.EXPAND_BRANCH, "*", DosShellTexts.EXPAND_BRANCH_HELP, Action.EXPAND_BRANCH),
                    item(DosShellTexts.EXPAND_ALL, "Ctrl+*", DosShellTexts.EXPAND_ALL_HELP, Action.EXPAND_ALL),
                    item(DosShellTexts.COLLAPSE, "-", DosShellTexts.COLLAPSE_HELP, Action.COLLAPSE))),
            new Menu(DosShellTexts.HELP, List.of(
                    item(DosShellTexts.INDEX, "", DosShellTexts.NOT_HERE, Action.NONE),
                    item(DosShellTexts.KEYBOARD, "", DosShellTexts.KEYBOARD_HELP, Action.HELP_KEYS),
                    item(DosShellTexts.BASICS, "", DosShellTexts.NOT_HERE, Action.NONE),
                    item(DosShellTexts.COMMANDS, "", DosShellTexts.COMMANDS_HELP, Action.HELP_COMMANDS),
                    item(DosShellTexts.PROCEDURES, "", DosShellTexts.NOT_HERE, Action.NONE),
                    item(DosShellTexts.USING_HELP, "", DosShellTexts.NOT_HERE, Action.NONE),
                    Item.RULE,
                    item(DosShellTexts.ABOUT, "", DosShellTexts.ABOUT_HELP, Action.ABOUT))));

    private DosShellMenus() {
    }

    private static Item item(final TextKey label, final String key, final TextKey help, final Action action) {
        return new Item(label, key, help, action);
    }

    /** What picking an item does. */
    enum Action {
        NONE, OPEN, RUN, PRINT, SEARCH, VIEW_CONTENTS, MOVE, COPY, DELETE, RENAME, MAKE_FOLDER, EXIT,
        CONFIRMATION, DISPLAY_OPTIONS, INFORMATION, SWAPPER, VIEW_FILES, VIEW_BOTH, VIEW_PROGRAMS, REPAINT, REFRESH,
        EXPAND_ONE, EXPAND_BRANCH, EXPAND_ALL, COLLAPSE, HELP_KEYS, HELP_COMMANDS, ABOUT
    }

    /** One menu: its title in the bar and its items. */
    record Menu(TextKey title, List<Item> items) {
    }

    /**
     * One item of a menu, or the rule between groups of them when it has no label.
     *
     * @param label  what it says, or null for a rule
     * @param key    the key that does the same, a key's name and not a word, or empty
     * @param help   what the bottom line says while it is picked
     * @param action what picking it does
     */
    record Item(@Nullable TextKey label, String key, @Nullable TextKey help, Action action) {

        /** The rule between two groups of items. */
        static final Item RULE = new Item(null, "", null, Action.NONE);

        /** Whether this is a rule rather than an item. */
        boolean rule() {
            return label == null;
        }
    }
}
