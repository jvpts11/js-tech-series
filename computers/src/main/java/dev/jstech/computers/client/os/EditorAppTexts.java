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

/** The Editor window's own title, apart from what {@link EditorTexts} covers. */
@TextHolder
final class EditorAppTexts {

    static final TextKey TITLE = TextKey.of("jsc.editor_app.title", "%s - Editor");

    private EditorAppTexts() {
    }
}
