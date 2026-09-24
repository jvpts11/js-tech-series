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

/** The words of the Help Viewer around the entries, which carry their own. */
@TextHolder
final class HelpViewerTexts {

    static final TextKey SEARCH = TextKey.of("jsc.help_viewer.search", "Search:");

    private HelpViewerTexts() {
    }
}
