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
 * What the three code editors, Virtual Studio, Virtual Studio Code and Exposure, say alike: the question before a
 * changed file is closed, the menu on the code, the menus they share, and what the code answers back. File names,
 * line numbers and what was typed are data. Kept apart from the windows so the language generator can read it on a
 * server too, where windows do not exist.
 */
@TextHolder
final class StudioTexts {

    // The small windows.
    static final TextKey OK = TextKey.of("jsc.studio.ok", "OK");
    static final TextKey CLOSE = TextKey.of("jsc.studio.close", "Close");
    static final TextKey SAVE_CHANGES = TextKey.of("jsc.studio.save_changes", "Save changes to %s?");
    static final TextKey SAVE = TextKey.of("jsc.studio.save", "Save");
    static final TextKey DONT_SAVE = TextKey.of("jsc.studio.dont_save", "Don't Save");
    static final TextKey CANCEL = TextKey.of("jsc.studio.cancel", "Cancel");
    static final TextKey NEW_FILE = TextKey.of("jsc.studio.new_file", "New File");
    static final TextKey FIND = TextKey.of("jsc.studio.find", "Find");
    static final TextKey GO_TO_LINE = TextKey.of("jsc.studio.go_to_line", "Go To Line");
    static final TextKey TAB_SIZE = TextKey.of("jsc.studio.tab_size", "Tab size");

    // The file windows they open.
    static final TextKey OPEN_FILE = TextKey.of("jsc.studio.open_file", "Open File");
    static final TextKey OPEN_FOLDER = TextKey.of("jsc.studio.open_folder", "Open Folder");
    static final TextKey SAVE_AS = TextKey.of("jsc.studio.save_as", "Save As");

    // The menu bars.
    static final TextKey FILE_MENU = TextKey.of("jsc.studio.file_menu", "File");
    static final TextKey EDIT_MENU = TextKey.of("jsc.studio.edit_menu", "Edit");
    static final TextKey VIEW_MENU = TextKey.of("jsc.studio.view_menu", "View");
    static final TextKey PROJECT_MENU = TextKey.of("jsc.studio.project_menu", "Project");
    static final TextKey HELP_MENU = TextKey.of("jsc.studio.help_menu", "Help");
    static final TextKey TERMINAL = TextKey.of("jsc.studio.terminal", "Terminal");
    static final TextKey TERMINAL_TAB = TextKey.of("jsc.studio.terminal_tab", "TERMINAL");
    static final TextKey START = TextKey.of("jsc.studio.start", "Start");
    static final TextKey STOP = TextKey.of("jsc.studio.stop", "Stop");

    // What the menus hold.
    static final TextKey NEW_FILE_ITEM = TextKey.of("jsc.studio.new_file_item", "New File...");
    static final TextKey OPEN_FILE_ITEM = TextKey.of("jsc.studio.open_file_item", "Open File...");
    static final TextKey OPEN_FOLDER_ITEM = TextKey.of("jsc.studio.open_folder_item", "Open Folder...");
    static final TextKey RECENT = TextKey.of("jsc.studio.recent", "Recent: %s");
    static final TextKey SAVE_AS_ITEM = TextKey.of("jsc.studio.save_as_item", "Save As...");
    static final TextKey SAVE_ALL = TextKey.of("jsc.studio.save_all", "Save All");
    static final TextKey EXIT = TextKey.of("jsc.studio.exit", "Exit");
    static final TextKey FIND_ITEM = TextKey.of("jsc.studio.find_item", "Find...");
    static final TextKey GO_TO_LINE_ITEM = TextKey.of("jsc.studio.go_to_line_item", "Go To Line...");
    static final TextKey TOGGLE_LINE_COMMENT = TextKey.of("jsc.studio.toggle_line_comment", "Toggle Line Comment");
    static final TextKey ZOOM_IN = TextKey.of("jsc.studio.zoom_in", "Zoom In");
    static final TextKey ZOOM_OUT = TextKey.of("jsc.studio.zoom_out", "Zoom Out");
    static final TextKey RESET_ZOOM = TextKey.of("jsc.studio.reset_zoom", "Reset Zoom");

    // The menu on the code.
    static final TextKey CUT = TextKey.of("jsc.studio.cut", "Cut");
    static final TextKey COPY = TextKey.of("jsc.studio.copy", "Copy");
    static final TextKey PASTE = TextKey.of("jsc.studio.paste", "Paste");
    static final TextKey SELECT_ALL = TextKey.of("jsc.studio.select_all", "Select All");
    static final TextKey REFACTOR = TextKey.of("jsc.studio.refactor", "Refactor");
    static final TextKey IMPLEMENT_INTERFACE = TextKey.of("jsc.studio.implement_interface", "Implement Interface");

    // What the code answers.
    static final TextKey NOT_A_LINE = TextKey.of("jsc.studio.not_a_line", "Not a line number: %s");
    static final TextKey NO_RESULTS = TextKey.of("jsc.studio.no_results", "No results for '%s'");
    static final TextKey INTERFACE_IMPLEMENTED =
            TextKey.of("jsc.studio.interface_implemented", "Interface implemented");
    static final TextKey NOTHING_TO_IMPLEMENT =
            TextKey.of("jsc.studio.nothing_to_implement", "Nothing to implement here");
    static final TextKey NOTHING_YET = TextKey.of("jsc.studio.nothing_yet", "Nothing yet");

    private StudioTexts() {
    }
}
