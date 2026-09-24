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
 * What the file explorer, and the desktop where it shares the explorer's work, say in words of their own: the
 * places, the kinds of row, the status bar, the properties and the right-button menu. File, folder, volume,
 * machine and program names are data. The words the explorer shares with the system's file window, such as the
 * columns and the drives, are that window's. Kept apart from the windows so the language generator can read it on
 * a server too, where windows do not exist.
 */
@TextHolder
final class FilesTexts {

    // The places.
    static final TextKey QUICK_ACCESS = TextKey.of("jsc.files.quick_access", "Quick access");
    static final TextKey STORAGE = TextKey.of("jsc.files.storage", "Storage");
    static final TextKey NETWORK = TextKey.of("jsc.files.network", "Network");
    static final TextKey SHARES = TextKey.of("jsc.files.shares", "Shares");
    static final TextKey SHARED_FOLDERS = TextKey.of("jsc.files.shared_folders", "Shared folders");

    // What a row is.
    static final TextKey STORED_ITEMS = TextKey.of("jsc.files.stored_items", "Stored items");
    static final TextKey STORED_ITEM = TextKey.of("jsc.files.stored_item", "Stored item");
    static final TextKey COUNT = TextKey.of("jsc.files.count", "%s it");
    static final TextKey REMOVABLE_DRIVE = TextKey.of("jsc.files.removable_drive", "Removable drive");
    static final TextKey COMPUTER = TextKey.of("jsc.files.computer", "Computer");
    static final TextKey SHARED_FOLDER = TextKey.of("jsc.files.shared_folder", "Shared folder");
    static final TextKey FILE = TextKey.of("jsc.files.file", "File");
    static final TextKey EXTENSION_FILE = TextKey.of("jsc.files.extension_file", "%s file");
    static final TextKey SOURCE = TextKey.of("jsc.files.source", "%s source");
    static final TextKey PROGRAM = TextKey.of("jsc.files.program", "%s program");

    // The status bar.
    static final TextKey WITH_SELECTED = TextKey.of("jsc.files.with_selected", "%s · %s selected");
    static final TextKey WITH_NAME = TextKey.of("jsc.files.with_name", "%s · %s");
    static final TextKey READ_ONLY_MEDIUM = TextKey.of("jsc.files.read_only_medium", "read-only medium");
    static final TextKey USED = TextKey.of("jsc.files.used", "%s mB used");

    // The properties.
    static final TextKey PROPERTIES = TextKey.of("jsc.files.properties", "Properties");
    static final TextKey CLOSE = TextKey.of("jsc.files.close", "Close");
    static final TextKey WHERE = TextKey.of("jsc.files.where", "Where");
    static final TextKey ACCESS = TextKey.of("jsc.files.access", "Access");
    static final TextKey READ_ONLY = TextKey.of("jsc.files.read_only", "read-only");
    static final TextKey READ_WRITE = TextKey.of("jsc.files.read_write", "read/write");

    // The right-button menu.
    static final TextKey OPEN = TextKey.of("jsc.files.open", "Open");
    static final TextKey RUN = TextKey.of("jsc.files.run", "Run");
    static final TextKey OPEN_WITH = TextKey.of("jsc.files.open_with", "Open with");
    static final TextKey CHOOSE_ANOTHER = TextKey.of("jsc.files.choose_another", "Choose another program...");
    static final TextKey CUT = TextKey.of("jsc.files.cut", "Cut");
    static final TextKey COPY = TextKey.of("jsc.files.copy", "Copy");
    static final TextKey PASTE = TextKey.of("jsc.files.paste", "Paste");
    static final TextKey RENAME = TextKey.of("jsc.files.rename", "Rename");
    static final TextKey DELETE = TextKey.of("jsc.files.delete", "Delete");
    static final TextKey NEW = TextKey.of("jsc.files.new", "New");
    static final TextKey NEW_FOLDER = TextKey.of("jsc.files.new_folder", "Folder");
    static final TextKey NEW_OF_TYPE = TextKey.of("jsc.files.new_of_type", "%s (.%s)");
    static final TextKey OPEN_IN = TextKey.of("jsc.files.open_in", "Open in %s");
    static final TextKey EJECT = TextKey.of("jsc.files.eject", "Eject");
    static final TextKey REFRESH = TextKey.of("jsc.files.refresh", "Refresh");
    static final TextKey EXTRACT_HERE = TextKey.of("jsc.files.extract_here", "Extract here");
    static final TextKey COMPRESS_TO = TextKey.of("jsc.files.compress_to", "Compress to %s");

    // What packing or unpacking came to, when it did not work.
    static final TextKey COULD_NOT = TextKey.of("jsc.files.could_not", "Could not do that");

    private FilesTexts() {
    }
}
