/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the typing line of a touch monitor says, where the chat's line is. */
@TextHolder
final class TouchTexts {

    static final TextKey TITLE = TextKey.of("jsc.touch.line.title", "Typing into a touch monitor");
    static final TextKey NOTE = TextKey.of("jsc.touch.line.note",
            "Typing into the touch monitor: Enter sends, Esc closes");
    static final TextKey LINE = TextKey.of("jsc.touch.line.line", "> %s");

    private TouchTexts() {
    }
}
