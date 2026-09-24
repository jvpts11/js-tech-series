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
 * What 67ark says: its buttons, its columns, the file windows it opens and its status line. File and folder names,
 * sizes and counts are data. Kept apart from the window so the language generator can read it on a server too,
 * where windows do not exist.
 */
@TextHolder
final class ArchiverTexts {

    // The toolbar.
    static final TextKey OPEN = TextKey.of("jsc.archiver.open", "Open");
    static final TextKey NEW = TextKey.of("jsc.archiver.new", "New");
    static final TextKey ADD = TextKey.of("jsc.archiver.add", "Add");
    static final TextKey REMOVE = TextKey.of("jsc.archiver.remove", "Remove");
    static final TextKey PACK = TextKey.of("jsc.archiver.pack", "Pack");
    static final TextKey EXTRACT = TextKey.of("jsc.archiver.extract", "Extract");
    static final TextKey EXTRACT_ALL = TextKey.of("jsc.archiver.extract_all", "Extract all");
    static final TextKey DELETE_ORIGINALS = TextKey.of("jsc.archiver.delete_originals", "Delete originals");
    static final TextKey KEEP_ORIGINALS = TextKey.of("jsc.archiver.keep_originals", "Keep originals");

    // The file windows it opens.
    static final TextKey OPEN_ARCHIVE = TextKey.of("jsc.archiver.open_archive", "Open archive");
    static final TextKey ARCHIVES = TextKey.of("jsc.archiver.archives", "Archives");
    static final TextKey ADD_TO_ARCHIVE = TextKey.of("jsc.archiver.add_to_archive", "Add to archive");
    static final TextKey PACK_INTO = TextKey.of("jsc.archiver.pack_into", "Pack into");
    static final TextKey TAKE_OUT_INTO = TextKey.of("jsc.archiver.take_out_into", "Take out into");
    static final TextKey TAKE_ALL_OUT_INTO =
            TextKey.of("jsc.archiver.take_all_out_into", "Take everything out into");

    // The list.
    static final TextKey NAME_COLUMN = TextKey.of("jsc.archiver.name_column", "Name");
    static final TextKey SIZE_COLUMN = TextKey.of("jsc.archiver.size_column", "Size");
    static final TextKey SAVED_COLUMN = TextKey.of("jsc.archiver.saved_column", "Saved");
    static final TextKey WHERE_COLUMN = TextKey.of("jsc.archiver.where_column", "Where");
    static final TextKey NO_ARCHIVE = TextKey.of("jsc.archiver.no_archive", "No archive open");
    static final TextKey NOTHING_IN_IT = TextKey.of("jsc.archiver.nothing_in_it", "Nothing in it");
    static final TextKey FILE = TextKey.of("jsc.archiver.file", "file");
    static final TextKey ROOT = TextKey.of("jsc.archiver.root", "root");
    static final TextKey BYTES = TextKey.of("jsc.archiver.bytes", "%s B");
    static final TextKey KILOBYTES = TextKey.of("jsc.archiver.kilobytes", "%s KB");

    // The status line.
    static final TextKey OPEN_OR_MAKE = TextKey.of("jsc.archiver.open_or_make", "Open an archive, or make one");
    static final TextKey READING = TextKey.of("jsc.archiver.reading", "Reading %s");
    static final TextKey NO_SUCH_FILE = TextKey.of("jsc.archiver.no_such_file", "No such file");
    static final TextKey NOT_AN_ARCHIVE = TextKey.of("jsc.archiver.not_an_archive", "%s is not an archive");
    static final TextKey ONE_FILE_INSIDE = TextKey.of("jsc.archiver.one_file_inside", "%s file inside");
    static final TextKey FILES_INSIDE = TextKey.of("jsc.archiver.files_inside", "%s files inside");
    static final TextKey ADD_FILES = TextKey.of("jsc.archiver.add_files", "Add the files to pack");
    static final TextKey ALREADY_LISTED = TextKey.of("jsc.archiver.already_listed", "That one is already in the list");
    static final TextKey ANOTHER_LISTED =
            TextKey.of("jsc.archiver.another_listed", "Another %s is already in the list");
    static final TextKey AT_MOST = TextKey.of("jsc.archiver.at_most", "An archive holds at most %s");
    static final TextKey ONE_TO_PACK = TextKey.of("jsc.archiver.one_to_pack", "%s file to pack");
    static final TextKey TO_PACK = TextKey.of("jsc.archiver.to_pack", "%s files to pack");
    static final TextKey NOTHING_TO_PACK = TextKey.of("jsc.archiver.nothing_to_pack", "Nothing to pack");
    static final TextKey PACKING = TextKey.of("jsc.archiver.packing", "Packing %s");
    static final TextKey PICK_FIRST = TextKey.of("jsc.archiver.pick_first", "Pick a file in the archive first");
    static final TextKey OPEN_FIRST = TextKey.of("jsc.archiver.open_first", "Open an archive first");
    static final TextKey TAKING_OUT = TextKey.of("jsc.archiver.taking_out", "Taking out");
    static final TextKey PERCENT_SAVED = TextKey.of("jsc.archiver.percent_saved", "%s%% saved");

    private ArchiverTexts() {
    }
}
