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
 * What the trash says, on the desktop and in each desktop's own trash window: the questions before something goes
 * for good, the properties of a thing in it, and each look's buttons, menus and columns, in the words its own desktop
 * used. The trash's own name is its kind's; file names and places are data. Kept apart from the windows so the
 * language generator can read it on a server too, where windows do not exist.
 */
@TextHolder
final class TrashTexts {

    // The questions.
    static final TextKey CONFIRM = TextKey.of("jsc.trash.confirm", "Confirm File Delete");
    static final TextKey ONE_ON_DISC = TextKey.of("jsc.trash.one_on_disc",
            "%s is on a removable disc, so it cannot go to the %s.\nDelete it for good?");
    static final TextKey MANY_ON_DISC = TextKey.of("jsc.trash.many_on_disc",
            "These %s items are on a removable disc, so they cannot go to the %s.\nDelete them for good?");
    static final TextKey ONE_ON_SHARE = TextKey.of("jsc.trash.one_on_share",
            "%s is on another machine's share, so it cannot go to the %s.\nDelete it for good?");
    static final TextKey MANY_ON_SHARE = TextKey.of("jsc.trash.many_on_share",
            "These %s items are on another machine's share, so they cannot go to the %s.\nDelete them for good?");
    static final TextKey DELETE_ONE = TextKey.of("jsc.trash.delete_one",
            "Delete %s for good?\nThis cannot be undone.");
    static final TextKey DELETE_MANY = TextKey.of("jsc.trash.delete_many",
            "Delete these %s items for good?\nThis cannot be undone.");
    static final TextKey EMPTY_ONE = TextKey.of("jsc.trash.empty_one", "Delete the item in the %s for good?");
    static final TextKey EMPTY_ALL = TextKey.of("jsc.trash.empty_all", "Delete all %s items in the %s for good?");
    static final TextKey EMPTY_EVERYTHING =
            TextKey.of("jsc.trash.empty_everything", "Delete everything in the %s for good?");

    // A thing's properties.
    static final TextKey PROPERTIES_OF = TextKey.of("jsc.trash.properties_of", "%s Properties");
    static final TextKey PROPERTIES_BODY =
            TextKey.of("jsc.trash.properties_body", "Name: %s\nOriginal location: %s\nSize: %s");

    // The trash's icon on the desktop.
    static final TextKey OPEN = TextKey.of("jsc.trash.open", "Open");
    static final TextKey EMPTY_NAMED = TextKey.of("jsc.trash.empty_named", "Empty %s");

    // Every look.
    static final TextKey PROPERTIES = TextKey.of("jsc.trash.properties", "Properties");
    static final TextKey RESTORE = TextKey.of("jsc.trash.restore", "Restore");
    static final TextKey DELETE = TextKey.of("jsc.trash.delete", "Delete");

    // Frames' Recycle Bin.
    static final TextKey EMPTY_RECYCLE_BIN = TextKey.of("jsc.trash.empty_recycle_bin", "Empty the Recycle Bin");
    static final TextKey RESTORE_ONE = TextKey.of("jsc.trash.restore_one", "Restore this item");
    static final TextKey RESTORE_MANY = TextKey.of("jsc.trash.restore_many", "Restore these items");
    static final TextKey TASKS = TextKey.of("jsc.trash.tasks", "Recycle Bin Tasks");
    static final TextKey DETAILS = TextKey.of("jsc.trash.details", "Details");
    static final TextKey NAME_COLUMN = TextKey.of("jsc.trash.name_column", "Name");
    static final TextKey PLACE_COLUMN = TextKey.of("jsc.trash.place_column", "Original Location");
    static final TextKey SIZE_COLUMN = TextKey.of("jsc.trash.size_column", "Size");

    // The Trash of the Linux desktops.
    static final TextKey EMPTY_TRASH = TextKey.of("jsc.trash.empty_trash", "Empty Trash");
    static final TextKey HOME = TextKey.of("jsc.trash.home", "Home");
    static final TextKey DESKTOP = TextKey.of("jsc.trash.desktop", "Desktop");
    static final TextKey RESTORE_FROM_TRASH = TextKey.of("jsc.trash.restore_from_trash", "Restore From Trash");
    static final TextKey DELETE_FROM_TRASH = TextKey.of("jsc.trash.delete_from_trash", "Delete From Trash");
    static final TextKey DELETE_PERMANENTLY = TextKey.of("jsc.trash.delete_permanently", "Delete Permanently");
    static final TextKey RESTORE_TO_FORMER =
            TextKey.of("jsc.trash.restore_to_former", "Restore to Former Location");

    // CDE's Trash Can.
    static final TextKey FILE_MENU = TextKey.of("jsc.trash.file_menu", "File");
    static final TextKey SELECTED_MENU = TextKey.of("jsc.trash.selected_menu", "Selected");
    static final TextKey VIEW_MENU = TextKey.of("jsc.trash.view_menu", "View");
    static final TextKey SELECT_ALL = TextKey.of("jsc.trash.select_all", "Select All");
    static final TextKey DESELECT_ALL = TextKey.of("jsc.trash.deselect_all", "Deselect All");
    static final TextKey CLOSE = TextKey.of("jsc.trash.close", "Close");
    static final TextKey BY_NAME = TextKey.of("jsc.trash.by_name", "By Name");
    static final TextKey BY_SIZE = TextKey.of("jsc.trash.by_size", "By Size");
    static final TextKey PUT_BACK = TextKey.of("jsc.trash.put_back", "Put Back");
    static final TextKey SHRED = TextKey.of("jsc.trash.shred", "Shred");

    private TrashTexts() {
    }
}
