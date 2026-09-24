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

/** The titles of the Style Manager's own pages: the Backdrop page and the Color page. */
@TextHolder
final class CdeStyleTitles {

    static final TextKey BACKDROP_PAGE = TextKey.of("jsc.cde.style.backdrop_title", "Style Manager - Backdrop");
    static final TextKey COLOR_PAGE = TextKey.of("jsc.cde.style.color_title", "Style Manager - Color");

    private CdeStyleTitles() {
    }
}
