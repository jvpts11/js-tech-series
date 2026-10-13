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
 * What Virtual Studio Code says in words of its own: the Welcome page, its menus and the palette's commands, the
 * side bar and the status bar. Folder and file names, language names and keys such as Ctrl+P are data. Kept apart
 * from the window so the language generator can read it on a server too, where windows do not exist.
 */
@TextHolder
final class VsCodeTexts {

    // The window.
    static final TextKey PROBLEMS_TAB = TextKey.of("jsc.vs_code.problems_tab", "PROBLEMS");
    static final TextKey EXTENSIONS_CAPTION = TextKey.of("jsc.vs_code.extensions_caption", "EXTENSIONS");
    static final TextKey INSTALLED = TextKey.of("jsc.vs_code.installed", "installed");
    static final TextKey SETTINGS_TITLE = TextKey.of("jsc.vs_code.settings_title", "Settings");
    static final TextKey GO_TO_LINE = TextKey.of("jsc.vs_code.go_to_line", "Go to Line");
    static final TextKey EMPTY = TextKey.of("jsc.vs_code.empty", "Pick a file in the Explorer, or Ctrl+P");
    static final TextKey NO_FOLDER = TextKey.of("jsc.vs_code.no_folder", "No folder open");
    static final TextKey SPACES = TextKey.of("jsc.vs_code.spaces", "Spaces: %s");

    // The Welcome page.
    static final TextKey EDITING_EVOLVED = TextKey.of("jsc.vs_code.editing_evolved", "Editing evolved");
    static final TextKey RECENT_HEADING = TextKey.of("jsc.vs_code.recent_heading", "Recent");
    static final TextKey WALKTHROUGHS = TextKey.of("jsc.vs_code.walkthroughs", "Walkthroughs");
    static final TextKey GET_STARTED = TextKey.of("jsc.vs_code.get_started", "Get started with Σ#:");
    static final TextKey WALKTHROUGH_STEPS =
            TextKey.of("jsc.vs_code.walkthrough_steps", "open a folder, write, press F5");
    static final TextKey SHORTCUTS_LINK = TextKey.of("jsc.vs_code.shortcuts_link", "Keyboard shortcuts");
    static final TextKey PALETTE_LINK = TextKey.of("jsc.vs_code.palette_link", "Command palette");

    // The menus.
    static final TextKey GO_MENU = TextKey.of("jsc.vs_code.go_menu", "Go");
    static final TextKey RUN_MENU = TextKey.of("jsc.vs_code.run_menu", "Run");
    static final TextKey CLOSE_EDITOR = TextKey.of("jsc.vs_code.close_editor", "Close Editor");
    static final TextKey CLOSE_FOLDER = TextKey.of("jsc.vs_code.close_folder", "Close Folder");
    static final TextKey PALETTE_ITEM = TextKey.of("jsc.vs_code.palette_item", "Command Palette...");
    static final TextKey EXPLORER = TextKey.of("jsc.vs_code.explorer", "Explorer");
    static final TextKey EXTENSIONS = TextKey.of("jsc.vs_code.extensions", "Extensions");
    static final TextKey PROBLEMS = TextKey.of("jsc.vs_code.problems", "Problems");
    static final TextKey APPEARANCE = TextKey.of("jsc.vs_code.appearance", "Appearance");
    static final TextKey GO_TO_FILE_ITEM = TextKey.of("jsc.vs_code.go_to_file_item", "Go to File...");
    static final TextKey GO_TO_LINE_ITEM = TextKey.of("jsc.vs_code.go_to_line_item", "Go to Line...");
    static final TextKey RUN_FILE = TextKey.of("jsc.vs_code.run_file", "Run File");
    static final TextKey BUILD_FILE = TextKey.of("jsc.vs_code.build_file", "Build File");
    static final TextKey BUILD_FOLDER = TextKey.of("jsc.vs_code.build_folder", "Build Folder");
    static final TextKey NEW_TERMINAL = TextKey.of("jsc.vs_code.new_terminal", "New Terminal");
    static final TextKey CLEAR = TextKey.of("jsc.vs_code.clear", "Clear");
    static final TextKey WELCOME = TextKey.of("jsc.vs_code.welcome", "Welcome");
    static final TextKey KEYBOARD_SHORTCUTS = TextKey.of("jsc.vs_code.keyboard_shortcuts", "Keyboard Shortcuts");
    static final TextKey ABOUT_ITEM = TextKey.of("jsc.vs_code.about_item", "About");
    static final TextKey ABOUT = TextKey.of("jsc.vs_code.about", "Virtual Studio Code, by Midsoft. Σ# 1.0.");

    // The palette: a command under the name of what it belongs to.
    static final TextKey COMMAND = TextKey.of("jsc.vs_code.command", "%s: %s");
    static final TextKey TOGGLE_PROBLEMS = TextKey.of("jsc.vs_code.toggle_problems", "Toggle Problems");
    static final TextKey PREFERENCES = TextKey.of("jsc.vs_code.preferences", "Preferences");
    static final TextKey OPEN_SETTINGS = TextKey.of("jsc.vs_code.open_settings", "Open Settings");

    // What it says back.
    static final TextKey NOT_A_PROGRAM =
            TextKey.of("jsc.vs_code.not_a_program", "%s is not a program to build");
    static final TextKey SHORTCUTS = TextKey.of("jsc.vs_code.shortcuts",
            "F5 run, Ctrl+Shift+B build, Ctrl+Shift+P palette, Ctrl+P file, Ctrl+G line, Ctrl+F find");
    static final TextKey NO_SEARCH = TextKey.of("jsc.vs_code.no_search", "Search across files is not here yet");

    private VsCodeTexts() {
    }
}
