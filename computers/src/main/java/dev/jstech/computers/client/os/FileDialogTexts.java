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
 * What the system's file window says: its places, its columns, its buttons and its bottom line. File and folder
 * names, paths, drive letters and extensions are data. Kept apart from the window so the language generator can
 * read it on a server too, where windows do not exist.
 */
@TextHolder
final class FileDialogTexts {

    // The places, and the address.
    static final TextKey PLACES = TextKey.of("jsc.file_dialog.places", "PLACES");
    static final TextKey HOME = TextKey.of("jsc.file_dialog.home", "Home");
    static final TextKey DESKTOP = TextKey.of("jsc.file_dialog.desktop", "Desktop");
    static final TextKey DEVICES_HEADING = TextKey.of("jsc.file_dialog.devices_heading", "DEVICES");
    static final TextKey ROOT = TextKey.of("jsc.file_dialog.root", "Root");
    static final TextKey QUICK_ACCESS = TextKey.of("jsc.file_dialog.quick_access", "QUICK ACCESS");
    static final TextKey THIS_PC_HEADING = TextKey.of("jsc.file_dialog.this_pc_heading", "THIS PC");
    static final TextKey LOCAL_DISK = TextKey.of("jsc.file_dialog.local_disk", "Local Disk (C:)");
    static final TextKey ON_DRIVE = TextKey.of("jsc.file_dialog.on_drive", "%s (%s)");
    static final TextKey DEVICES = TextKey.of("jsc.file_dialog.devices", "Devices");
    static final TextKey THIS_PC = TextKey.of("jsc.file_dialog.this_pc", "This PC");

    // The list.
    static final TextKey NAME_COLUMN = TextKey.of("jsc.file_dialog.name_column", "Name");
    static final TextKey TYPE_COLUMN = TextKey.of("jsc.file_dialog.type_column", "Type");
    static final TextKey SIZE_COLUMN = TextKey.of("jsc.file_dialog.size_column", "Size");
    static final TextKey UP_ONE_LEVEL = TextKey.of("jsc.file_dialog.up_one_level", "Up one level");
    static final TextKey FOLDER = TextKey.of("jsc.file_dialog.folder", "Folder");
    static final TextKey SIZE = TextKey.of("jsc.file_dialog.size", "%s mB");

    // The bottom rows.
    static final TextKey FOLDER_LABEL = TextKey.of("jsc.file_dialog.folder_label", "Folder:");
    static final TextKey FILE_NAME_LABEL = TextKey.of("jsc.file_dialog.file_name_label", "File name:");
    static final TextKey SAVE_AS_LABEL = TextKey.of("jsc.file_dialog.save_as_label", "Save as:");
    static final TextKey TYPE_LABEL = TextKey.of("jsc.file_dialog.type_label", "Type:");
    static final TextKey NEW_FOLDER = TextKey.of("jsc.file_dialog.new_folder", "New folder");
    static final TextKey CANCEL = TextKey.of("jsc.file_dialog.cancel", "Cancel");
    static final TextKey OPEN = TextKey.of("jsc.file_dialog.open", "Open");
    static final TextKey SELECT_FOLDER = TextKey.of("jsc.file_dialog.select_folder", "Select Folder");
    static final TextKey SAVE = TextKey.of("jsc.file_dialog.save", "Save");
    static final TextKey REPLACE = TextKey.of("jsc.file_dialog.replace", "Replace");

    // The kinds of file a window lists.
    static final TextKey ALL_FILES = TextKey.of("jsc.file_dialog.all_files", "All files");
    static final TextKey FILTER = TextKey.of("jsc.file_dialog.filter", "%s (%s)");

    // The bottom line.
    static final TextKey PICKS = TextKey.of("jsc.file_dialog.picks", "Select Folder picks %s");
    static final TextKey PICKS_THIS = TextKey.of("jsc.file_dialog.picks_this", "Select Folder picks this folder");
    static final TextKey TYPE_A_NAME = TextKey.of("jsc.file_dialog.type_a_name", "Type a name");
    static final TextKey WILL_BE_WRITTEN = TextKey.of("jsc.file_dialog.will_be_written", "Will be written to %s");
    static final TextKey ONE_ITEM = TextKey.of("jsc.file_dialog.one_item", "%s item");
    static final TextKey ITEMS = TextKey.of("jsc.file_dialog.items", "%s items");
    static final TextKey TYPE_OR_PICK =
            TextKey.of("jsc.file_dialog.type_or_pick", "Type a name, or pick one from the list");
    static final TextKey NO_SUCH_DRIVE = TextKey.of("jsc.file_dialog.no_such_drive", "No such drive: %s");
    static final TextKey NOT_FOUND = TextKey.of("jsc.file_dialog.not_found", "Not found: %s");
    static final TextKey EXISTS = TextKey.of("jsc.file_dialog.exists", "%s exists. Replace it?");

    private FileDialogTexts() {
    }
}
