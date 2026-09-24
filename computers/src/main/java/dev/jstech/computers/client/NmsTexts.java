/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Network Management Studio says round a query: its menus, panes, dialogs and status bar. The query
 * language's own words (its tables, columns and keywords) and the names of files are data. Kept apart from the
 * studio so the language generator can read it on a server too, where screens do not exist.
 */
@TextHolder
final class NmsTexts {

    // The menu bar and the File menu.
    static final TextKey FILE = TextKey.of("jsc.nms.menu.file", "File");
    static final TextKey EDIT = TextKey.of("jsc.nms.menu.edit", "Edit");
    static final TextKey VIEW = TextKey.of("jsc.nms.menu.view", "View");
    static final TextKey QUERY = TextKey.of("jsc.nms.menu.query", "Query");
    static final TextKey TOOLS = TextKey.of("jsc.nms.menu.tools", "Tools");
    static final TextKey WINDOW = TextKey.of("jsc.nms.menu.window", "Window");
    static final TextKey HELP = TextKey.of("jsc.nms.menu.help", "Help");
    static final TextKey NEW = TextKey.of("jsc.nms.menu.new", "New");
    static final TextKey SAVE = TextKey.of("jsc.nms.menu.save", "Save");
    static final TextKey SAVE_AS_ITEM = TextKey.of("jsc.nms.menu.save_as", "Save As...");
    static final TextKey OPEN_ITEM = TextKey.of("jsc.nms.menu.open", "Open...");

    // The status bar.
    static final TextKey READY = TextKey.of("jsc.nms.status.ready", "ready");
    static final TextKey COULD_NOT_OPEN = TextKey.of("jsc.nms.status.could_not_open", "could not open file");
    static final TextKey OPENED = TextKey.of("jsc.nms.status.opened", "opened: %s");
    static final TextKey EXECUTING = TextKey.of("jsc.nms.status.executing", "executing...");
    static final TextKey NEW_QUERY = TextKey.of("jsc.nms.status.new_query", "new query");
    static final TextKey ENTER_A_NAME = TextKey.of("jsc.nms.status.enter_a_name", "enter a file name");
    static final TextKey F5_TO_RUN = TextKey.of("jsc.nms.status.f5_to_run", "F5 to run");

    // The toolbar and the panes.
    static final TextKey EXECUTE = TextKey.of("jsc.nms.execute", "Execute");
    static final TextKey NETWORK = TextKey.of("jsc.nms.network", "network: %s");
    static final TextKey ENGINE = TextKey.of("jsc.nms.engine", "engine: %s");
    static final TextKey OBJECT_EXPLORER = TextKey.of("jsc.nms.object_explorer", "OBJECT EXPLORER");
    /* The explorer's root: the network, served by its Mainframe. */
    static final TextKey ON_MAINFRAME = TextKey.of("jsc.nms.on_mainframe", "%s (Mainframe)");
    static final TextKey RESULTS = TextKey.of("jsc.nms.results", "Results");
    static final TextKey MESSAGES = TextKey.of("jsc.nms.messages", "Messages");
    static final TextKey ITEM = TextKey.of("jsc.nms.grid.item", "item");
    static final TextKey QUANTITY = TextKey.of("jsc.nms.grid.quantity", "qty");
    static final TextKey NO_RESULT_SET = TextKey.of("jsc.nms.grid.no_result_set", "no result set");
    static final TextKey MESSAGE = TextKey.of("jsc.nms.grid.message", "message");
    static final TextKey NO_MESSAGES = TextKey.of("jsc.nms.grid.no_messages", "no messages yet");

    // The dialogs.
    static final TextKey SAVE_AS = TextKey.of("jsc.nms.dialog.save_as", "Save As");
    static final TextKey FILE_NAME = TextKey.of("jsc.nms.dialog.file_name", "File name:");
    static final TextKey SAVE_KEYS = TextKey.of("jsc.nms.dialog.save_keys", "Enter = save   Esc = cancel");
    static final TextKey OPEN_FILE = TextKey.of("jsc.nms.dialog.open_file", "Open IQL File");
    static final TextKey NO_FILES = TextKey.of("jsc.nms.dialog.no_files", "no .iql files on disk");
    static final TextKey CANCEL = TextKey.of("jsc.nms.dialog.cancel", "Cancel");

    private NmsTexts() {
    }
}
