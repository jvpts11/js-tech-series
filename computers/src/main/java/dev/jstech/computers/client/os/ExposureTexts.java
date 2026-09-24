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
 * What Exposure says in words of its own: its panes, its problems table, its menus and its status line. File names,
 * the outline's declarations and line numbers are data. Kept apart from the window so the language generator can
 * read it on a server too, where windows do not exist.
 */
@TextHolder
final class ExposureTexts {

    // The window.
    static final TextKey REBUILD_ALL_BUTTON = TextKey.of("jsc.exposure.rebuild_all_button", "Rebuild all");
    static final TextKey FOLDER = TextKey.of("jsc.exposure.folder", "Folder: %s");
    static final TextKey PACKAGE = TextKey.of("jsc.exposure.package", "PACKAGE");
    static final TextKey OUTLINE = TextKey.of("jsc.exposure.outline", "OUTLINE");
    static final TextKey PROBLEMS = TextKey.of("jsc.exposure.problems", "PROBLEMS (%s)");
    static final TextKey LINE = TextKey.of("jsc.exposure.line", "LINE");
    static final TextKey COMPILER_SAID = TextKey.of("jsc.exposure.compiler_said", "WHAT THE COMPILER SAID");
    static final TextKey EMPTY = TextKey.of("jsc.exposure.empty", "Pick a file, or File > New File");
    static final TextKey WRITABLE = TextKey.of("jsc.exposure.writable", "Writable");

    // The menus.
    static final TextKey SOURCE_MENU = TextKey.of("jsc.exposure.source_menu", "Source");
    static final TextKey CLOSE_FILE = TextKey.of("jsc.exposure.close_file", "Close File");
    static final TextKey REBUILD_ALL = TextKey.of("jsc.exposure.rebuild_all", "Rebuild All");
    static final TextKey RUN_AT_TERMINAL = TextKey.of("jsc.exposure.run_at_terminal", "Run at Terminal");

    // What it says back.
    static final TextKey NOT_A_PROGRAM = TextKey.of("jsc.exposure.not_a_program", "%s is not a program to run");

    private ExposureTexts() {
    }
}
