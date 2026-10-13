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

/** The file explorer window's own title, apart from what {@link FilesTexts} covers. */
@TextHolder
final class FilesAppTexts {

    static final TextKey TITLE = TextKey.of("jsc.files_app.title", "Files");

    private FilesAppTexts() {
    }
}
