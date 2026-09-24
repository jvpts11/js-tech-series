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
 * What the text editor and the code workspace say: their buttons and their status lines. File names and counts are
 * data. Kept apart from the windows so the language generator can read it on a server too, where windows do not
 * exist.
 */
@TextHolder
final class EditorTexts {

    // The toolbar.
    static final TextKey OPEN = TextKey.of("jsc.editor.open", "Open");
    static final TextKey SAVE = TextKey.of("jsc.editor.save", "Save");
    static final TextKey SAVE_AS = TextKey.of("jsc.editor.save_as", "Save As");
    static final TextKey FIND = TextKey.of("jsc.editor.find", "Find");
    static final TextKey GO_TO = TextKey.of("jsc.editor.go_to", "Go to");

    // The status line.
    static final TextKey CTRL_S_TO_SAVE = TextKey.of("jsc.editor.ctrl_s_to_save", "Ctrl+S to save");
    static final TextKey NEW_FILE = TextKey.of("jsc.editor.new_file", "New file");
    static final TextKey NEW_FILE_NAMED = TextKey.of("jsc.editor.new_file_named", "New file %s");
    static final TextKey OPENED = TextKey.of("jsc.editor.opened", "Opened %s");
    static final TextKey ONLY_SO_MANY = TextKey.of("jsc.editor.only_so_many", "Only %s files at once");
    static final TextKey UNSAVED =
            TextKey.of("jsc.editor.unsaved", "Unsaved changes in %s - close again to discard");
    static final TextKey SAVE_AS_FIRST = TextKey.of("jsc.editor.save_as_first", "Save As first");
    static final TextKey CANNOT_SAVE_DAT = TextKey.of("jsc.editor.cannot_save_dat", "Cannot save a .dat file");
    static final TextKey TOO_LONG = TextKey.of("jsc.editor.too_long", "Too long to save: %s of %s characters");
    static final TextKey SAVING = TextKey.of("jsc.editor.saving", "Saving...");
    static final TextKey TOO_LARGE = TextKey.of("jsc.editor.too_large", "%s is too large to open here");
    static final TextKey LINE_AND_COLUMN = TextKey.of("jsc.editor.line_and_column", "Ln %s, Col %s");
    static final TextKey ONE_LINE = TextKey.of("jsc.editor.one_line", "%s line");
    static final TextKey LINES = TextKey.of("jsc.editor.lines", "%s lines");
    static final TextKey WITH_PROBLEMS =
            TextKey.of("jsc.editor.with_problems", "%s of %s program(s) with problems");

    // The strip that finds, replaces and goes to a line, whose first caption is the Find button's word.
    static final TextKey LINE = TextKey.of("jsc.editor.line", "Line");
    static final TextKey REPLACE = TextKey.of("jsc.editor.replace", "Replace");
    static final TextKey NEXT = TextKey.of("jsc.editor.next", "Next");
    static final TextKey GO = TextKey.of("jsc.editor.go", "Go");
    static final TextKey REPLACE_ALL = TextKey.of("jsc.editor.replace_all", "Replace all");
    static final TextKey NONE_FOUND = TextKey.of("jsc.editor.none_found", "none");
    static final TextKey AT_LINE = TextKey.of("jsc.editor.at_line", "line %s");
    static final TextKey REPLACED = TextKey.of("jsc.editor.replaced", "%s replaced");
    static final TextKey ONE_MATCH = TextKey.of("jsc.editor.one_match", "%s match");
    static final TextKey MATCHES = TextKey.of("jsc.editor.matches", "%s matches");

    private EditorTexts() {
    }
}
