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

/** The words of the Print dialog and of the programs' Print actions. */
@TextHolder
public final class PrintTexts {

    public static final TextKey PRINT = TextKey.of("jsc.print.print", "Print");
    public static final TextKey PRINT_MENU = TextKey.of("jsc.print.print_menu", "Print...");
    public static final TextKey CANCEL = TextKey.of("jsc.print.cancel", "Cancel");
    public static final TextKey SELECT_PRINTER = TextKey.of("jsc.print.select_printer", "Select Printer");
    public static final TextKey PRINTER = TextKey.of("jsc.print.printer", "Printer");
    public static final TextKey STATUS = TextKey.of("jsc.print.status", "Status: %s");
    public static final TextKey LOCATION = TextKey.of("jsc.print.location", "Location: %s, on %s");
    public static final TextKey PAPER = TextKey.of("jsc.print.paper", "Paper: %s sheets in the tray");
    public static final TextKey PAGE_RANGE = TextKey.of("jsc.print.page_range", "Page Range");
    public static final TextKey ALL = TextKey.of("jsc.print.all", "All");
    public static final TextKey PAGES = TextKey.of("jsc.print.pages", "Pages:");
    public static final TextKey COPIES = TextKey.of("jsc.print.copies", "Copies");
    public static final TextKey ORIENTATION = TextKey.of("jsc.print.orientation", "Orientation");
    public static final TextKey PORTRAIT = TextKey.of("jsc.print.portrait", "Portrait");
    public static final TextKey LANDSCAPE = TextKey.of("jsc.print.landscape", "Landscape");
    public static final TextKey USES = TextKey.of("jsc.print.uses", "Uses %s sheets from the tray (%s left).");
    public static final TextKey USES_ONE = TextKey.of("jsc.print.uses_one", "Uses 1 sheet from the tray (%s left).");
    public static final TextKey LOOKING = TextKey.of("jsc.print.looking", "Looking for printers...");
    public static final TextKey PREVIEW_PAGE = TextKey.of("jsc.print.preview_page", "%s of %s");
    public static final TextKey SENT_TITLE = TextKey.of("jsc.print.sent_title", "Printing");
    public static final TextKey FAILED_TITLE = TextKey.of("jsc.print.failed_title", "Could not print");

    private PrintTexts() {
    }
}
