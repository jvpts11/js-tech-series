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

/** The words of a printer's window, kept apart from the screen so they can be read without the player's game. */
@TextHolder
final class PrinterTexts {

    static final TextKey PAPER = TextKey.of("jsc.printer.paper", "PAPER");
    static final TextKey NOW = TextKey.of("jsc.printer.now", "NOW");
    /* The track counts the job's sheets, every copy's pages, so it says sheet rather than page. */
    static final TextKey PAGE = TextKey.of("jsc.printer.page", "SHEET");
    static final TextKey QUEUE = TextKey.of("jsc.printer.queue", "QUEUE");
    static final TextKey OUT = TextKey.of("jsc.printer.out", "OUT");
    static final TextKey PAPER_NOTE_1 = TextKey.of("jsc.printer.paper_note_1", "sheets from the tray,");
    static final TextKey PAPER_NOTE_2 = TextKey.of("jsc.printer.paper_note_2", "one a page");
    static final TextKey NOTHING = TextKey.of("jsc.printer.nothing", "Nothing to print");
    static final TextKey PAGE_OF = TextKey.of("jsc.printer.page_of", "%s of %s");
    static final TextKey FROM_PAGES = TextKey.of("jsc.printer.from_pages", "from %s, %s pages");
    static final TextKey FROM_PAGE = TextKey.of("jsc.printer.from_page", "from %s, 1 page");
    static final TextKey PRINTING = TextKey.of("jsc.printer.status.printing", "printing");
    static final TextKey WAITING = TextKey.of("jsc.printer.status.waiting", "waiting");
    static final TextKey PAUSED = TextKey.of("jsc.printer.status.paused", "paused");
    static final TextKey NO_PAPER = TextKey.of("jsc.printer.status.no_paper", "no paper");
    static final TextKey OUTPUT_FULL = TextKey.of("jsc.printer.status.output_full", "take the sheets");
    static final TextKey EMPTY_QUEUE = TextKey.of("jsc.printer.empty_queue", "The queue is empty.");
    static final TextKey MORE = TextKey.of("jsc.printer.more", "and %s more");
    static final TextKey PAUSE = TextKey.of("jsc.printer.pause", "PAUSE");
    static final TextKey RESUME = TextKey.of("jsc.printer.resume", "RESUME");
    static final TextKey CANCEL = TextKey.of("jsc.printer.cancel", "CANCEL JOB");
    static final TextKey LINKED = TextKey.of("jsc.printer.linked", "Linked to a computer");
    static final TextKey OFFLINE = TextKey.of("jsc.printer.offline", "No computer linked");

    private PrinterTexts() {
    }
}
