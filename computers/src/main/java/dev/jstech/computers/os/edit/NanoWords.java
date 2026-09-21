/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import java.util.List;

/**
 * What nano says, and the keys it lists under the text.
 *
 * <p>All of it is the real editor's own wording, because somebody who has used it reads the two rows at the
 * bottom without reading them: they know where Exit is. What is listed is what works here, in the places the
 * real one lists it.
 */
public final class NanoWords {

    /** What the title row starts with. */
    public static final String VERSION = "  GNU nano 8.2";

    /** What the title row names a buffer that came from no file. */
    public static final String NEW_BUFFER = "New Buffer";

    /** What the title row ends with once the text is not what is on the disk. */
    public static final String MODIFIED = "Modified";

    /** The questions it asks, each standing at the start of the row its answer is typed on. */
    public static final String SAVE_MODIFIED = "Save modified buffer? ";
    public static final String FILE_NAME = "File Name to Write: ";
    public static final String SEARCH = "Search";
    public static final String SEARCH_TO_REPLACE = "Search (to replace)";
    public static final String REPLACE_WITH = "Replace with: ";
    public static final String REPLACE_THIS = "Replace this instance? ";
    public static final String FILE_TO_INSERT = "File to insert [from ./]: ";

    /** The things it says in passing that are the same every time. */
    public static final String CANCELLED = "[ Cancelled ]";
    public static final String NEW_FILE = "[ New File ]";
    public static final String SEARCH_WRAPPED = "[ Search Wrapped ]";
    public static final String NOTHING_TO_UNDO = "[ Nothing to undo ]";
    public static final String NOTHING_TO_REDO = "[ Nothing to redo ]";

    /** The two rows under the text while it is being edited. */
    public static final List<List<TtyLook.Key>> EDITING = List.of(
            List.of(key("^G", "Help"), key("^O", "Write Out"), key("^W", "Where Is"), key("^K", "Cut")),
            List.of(key("^X", "Exit"), key("^R", "Read File"), key("^\\", "Replace"), key("^U", "Paste")));

    /** Under a question that is answered by typing something: the way out, where the real one keeps it. */
    public static final List<List<TtyLook.Key>> TYPING = List.of(
            List.of(key("", ""), key("", ""), key("", ""), key("", "")),
            List.of(key("^C", "Cancel"), key("", ""), key("", ""), key("", "")));

    /** Under a question that is answered yes or no. */
    public static final List<List<TtyLook.Key>> YES_OR_NO = List.of(
            List.of(key(" Y", "Yes"), key("", ""), key("", ""), key("", "")),
            List.of(key(" N", "No"), key("^C", "Cancel"), key("", ""), key("", "")));

    /** Under the question asked at each place a replacement could be made. */
    public static final List<List<TtyLook.Key>> EACH_OR_ALL = List.of(
            List.of(key(" Y", "Yes"), key(" A", "All"), key("", ""), key("", "")),
            List.of(key(" N", "No"), key("^C", "Cancel"), key("", ""), key("", "")));

    /** Under the help text. */
    public static final List<List<TtyLook.Key>> READING_HELP = List.of(
            List.of(key("^X", "Close"), key("", ""), key("", ""), key("", "")),
            List.of(key("", ""), key("", ""), key("", ""), key("", "")));

    /** The help text, which is the real one's opening and then the keys that are listed and a few that are not. */
    public static final List<String> HELP = List.of(
            "Main nano help text",
            "",
            " The nano editor is designed to emulate the",
            " functionality and ease-of-use of the UW Pico text",
            " editor.  The top line shows the program version,",
            " the file being edited, and whether or not the file",
            " has been modified.  The line above the two rows of",
            " keys shows important messages.",
            "",
            " The notation for shortcuts is as follows:",
            " Control-key sequences are notated with a '^' and",
            " Meta (Alt) sequences with 'M-'.",
            "",
            "^G    Display this help text",
            "^X    Close the current buffer / Exit from nano",
            "^O    Write the current buffer to disk",
            "^R    Insert another file into the current buffer",
            "^W    Search forward for a string",
            "^\\    Replace a string",
            "^K    Cut the current line into the cutbuffer",
            "^U    Paste the contents of the cutbuffer",
            "^C    Display the position of the cursor",
            "M-U   Undo the last operation",
            "M-E   Redo the last undone operation",
            "^A    Go to beginning of the current line",
            "^E    Go to end of the current line",
            "^Y    Go one screenful up",
            "^V    Go one screenful down");

    private NanoWords() {
    }

    /** Something said in passing, which this editor always puts in brackets. */
    public static String said(final String what) {
        return "[ " + what + " ]";
    }

    /** After a file has been written. */
    public static String wrote(final int lines) {
        return said("Wrote " + lines + (lines == 1 ? " line" : " lines"));
    }

    /** After a file has been read in, on opening it or on inserting it. */
    public static String read(final int lines) {
        return said("Read " + lines + (lines == 1 ? " line" : " lines"));
    }

    /**
     * A search's question, which offers the last thing searched for again: Enter by itself takes it.
     *
     * @param question which search is asking, the plain one or the one that replaces
     */
    public static String searching(final String question, final String last, final String typed) {
        return question + (last.isEmpty() ? "" : " [" + last + "]") + ": " + typed;
    }

    /** After a file that was asked for turned out not to be there. */
    public static String noSuchFile(final String name) {
        return said("File \"" + name + "\" not found");
    }

    /** After a search that found nothing anywhere. */
    public static String notFound(final String needle) {
        return said("\"" + needle + "\" not found");
    }

    /** After a replacement has been through the file. */
    public static String replaced(final int times) {
        return said("Replaced " + times + (times == 1 ? " occurrence" : " occurrences"));
    }

    /**
     * Where the cursor is, the way the real one counts it: the line among the lines, the column among that
     * line's columns, and the character among all of them, each with how far through that is.
     */
    public static String position(final int line, final int lines, final int column, final int columns,
                                  final int character, final int characters) {
        return said("line " + line + "/" + lines + " (" + percent(line, lines) + "%), col " + column + "/" + columns
                + " (" + percent(column, columns) + "%), char " + character + "/" + characters + " ("
                + percent(character, characters) + "%)");
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

    private static TtyLook.Key key(final String chord, final String does) {
        return new TtyLook.Key(chord, does);
    }
}
