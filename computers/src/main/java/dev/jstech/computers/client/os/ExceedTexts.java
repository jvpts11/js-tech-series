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
 * What Exceed says: its buttons, the file windows it opens and its status line. File names, cell names and what
 * the cells hold are data. Kept apart from the window so the language generator can read it on a server too, where
 * windows do not exist.
 */
@TextHolder
final class ExceedTexts {

    // The toolbar.
    static final TextKey OPEN = TextKey.of("jsc.exceed.open", "Open");
    static final TextKey SAVE = TextKey.of("jsc.exceed.save", "Save");
    static final TextKey SAVE_AS = TextKey.of("jsc.exceed.save_as", "Save As");
    static final TextKey REFRESH = TextKey.of("jsc.exceed.refresh", "Refresh");

    // The file windows it opens.
    static final TextKey OPEN_SHEET = TextKey.of("jsc.exceed.open_sheet", "Open sheet");
    static final TextKey SAVE_SHEET = TextKey.of("jsc.exceed.save_sheet", "Save sheet");
    static final TextKey SHEETS = TextKey.of("jsc.exceed.sheets", "Sheets");

    // The status line.
    static final TextKey READY = TextKey.of("jsc.exceed.ready", "Ready");
    static final TextKey ONE_LIVE_CELL = TextKey.of("jsc.exceed.one_live_cell", "%s live cell");
    static final TextKey LIVE_CELLS = TextKey.of("jsc.exceed.live_cells", "%s live cells");
    static final TextKey NEW_SHEET = TextKey.of("jsc.exceed.new_sheet", "New sheet");
    static final TextKey TOO_LARGE_TO_SAVE = TextKey.of("jsc.exceed.too_large_to_save", "Sheet too large to save");
    static final TextKey SAVING = TextKey.of("jsc.exceed.saving", "Saving");

    private ExceedTexts() {
    }
}
