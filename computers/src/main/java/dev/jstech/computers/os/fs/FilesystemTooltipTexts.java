/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a medium's tooltip says of the files on it. File names, counts and extensions are data. Kept apart from the
 * tooltip, which reads the keyboard, so the language generator can read it on a server too.
 */
@TextHolder
final class FilesystemTooltipTexts {

    static final TextKey ONE_FILE = TextKey.of("jsc.filesystem_tooltip.one_file", "1 file:");
    static final TextKey FILES = TextKey.of("jsc.filesystem_tooltip.files", "%s files:");
    static final TextKey AND_MORE = TextKey.of("jsc.filesystem_tooltip.and_more", "  ...and %s more");
    static final TextKey BY_KIND = TextKey.of("jsc.filesystem_tooltip.by_kind", "%s files: %s");
    static final TextKey HOLD_SHIFT = TextKey.of("jsc.filesystem_tooltip.hold_shift", "Hold Shift to list");

    private FilesystemTooltipTexts() {
    }
}
