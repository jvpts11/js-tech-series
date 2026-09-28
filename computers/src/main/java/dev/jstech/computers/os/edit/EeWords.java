/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * What FreeBSD's {@code ee} writes on the glass: its five rows of shortcuts, the row naming where the
 * caret stands, its menu and what its menu asks on the way out.
 *
 * <p>It is the editor FreeBSD gives to a newcomer, so nothing here is a secret: every key it answers to is
 * spelled out on the screen, which is the one thing that makes it {@code ee} rather than another {@code vi}.
 * Read in the player's language where it is drawn, as every word a terminal editor says here is.
 */
@TextHolder
public final class EeWords {

    /* The five rows of shortcuts, written across the top the way the real editor writes them. */
    private static final TextKey ESCAPE_MENU = TextKey.of("jsc.ee.key.escape_menu", "(escape) menu");
    private static final TextKey SEARCH_PROMPT = TextKey.of("jsc.ee.key.search_prompt", "search prompt");
    private static final TextKey DELETE_LINE = TextKey.of("jsc.ee.key.delete_line", "delete line");
    private static final TextKey PREV_LINE = TextKey.of("jsc.ee.key.prev_line", "prev li");
    private static final TextKey PREV_PAGE = TextKey.of("jsc.ee.key.prev_page", "prev page");
    private static final TextKey ASCII_CODE = TextKey.of("jsc.ee.key.ascii_code", "ascii code");
    private static final TextKey SEARCH = TextKey.of("jsc.ee.key.search", "search");
    private static final TextKey UNDELETE_LINE = TextKey.of("jsc.ee.key.undelete_line", "undelete line");
    private static final TextKey NEXT_LINE = TextKey.of("jsc.ee.key.next_line", "next li");
    private static final TextKey NEXT_PAGE = TextKey.of("jsc.ee.key.next_page", "next page");
    private static final TextKey END_OF_FILE = TextKey.of("jsc.ee.key.end_of_file", "end of file");
    private static final TextKey BEGIN_OF_LINE = TextKey.of("jsc.ee.key.begin_of_line", "begin of line");
    private static final TextKey DELETE_WORD = TextKey.of("jsc.ee.key.delete_word", "delete word");
    private static final TextKey BACK_1_CHAR = TextKey.of("jsc.ee.key.back_1_char", "back 1 char");
    private static final TextKey NEXT_WORD = TextKey.of("jsc.ee.key.next_word", "next word");
    private static final TextKey TOP_OF_TEXT = TextKey.of("jsc.ee.key.top_of_text", "top of text");
    private static final TextKey END_OF_LINE = TextKey.of("jsc.ee.key.end_of_line", "end of line");
    private static final TextKey RESTORE_WORD = TextKey.of("jsc.ee.key.restore_word", "restore word");
    private static final TextKey FORWARD_CHAR = TextKey.of("jsc.ee.key.forward_char", "forward char");
    private static final TextKey COMMAND = TextKey.of("jsc.ee.key.command", "command");
    private static final TextKey DELETE_CHAR = TextKey.of("jsc.ee.key.delete_char", "delete char");
    private static final TextKey UNDELETE_CHAR = TextKey.of("jsc.ee.key.undelete_char", "undelete char");
    private static final TextKey EXIT_TIP = TextKey.of("jsc.ee.key.exit_tip", "ESC-Enter: exit");

    /** The five rows of shortcuts, five columns wide, exactly as the real editor writes them. */
    public static final List<List<TtyLook.Key>> KEYS = List.of(
            List.of(key("^[", ESCAPE_MENU), key("^y", SEARCH_PROMPT), key("^k", DELETE_LINE),
                    key("^p", PREV_LINE), key("^g", PREV_PAGE)),
            List.of(key("^o", ASCII_CODE), key("^x", SEARCH), key("^l", UNDELETE_LINE),
                    key("^n", NEXT_LINE), key("^v", NEXT_PAGE)),
            List.of(key("^u", END_OF_FILE), key("^a", BEGIN_OF_LINE), key("^w", DELETE_WORD),
                    key("^b", BACK_1_CHAR), key("^z", NEXT_WORD)),
            List.of(key("^t", TOP_OF_TEXT), key("^e", END_OF_LINE), key("^r", RESTORE_WORD),
                    key("^f", FORWARD_CHAR), none()),
            List.of(key("^c", COMMAND), key("^d", DELETE_CHAR), key("^j", UNDELETE_CHAR), none(), tip(EXIT_TIP)));

    /* The main menu Escape opens. */
    public static final TextKey MAIN_MENU_TITLE = TextKey.of("jsc.ee.menu.title", "ee main menu");
    private static final TextKey LEAVE_EDITOR = TextKey.of("jsc.ee.menu.leave_editor", "leave editor");
    private static final TextKey HELP = TextKey.of("jsc.ee.menu.help", "help");
    private static final TextKey FILE_OPERATIONS = TextKey.of("jsc.ee.menu.file_operations", "file operations");
    private static final TextKey REDRAW_SCREEN = TextKey.of("jsc.ee.menu.redraw_screen", "redraw screen");
    private static final TextKey SETTINGS = TextKey.of("jsc.ee.menu.settings", "settings");
    private static final TextKey SEARCH_ITEM = TextKey.of("jsc.ee.menu.search", "search");

    /** The main menu's items, in the letter order the player picks them by. */
    public static final List<TextKey> MENU_ITEMS = List.of(LEAVE_EDITOR, HELP, FILE_OPERATIONS, REDRAW_SCREEN,
            SETTINGS, SEARCH_ITEM);

    /* The question "leave editor" asks. */
    private static final TextKey SAVE_CHANGES = TextKey.of("jsc.ee.menu.save_changes", "save changes");
    private static final TextKey NO_SAVE = TextKey.of("jsc.ee.menu.no_save", "no save");

    /** The two answers to the question leaving the editor asks, in the letter order the player picks them by. */
    public static final List<TextKey> LEAVE_ITEMS = List.of(SAVE_CHANGES, NO_SAVE);

    /* What "file operations" offers: the two of ee's own that this editor can really do, in ee's own order.
     * The second is named "save file" rather than ee's own "write a file", since this editor's own version
     * of it writes the open file back under its own name and never asks for another. */
    private static final TextKey FILE_OP_READ = TextKey.of("jsc.ee.menu.file_op.read", "read a file");
    private static final TextKey FILE_OP_SAVE = TextKey.of("jsc.ee.menu.file_op.save", "save file");

    /** The two answers "file operations" offers, in the letter order the player picks them by. */
    public static final List<TextKey> FILE_OP_ITEMS = List.of(FILE_OP_READ, FILE_OP_SAVE);

    /* What "settings" offers: the one of ee's own settings this editor can really honour. */
    private static final TextKey INFO_WINDOW_ON = TextKey.of("jsc.ee.menu.info_window_on", "info window: on");
    private static final TextKey INFO_WINDOW_OFF = TextKey.of("jsc.ee.menu.info_window_off", "info window: off");

    /** The two answers "settings" offers, in the letter order the player picks them by. */
    public static final List<TextKey> SETTINGS_ITEMS = List.of(INFO_WINDOW_ON, INFO_WINDOW_OFF);

    /* The page "help" opens: the title and each of the five rows of shortcuts written out two to a row, the way
     * the real editor's own help screen packs them so the whole page fits a small console. How to leave it is
     * said on the row under the page instead, which never scrolls out of view. */
    private static final TextKey HELP_TITLE = TextKey.of("jsc.ee.help.title", "ee command reference");
    private static final TextKey HELP_LINE_PAIR = TextKey.of("jsc.ee.help.line_pair", "%s  %s     %s  %s");

    /** Said on the row under the page while it is up, since a page longer than the glass can hide it otherwise. */
    public static final TextKey HELP_CONTINUE = TextKey.of("jsc.ee.help.continue", "(press any key to continue)");

    /** The page "help" shows: the same commands the five rows on top name, two to a line. */
    public static final List<Text> HELP_PAGE = List.of(
            HELP_TITLE.text(),
            Text.EMPTY,
            helpLinePair("^[", ESCAPE_MENU, "^y", SEARCH_PROMPT),
            helpLinePair("^k", DELETE_LINE, "^p", PREV_LINE),
            helpLinePair("^g", PREV_PAGE, "^o", ASCII_CODE),
            helpLinePair("^x", SEARCH, "^l", UNDELETE_LINE),
            helpLinePair("^n", NEXT_LINE, "^v", NEXT_PAGE),
            helpLinePair("^u", END_OF_FILE, "^a", BEGIN_OF_LINE),
            helpLinePair("^w", DELETE_WORD, "^b", BACK_1_CHAR),
            helpLinePair("^z", NEXT_WORD, "^t", TOP_OF_TEXT),
            helpLinePair("^e", END_OF_LINE, "^r", RESTORE_WORD),
            helpLinePair("^f", FORWARD_CHAR, "^c", COMMAND),
            helpLinePair("^d", DELETE_CHAR, "^j", UNDELETE_CHAR));

    /* What is said at the bottom: on opening a file, on writing one, and while a prompt is answered. */
    private static final TextKey NEW_FILE = TextKey.of("jsc.ee.new_file", "\"%s\" [New file]");
    private static final TextKey OPENED = TextKey.of("jsc.ee.opened", "\"%s\" %s lines, %s characters");
    private static final TextKey ASKING = TextKey.of("jsc.ee.asking", "%s: %s");
    private static final TextKey UNKNOWN_COMMAND = TextKey.of("jsc.ee.unknown_command", "Unknown command: %s");
    private static final TextKey CANNOT_READ = TextKey.of("jsc.ee.cannot_read", "\"%s\": No such file or directory");
    private static final TextKey PATTERN_NOT_FOUND = TextKey.of("jsc.ee.pattern_not_found", "Not found: %s");

    /** What a prompt asks, for {@link #asking}: a search, an ASCII number, a command line or a file to read. */
    public static final TextKey SEARCH_PROMPT_LABEL = TextKey.of("jsc.ee.search_prompt_label", "Search");
    public static final TextKey ASCII_PROMPT_LABEL = TextKey.of("jsc.ee.ascii_prompt_label", "ASCII number");
    public static final TextKey COMMAND_PROMPT_LABEL = TextKey.of("jsc.ee.command_prompt_label", "Command");
    public static final TextKey FILE_TO_READ_LABEL = TextKey.of("jsc.ee.file_to_read_label", "File to read");

    /* The row naming where the caret stands, right under the keys. */
    private static final TextKey POSITION = TextKey.of("jsc.ee.position", "=====line %s col %s lines from top %s ");

    private EeWords() {
    }

    /** After opening a file that was already there. */
    public static Text opened(final String name, final int lines, final int characters) {
        return OPENED.with(name, lines, characters);
    }

    /** After opening a name that named nothing yet. */
    public static Text newFile(final String name) {
        return NEW_FILE.with(name);
    }

    /** A prompt being answered: what it asks, and what has been typed so far. */
    public static Text asking(final TextKey question, final String typed) {
        return ASKING.with(question, typed);
    }

    /** After a command nothing here knows. */
    public static Text unknownCommand(final String typed) {
        return UNKNOWN_COMMAND.with(typed);
    }

    /** After a search that found nothing anywhere. */
    public static Text notFound(final String needle) {
        return PATTERN_NOT_FOUND.with(needle);
    }

    /** After a name "read" was asked for that names nothing on the disk. */
    public static Text cannotRead(final String name) {
        return CANNOT_READ.with(name);
    }

    /**
     * The row naming where the caret stands, before the {@code =} that pads it out to the glass's width are
     * added: that padding is fixed and not a word of any language, so it is the terminal's to add once this
     * has been read in the player's own, not this class's.
     */
    public static Text position(final int line, final int col, final int fromTop) {
        return POSITION.with(line, col, fromTop);
    }

    private static TtyLook.Key key(final String chord, final TextKey does) {
        return new TtyLook.Key(chord, does.text());
    }

    /**
     * A line of the help page holding two chords side by side, each with what it does. Left as two loose
     * pairs with room between rather than padded to a column count, since this glass draws a proportional
     * font that a character count of spaces does not reach evenly.
     */
    private static Text helpLinePair(final String chord1, final TextKey does1, final String chord2,
                                     final TextKey does2) {
        return HELP_LINE_PAIR.with(chord1, does1, chord2, does2);
    }

    /** A tip written out whole, with no chord of its own to badge. */
    private static TtyLook.Key tip(final TextKey does) {
        return new TtyLook.Key("", does.text());
    }

    /* A place in a row that nothing is listed in. */
    private static TtyLook.Key none() {
        return new TtyLook.Key("", Text.EMPTY);
    }
}
