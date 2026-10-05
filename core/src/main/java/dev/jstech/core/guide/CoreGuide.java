/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.font.CoreFonts;
import dev.jstech.core.registry.CoreItems;
import java.util.Map;

/**
 * What the Core brings to every manual: the binder style, two cream pages side by side in navy vinyl covers with three
 * rings through them, its tables in the terminal font and its pages numbered by chapter, as technical manuals were.
 */
public final class CoreGuide {

    /** The binder style's id, which a manual names to be drawn in it. */
    public static final String BINDER = "jscore:binder";

    /** A binder's page, in the screen's pixels: two of them and the covers fit a screen of 640 by 360. */
    public static final int PAGE_WIDTH = 166;
    public static final int PAGE_HEIGHT = 201;
    public static final int MARGIN = 10;

    static {
        CoreItems.CONTENT.guide().style("binder", new GuideStyle("jscore:guide/binder", Map.of(), true, PAGE_WIDTH,
                PAGE_HEIGHT, MARGIN, true, "", CoreFonts.FIXED_6X10.id().toString(), GuideStyle.Folios.CHAPTER_PAGE));
    }

    private CoreGuide() {
    }

    /** Declares the Core's style, before the data generation writes the Core's files. */
    public static void declare() {
        // Loading the class declares it.
    }
}
