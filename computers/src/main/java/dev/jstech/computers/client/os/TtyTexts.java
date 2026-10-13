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
 * What the terminal editors and the pager say in their own words: emacs's echo area and compile buffer, Vim's
 * notes and its insert-mode line, and less's line at the foot. Files, keys and counts are data. Read in the
 * player's language as the real ones are; kept apart from the editors so the language generator can read it on a
 * server too, where terminals do not exist.
 */
@TextHolder
final class TtyTexts {

    // Any of them, on a file that was not there, and on one that has been written.
    static final TextKey NEW_FILE = TextKey.of("jsc.tty.new_file", "\"%s\" [New]");
    static final TextKey WRITTEN = TextKey.of("jsc.tty.written", "\"%s\" written");

    // emacs.
    static final TextKey ANSWER_Y_OR_N = TextKey.of("jsc.emacs.answer_y_or_n", "Please answer y or n.");
    static final TextKey QUIT = TextKey.of("jsc.emacs.quit", "Quit");
    static final TextKey UNDO = TextKey.of("jsc.emacs.undo", "Undo");
    static final TextKey NO_FURTHER_UNDO = TextKey.of("jsc.emacs.no_further_undo", "No further undo information");
    static final TextKey NO_COMPILER = TextKey.of("jsc.emacs.no_compiler", "no compiler knows %s");
    static final TextKey COMPILATION_FINISHED = TextKey.of("jsc.emacs.compilation_finished", "Compilation finished");
    static final TextKey ASSEMBLY_LINES = TextKey.of("jsc.emacs.assembly_lines", "%s -> %s lines of assembly");
    static final TextKey COMPILATION_FAILED =
            TextKey.of("jsc.emacs.compilation_failed", "Compilation exited abnormally with %s error(s)");

    // Vim.
    static final TextKey INSERT = TextKey.of("jsc.vim.insert", "-- INSERT --  %s");
    static final TextKey ONE_CHANGE_BEFORE = TextKey.of("jsc.vim.one_change_before", "1 change; before");
    static final TextKey OLDEST_CHANGE = TextKey.of("jsc.vim.oldest_change", "Already at oldest change");
    static final TextKey ONE_LINE_YANKED = TextKey.of("jsc.vim.one_line_yanked", "1 line yanked");
    static final TextKey ONE_CHANGE_AFTER = TextKey.of("jsc.vim.one_change_after", "1 change; after");
    static final TextKey NEWEST_CHANGE = TextKey.of("jsc.vim.newest_change", "Already at newest change");

    // less.
    static final TextKey LESS_STATUS =
            TextKey.of("jsc.less.status", "%s  line %s/%s  (%s%%)   /search   q quit");
    static final TextKey NO_SUCH_FILE = TextKey.of("jsc.less.no_such_file", "%s: no such file");
    static final TextKey PATTERN_NOT_FOUND = TextKey.of("jsc.less.pattern_not_found", "Pattern not found below: %s");

    private TtyTexts() {
    }
}
