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
 * What nano says, and the keys it lists under the text.
 *
 * <p>All of it is the real editor's own wording, because somebody who has used it reads the two rows at the
 * bottom without reading them: they know where Exit is. What is listed is what works here, in the places the
 * real one lists it. Like the real one's, it is read in the player's language; the keys it names are not words.
 */
@TextHolder
public final class NanoWords {

    /** What the title row starts with. */
    public static final String VERSION = "  GNU nano 8.2";

    /** What the title row names a buffer that came from no file. */
    public static final TextKey NEW_BUFFER = TextKey.of("jsc.nano.new_buffer", "New Buffer");

    /** What the title row ends with once the text is not what is on the disk. */
    public static final TextKey MODIFIED = TextKey.of("jsc.nano.modified", "Modified");

    /** The questions it asks, each standing at the start of the row its answer is typed on. */
    public static final TextKey SAVE_MODIFIED = TextKey.of("jsc.nano.save_modified", "Save modified buffer? ");
    public static final TextKey FILE_NAME = TextKey.of("jsc.nano.file_name", "File Name to Write: %s");
    public static final TextKey SEARCH = TextKey.of("jsc.nano.search", "Search");
    public static final TextKey SEARCH_TO_REPLACE = TextKey.of("jsc.nano.search_to_replace", "Search (to replace)");
    public static final TextKey REPLACE_WITH = TextKey.of("jsc.nano.replace_with", "Replace with: %s");
    public static final TextKey REPLACE_THIS = TextKey.of("jsc.nano.replace_this", "Replace this instance? ");
    public static final TextKey FILE_TO_INSERT = TextKey.of("jsc.nano.file_to_insert", "File to insert [from ./]: %s");

    /** The things it says in passing that are the same every time. */
    public static final TextKey CANCELLED = TextKey.of("jsc.nano.cancelled", "[ Cancelled ]");
    public static final TextKey NEW_FILE = TextKey.of("jsc.nano.new_file", "[ New File ]");
    public static final TextKey SEARCH_WRAPPED = TextKey.of("jsc.nano.search_wrapped", "[ Search Wrapped ]");
    public static final TextKey NOTHING_TO_UNDO = TextKey.of("jsc.nano.nothing_to_undo", "[ Nothing to undo ]");
    public static final TextKey NOTHING_TO_REDO = TextKey.of("jsc.nano.nothing_to_redo", "[ Nothing to redo ]");

    // What the keys under the text do.
    private static final TextKey HELP_KEY = TextKey.of("jsc.nano.key.help", "Help");
    private static final TextKey WRITE_OUT = TextKey.of("jsc.nano.key.write_out", "Write Out");
    private static final TextKey WHERE_IS = TextKey.of("jsc.nano.key.where_is", "Where Is");
    private static final TextKey CUT = TextKey.of("jsc.nano.key.cut", "Cut");
    private static final TextKey EXIT = TextKey.of("jsc.nano.key.exit", "Exit");
    private static final TextKey READ_FILE = TextKey.of("jsc.nano.key.read_file", "Read File");
    private static final TextKey REPLACE = TextKey.of("jsc.nano.key.replace", "Replace");
    private static final TextKey PASTE = TextKey.of("jsc.nano.key.paste", "Paste");
    private static final TextKey CANCEL = TextKey.of("jsc.nano.key.cancel", "Cancel");
    private static final TextKey YES = TextKey.of("jsc.nano.key.yes", "Yes");
    private static final TextKey NO = TextKey.of("jsc.nano.key.no", "No");
    private static final TextKey ALL = TextKey.of("jsc.nano.key.all", "All");
    private static final TextKey CLOSE = TextKey.of("jsc.nano.key.close", "Close");

    // What it says in passing that depends on what happened; each is bracketed like the rest.
    private static final TextKey WROTE_ONE = TextKey.of("jsc.nano.wrote_one", "[ Wrote %s line ]");
    private static final TextKey WROTE = TextKey.of("jsc.nano.wrote", "[ Wrote %s lines ]");
    private static final TextKey READ_ONE = TextKey.of("jsc.nano.read_one", "[ Read %s line ]");
    private static final TextKey READ = TextKey.of("jsc.nano.read", "[ Read %s lines ]");
    private static final TextKey NO_SUCH_FILE = TextKey.of("jsc.nano.no_such_file", "[ File \"%s\" not found ]");
    private static final TextKey NOT_FOUND = TextKey.of("jsc.nano.not_found", "[ \"%s\" not found ]");
    private static final TextKey REPLACED_ONE = TextKey.of("jsc.nano.replaced_one", "[ Replaced %s occurrence ]");
    private static final TextKey REPLACED = TextKey.of("jsc.nano.replaced", "[ Replaced %s occurrences ]");
    private static final TextKey POSITION = TextKey.of("jsc.nano.position",
            "[ line %s/%s (%s%%), col %s/%s (%s%%), char %s/%s (%s%%) ]");
    /* A search's question, with and without the last thing searched for offered again. */
    private static final TextKey ASKING = TextKey.of("jsc.nano.asking", "%s: %s");
    private static final TextKey ASKING_AGAIN = TextKey.of("jsc.nano.asking_again", "%s [%s]: %s");

    // The help text: the real one's opening, then the keys that are listed and a few that are not.
    private static final TextKey HELP_TITLE = TextKey.of("jsc.nano.help.title", "Main nano help text");
    private static final TextKey HELP_ABOUT = TextKey.of("jsc.nano.help.about",
            " The nano editor is designed to emulate the functionality and ease-of-use of the UW Pico text"
                    + " editor.  The top line shows the program version, the file being edited, and whether or"
                    + " not the file has been modified.  The line above the two rows of keys shows important"
                    + " messages.");
    private static final TextKey HELP_NOTATION = TextKey.of("jsc.nano.help.notation",
            " The notation for shortcuts is as follows: Control-key sequences are notated with a '^' and"
                    + " Meta (Alt) sequences with 'M-'.");
    private static final TextKey HELP_KEY_LINE = TextKey.of("jsc.nano.help.key_line", "%s%s");
    private static final TextKey HELP_DISPLAY = TextKey.of("jsc.nano.help.display", "Display this help text");
    private static final TextKey HELP_CLOSE =
            TextKey.of("jsc.nano.help.close", "Close the current buffer / Exit from nano");
    private static final TextKey HELP_WRITE = TextKey.of("jsc.nano.help.write", "Write the current buffer to disk");
    private static final TextKey HELP_INSERT =
            TextKey.of("jsc.nano.help.insert", "Insert another file into the current buffer");
    private static final TextKey HELP_SEARCH = TextKey.of("jsc.nano.help.search", "Search forward for a string");
    private static final TextKey HELP_REPLACE = TextKey.of("jsc.nano.help.replace", "Replace a string");
    private static final TextKey HELP_CUT = TextKey.of("jsc.nano.help.cut", "Cut the current line into the cutbuffer");
    private static final TextKey HELP_PASTE = TextKey.of("jsc.nano.help.paste", "Paste the contents of the cutbuffer");
    private static final TextKey HELP_POSITION =
            TextKey.of("jsc.nano.help.position", "Display the position of the cursor");
    private static final TextKey HELP_UNDO = TextKey.of("jsc.nano.help.undo", "Undo the last operation");
    private static final TextKey HELP_REDO = TextKey.of("jsc.nano.help.redo", "Redo the last undone operation");
    private static final TextKey HELP_HOME = TextKey.of("jsc.nano.help.home", "Go to beginning of the current line");
    private static final TextKey HELP_END = TextKey.of("jsc.nano.help.end", "Go to end of the current line");
    private static final TextKey HELP_PAGE_UP = TextKey.of("jsc.nano.help.page_up", "Go one screenful up");
    private static final TextKey HELP_PAGE_DOWN = TextKey.of("jsc.nano.help.page_down", "Go one screenful down");

    /** The two rows under the text while it is being edited. */
    public static final List<List<TtyLook.Key>> EDITING = List.of(
            List.of(key("^G", HELP_KEY), key("^O", WRITE_OUT), key("^W", WHERE_IS), key("^K", CUT)),
            List.of(key("^X", EXIT), key("^R", READ_FILE), key("^\\", REPLACE), key("^U", PASTE)));

    /** Under a question that is answered by typing something: the way out, where the real one keeps it. */
    public static final List<List<TtyLook.Key>> TYPING = List.of(
            List.of(none(), none(), none(), none()),
            List.of(key("^C", CANCEL), none(), none(), none()));

    /** Under a question that is answered yes or no. */
    public static final List<List<TtyLook.Key>> YES_OR_NO = List.of(
            List.of(key(" Y", YES), none(), none(), none()),
            List.of(key(" N", NO), key("^C", CANCEL), none(), none()));

    /** Under the question asked at each place a replacement could be made. */
    public static final List<List<TtyLook.Key>> EACH_OR_ALL = List.of(
            List.of(key(" Y", YES), key(" A", ALL), none(), none()),
            List.of(key(" N", NO), key("^C", CANCEL), none(), none()));

    /** Under the help text. */
    public static final List<List<TtyLook.Key>> READING_HELP = List.of(
            List.of(key("^X", CLOSE), none(), none(), none()),
            List.of(none(), none(), none(), none()));

    /**
     * The help text, which is the real one's opening and then the keys that are listed and a few that are not. The
     * two paragraphs of the opening are one line each here, and wrap to the page where they are drawn.
     */
    public static final List<Text> HELP = List.of(
            HELP_TITLE.text(),
            Text.EMPTY,
            HELP_ABOUT.text(),
            Text.EMPTY,
            HELP_NOTATION.text(),
            Text.EMPTY,
            helpKey("^G", HELP_DISPLAY),
            helpKey("^X", HELP_CLOSE),
            helpKey("^O", HELP_WRITE),
            helpKey("^R", HELP_INSERT),
            helpKey("^W", HELP_SEARCH),
            helpKey("^\\", HELP_REPLACE),
            helpKey("^K", HELP_CUT),
            helpKey("^U", HELP_PASTE),
            helpKey("^C", HELP_POSITION),
            helpKey("M-U", HELP_UNDO),
            helpKey("M-E", HELP_REDO),
            helpKey("^A", HELP_HOME),
            helpKey("^E", HELP_END),
            helpKey("^Y", HELP_PAGE_UP),
            helpKey("^V", HELP_PAGE_DOWN));

    private NanoWords() {
    }

    /** After a file has been written. */
    public static Text wrote(final int lines) {
        return (lines == 1 ? WROTE_ONE : WROTE).with(lines);
    }

    /** After a file has been read in, on opening it or on inserting it. */
    public static Text read(final int lines) {
        return (lines == 1 ? READ_ONE : READ).with(lines);
    }

    /**
     * A search's question, which offers the last thing searched for again: Enter by itself takes it.
     *
     * @param question which search is asking, the plain one or the one that replaces
     */
    public static Text searching(final TextKey question, final String last, final String typed) {
        return last.isEmpty() ? ASKING.with(question, typed) : ASKING_AGAIN.with(question, last, typed);
    }

    /** After a file that was asked for turned out not to be there. */
    public static Text noSuchFile(final String name) {
        return NO_SUCH_FILE.with(name);
    }

    /** After a search that found nothing anywhere. */
    public static Text notFound(final String needle) {
        return NOT_FOUND.with(needle);
    }

    /** After a replacement has been through the file. */
    public static Text replaced(final int times) {
        return (times == 1 ? REPLACED_ONE : REPLACED).with(times);
    }

    /**
     * Where the cursor is, the way the real one counts it: the line among the lines, the column among that
     * line's columns, and the character among all of them, each with how far through that is.
     */
    public static Text position(final int line, final int lines, final int column, final int columns,
                                final int character, final int characters) {
        return POSITION.with(line, lines, percent(line, lines), column, columns, percent(column, columns),
                character, characters, percent(character, characters));
    }

    /**
     * A file's path as the title row shows it: what was typed, without whatever says which set of files it
     * belongs to, which is the machine's business and not something anybody typed.
     */
    public static String shown(final String path) {
        final int colon = path.indexOf(':');
        final int slash = path.indexOf('/');
        return colon >= 0 && (slash < 0 || colon < slash) ? path.substring(colon + 1) : path;
    }

    private static int percent(final int part, final int whole) {
        return whole <= 0 ? 0 : (int) Math.round(100.0 * part / whole);
    }

    private static TtyLook.Key key(final String chord, final TextKey does) {
        return new TtyLook.Key(chord, does.text());
    }

    /* A place in a row of keys that nothing is listed in. */
    private static TtyLook.Key none() {
        return new TtyLook.Key("", Text.EMPTY);
    }

    /* A line of the help text: the key, padded to where the words begin, and what it does. */
    private static Text helpKey(final String chord, final TextKey does) {
        return HELP_KEY_LINE.with(String.format("%-6s", chord), does);
    }
}
