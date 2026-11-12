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

/** The words of a scanner's window. */
@TextHolder
final class ScannerTexts {

    static final TextKey GLASS = TextKey.of("jsc.scanner.glass", "GLASS");
    static final TextKey MAKES = TextKey.of("jsc.scanner.makes", "MAKES");
    static final TextKey SPEED = TextKey.of("jsc.scanner.speed", "SPEED");
    static final TextKey SCAN = TextKey.of("jsc.scanner.scan", "SCAN");
    static final TextKey STOP = TextKey.of("jsc.scanner.stop", "STOP");
    static final TextKey NOTE_1 = TextKey.of("jsc.scanner.note_1", "The item stays: a");
    static final TextKey NOTE_2 = TextKey.of("jsc.scanner.note_2", "scan only reads it.");
    static final TextKey SAVES_TO = TextKey.of("jsc.scanner.saves_to", "SAVES TO");
    static final TextKey NO_COMPUTER = TextKey.of("jsc.scanner.no_computer", "No computer");
    static final TextKey NO_FILE_YET = TextKey.of("jsc.scanner.no_file_yet", "No file yet");
    static final TextKey PAGE = TextKey.of("jsc.scanner.page", "PAGE");
    static final TextKey PAGE_OF = TextKey.of("jsc.scanner.page_of", "%s / %s");
    static final TextKey READS = TextKey.of("jsc.scanner.reads", "READS");
    static final TextKey READS_WRITTEN = TextKey.of("jsc.scanner.reads.written_books", "written books");
    static final TextKey READS_QUILLS = TextKey.of("jsc.scanner.reads.books_and_quills", "books and quills");
    static final TextKey READS_ENCHANTED = TextKey.of("jsc.scanner.reads.enchanted_books", "enchanted books");
    static final TextKey READS_MAPS = TextKey.of("jsc.scanner.reads.maps", "maps");
    static final TextKey READS_PAPER = TextKey.of("jsc.scanner.reads.printed_paper", "printed paper");
    static final TextKey LINKED = TextKey.of("jsc.scanner.linked", "LINKED");
    static final TextKey SCANNED = TextKey.of("jsc.scanner.scanned", "SCANNED");
    static final TextKey USES_UP = TextKey.of("jsc.scanner.uses_up", "USES UP");
    static final TextKey NOTHING = TextKey.of("jsc.scanner.nothing", "nothing");
    static final TextKey NONE = TextKey.of("jsc.scanner.none", "none");
    static final TextKey FILES = TextKey.of("jsc.scanner.files", "%s files");
    static final TextKey ONE_FILE = TextKey.of("jsc.scanner.one_file", "1 file");
    static final TextKey PER_PAGE = TextKey.of("jsc.scanner.per_page", "%s s a page");
    static final TextKey EMPTY_GLASS = TextKey.of("jsc.scanner.empty_glass", "Lay a book, a map");
    static final TextKey EMPTY_GLASS_2 = TextKey.of("jsc.scanner.empty_glass_2", "or a printed sheet");
    static final TextKey EMPTY_GLASS_3 = TextKey.of("jsc.scanner.empty_glass_3", "on the glass.");
    static final TextKey BY = TextKey.of("jsc.scanner.by", "by %s");
    static final TextKey PAGES = TextKey.of("jsc.scanner.pages", "%s pages");
    static final TextKey ONE_PAGE = TextKey.of("jsc.scanner.one_page", "1 page");
    static final TextKey MAP_NUMBER = TextKey.of("jsc.scanner.map_number", "Map #%s");
    static final TextKey STATE_READY = TextKey.of("jsc.scanner.state.ready", "READY");
    static final TextKey STATE_SCANNING = TextKey.of("jsc.scanner.state.scanning", "SCANNING");
    /** The state while a scan of many pages reads one of them: the page and how many there are. */
    static final TextKey STATE_PAGE = TextKey.of("jsc.scanner.state.page", "PAGE %s OF %s");
    static final TextKey STATE_NOTHING = TextKey.of("jsc.scanner.state.nothing_on_glass", "GLASS EMPTY");
    static final TextKey STATE_NOT_LINKED = TextKey.of("jsc.scanner.state.not_linked", "NO COMPUTER");
    static final TextKey STATE_NO_SYSTEM = TextKey.of("jsc.scanner.state.no_system", "NO SYSTEM");
    static final TextKey STATE_DISK_FULL = TextKey.of("jsc.scanner.state.disk_full", "DISK FULL");
    static final TextKey STATE_NOT_SAVED = TextKey.of("jsc.scanner.state.not_saved", "NOT SAVED");

    private ScannerTexts() {
    }
}
