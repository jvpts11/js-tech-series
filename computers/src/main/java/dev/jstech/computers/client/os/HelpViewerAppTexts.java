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

/** The Help Viewer window's own title, apart from what {@link HelpViewerTexts} covers. */
@TextHolder
final class HelpViewerAppTexts {

    static final TextKey TITLE = TextKey.of("jsc.help_viewer_app.title", "Help Viewer");

    private HelpViewerAppTexts() {
    }
}
