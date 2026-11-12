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

/** The words of the Large Scale Printer's window. */
@TextHolder
final class LargeScalePrinterTexts {

    static final TextKey FEEDER = TextKey.of("jsc.large_scale_printer.feeder", "FEEDER");
    static final TextKey FEEDER_1 = TextKey.of("jsc.large_scale_printer.feeder_1", "Paper");
    static final TextKey FEEDER_2 = TextKey.of("jsc.large_scale_printer.feeder_2", "by hand, a bus");
    static final TextKey FEEDER_3 = TextKey.of("jsc.large_scale_printer.feeder_3", "or a hopper");
    static final TextKey SHEETS = TextKey.of("jsc.large_scale_printer.sheets", "SHEETS");
    static final TextKey A_BOOK = TextKey.of("jsc.large_scale_printer.a_book", "A BOOK");
    static final TextKey THREE_SHEETS = TextKey.of("jsc.large_scale_printer.three_sheets", "%s sheets");
    static final TextKey PAUSE = TextKey.of("jsc.large_scale_printer.pause", "PAUSE");
    static final TextKey RESUME = TextKey.of("jsc.large_scale_printer.resume", "RESUME");
    static final TextKey CANCEL = TextKey.of("jsc.large_scale_printer.cancel", "CANCEL");
    static final TextKey PACE_1 = TextKey.of("jsc.large_scale_printer.pace_1", "Books bind in");
    static final TextKey PACE_2 = TextKey.of("jsc.large_scale_printer.pace_2", "%s s each.");
    static final TextKey QUEUE = TextKey.of("jsc.large_scale_printer.queue", "QUEUE");
    static final TextKey EMPTY_QUEUE = TextKey.of("jsc.large_scale_printer.empty_queue",
            "Nothing to print. Send a .book or a .ench");
    static final TextKey EMPTY_QUEUE_2 = TextKey.of("jsc.large_scale_printer.empty_queue_2",
            "from a computer's Print dialog.");
    static final TextKey TIMES = TextKey.of("jsc.large_scale_printer.times", "%s  x%s");
    static final TextKey FROM_PRINTING = TextKey.of("jsc.large_scale_printer.from_printing", "from %s, printing");
    static final TextKey FROM_WAITING = TextKey.of("jsc.large_scale_printer.from_waiting", "from %s, waiting");
    /** A queued enchanted book once the server has turned their printing off. */
    static final TextKey OFF_HERE = TextKey.of("jsc.large_scale_printer.off_here", "off on this server");
    static final TextKey MORE = TextKey.of("jsc.large_scale_printer.more", "and %s more");
    static final TextKey NOW = TextKey.of("jsc.large_scale_printer.now", "NOW");
    static final TextKey NOTHING = TextKey.of("jsc.large_scale_printer.nothing", "Nothing");
    static final TextKey COPY_OF = TextKey.of("jsc.large_scale_printer.copy_of", "%s, copy %s of %s");
    static final TextKey PAGES_OF = TextKey.of("jsc.large_scale_printer.pages_of", "pages %s of %s, then the binding");
    static final TextKey BINDING = TextKey.of("jsc.large_scale_printer.binding", "the binding");
    static final TextKey GO_TO = TextKey.of("jsc.large_scale_printer.go_to", "BOOKS GO TO");
    static final TextKey THE_NETWORK = TextKey.of("jsc.large_scale_printer.the_network", "the network");
    static final TextKey THE_CHEST = TextKey.of("jsc.large_scale_printer.the_chest", "the chest");
    static final TextKey SWITCH_TIP = TextKey.of("jsc.large_scale_printer.switch_tip",
            "Click to send the books to the other place");
    static final TextKey PRINTED = TextKey.of("jsc.large_scale_printer.printed", "PRINTED");
    static final TextKey BOOKS = TextKey.of("jsc.large_scale_printer.books", "%s books");
    static final TextKey ONE_BOOK = TextKey.of("jsc.large_scale_printer.one_book", "1 book");
    static final TextKey ENCHANTED = TextKey.of("jsc.large_scale_printer.enchanted", "ENCHANTED");
    static final TextKey ALLOWED = TextKey.of("jsc.large_scale_printer.allowed", "allowed");
    static final TextKey OFF = TextKey.of("jsc.large_scale_printer.off", "off");
    static final TextKey STATE_BOOK = TextKey.of("jsc.large_scale_printer.state.book", "BOOK %s OF %s");
    static final TextKey STATE_READY = TextKey.of("jsc.large_scale_printer.state.ready", "READY");
    static final TextKey STATE_PAUSED = TextKey.of("jsc.large_scale_printer.state.paused", "PAUSED");
    static final TextKey STATE_NO_PAPER = TextKey.of("jsc.large_scale_printer.state.no_paper", "NO PAPER");
    static final TextKey STATE_APART = TextKey.of("jsc.large_scale_printer.state.apart", "NOT FORMED");

    private LargeScalePrinterTexts() {
    }
}
