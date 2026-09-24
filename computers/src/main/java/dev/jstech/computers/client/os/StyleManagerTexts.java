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

/** What CDE's Style Manager calls itself and its two pages. */
@TextHolder
final class StyleManagerTexts {

    static final TextKey TITLE = TextKey.of("jsc.style_manager.title", "Style Manager");
    static final TextKey COLOR_PAGE = TextKey.of("jsc.style_manager.color_page", "Color");
    static final TextKey BACKDROP_PAGE = TextKey.of("jsc.style_manager.backdrop_page", "Backdrop");

    private StyleManagerTexts() {
    }
}
