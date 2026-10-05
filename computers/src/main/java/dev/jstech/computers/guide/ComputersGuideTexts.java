/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The sentences many entries of J's Computers' chapter share, each declared once and translated once. */
@TextHolder
public final class ComputersGuideTexts {

    /** Where nearly everything of the mod comes from: a tab for every era. */
    public static final TextKey FROM_THE_TABS = TextKey.of("jsc.manual.from_the_tabs", "From the creative menu: every"
            + " era has its tab, J's Computers - Vintage to J's Computers - Advanced. There are no crafting recipes"
            + " yet; they come with the parts industry will make.");
    /** Where what every era shares comes from. */
    public static final TextKey FROM_THE_SHARED_TAB = TextKey.of("jsc.manual.from_the_shared_tab",
            "From the creative menu, tab J's Computers.");
    /** Where what only the Standard has comes from. */
    public static final TextKey FROM_THE_STANDARD_TAB = TextKey.of("jsc.manual.from_the_standard_tab",
            "From the creative menu, tab J's Computers - Standard.");

    private ComputersGuideTexts() {
    }
}
