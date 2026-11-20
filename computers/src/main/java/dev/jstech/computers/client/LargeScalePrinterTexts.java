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

/** The words of the Large Scale Printer's Queue tab. */
@TextHolder
final class LargeScalePrinterTexts {

    static final TextKey QUEUE = TextKey.of("jsc.large_scale_printer.queue", "Queue");
    static final TextKey PAUSE = TextKey.of("jsc.large_scale_printer.pause", "PAUSE");
    static final TextKey RESUME = TextKey.of("jsc.large_scale_printer.resume", "RESUME");
    static final TextKey CANCEL = TextKey.of("jsc.large_scale_printer.cancel", "CANCEL");
    static final TextKey EMPTY_QUEUE = TextKey.of("jsc.large_scale_printer.empty_queue",
            "Nothing to print. Send a .book or a .ench");
    static final TextKey EMPTY_QUEUE_2 = TextKey.of("jsc.large_scale_printer.empty_queue_2",
            "from a computer's Print dialog.");
    static final TextKey TIMES = TextKey.of("jsc.large_scale_printer.times", "%s  x%s");
    static final TextKey FROM_PRINTING = TextKey.of("jsc.large_scale_printer.from_printing", "%s, printing");
    static final TextKey FROM_WAITING = TextKey.of("jsc.large_scale_printer.from_waiting", "%s, waiting");
    /** A queued enchanted book once the server has turned their printing off. */
    static final TextKey OFF_HERE = TextKey.of("jsc.large_scale_printer.off_here", "off on this server");
    static final TextKey MORE = TextKey.of("jsc.large_scale_printer.more", "and %s more");
    static final TextKey NOW = TextKey.of("jsc.large_scale_printer.now", "now: %s");
    static final TextKey COPY_OF = TextKey.of("jsc.large_scale_printer.copy_of", "copy %s of %s");
    static final TextKey NOTHING = TextKey.of("jsc.large_scale_printer.nothing", "nothing");
    static final TextKey A_BOOK = TextKey.of("jsc.large_scale_printer.a_book", "a book: %s sheets");
    static final TextKey GO_TO = TextKey.of("jsc.large_scale_printer.go_to", "BOOKS TO");
    static final TextKey THE_NETWORK = TextKey.of("jsc.large_scale_printer.the_network", "NET");
    static final TextKey THE_CHEST = TextKey.of("jsc.large_scale_printer.the_chest", "CHEST");
    static final TextKey SWITCH_TIP = TextKey.of("jsc.large_scale_printer.switch_tip",
            "Click to send the books to the other place");
    static final TextKey PRINTED = TextKey.of("jsc.large_scale_printer.printed", "PRINTED");
    static final TextKey ENCHANTED = TextKey.of("jsc.large_scale_printer.enchanted", "ENCHANTED");
    static final TextKey ENCHANTED_TIP = TextKey.of("jsc.large_scale_printer.enchanted_tip",
            "Whether this server lets enchanted books be printed: %s");
    static final TextKey ALLOWED = TextKey.of("jsc.large_scale_printer.allowed", "allowed");
    static final TextKey OFF = TextKey.of("jsc.large_scale_printer.off", "off");

    private LargeScalePrinterTexts() {
    }
}
