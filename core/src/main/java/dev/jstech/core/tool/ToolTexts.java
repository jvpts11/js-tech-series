/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a building tool tells its player on the action bar, and what a tool's tooltip says of its copy. */
@TextHolder
public final class ToolTexts {

    public static final TextKey CORNER = TextKey.of("jscore.tool.corner", "First corner marked; use on the second");
    public static final TextKey COPIED = TextKey.of("jscore.tool.copied", "Copied %s blocks");
    public static final TextKey TOO_BIG = TextKey.of("jscore.tool.too_big",
            "A copy holds at most %s blocks; this region has %s");
    public static final TextKey OTHER_WORLD = TextKey.of("jscore.tool.other_world",
            "The first corner is in another world; marked this one instead");
    public static final TextKey CLEARED = TextKey.of("jscore.tool.cleared", "Copy cleared");
    public static final TextKey NOTHING_COPIED = TextKey.of("jscore.tool.nothing_copied",
            "Nothing copied yet: mark two corners in copy mode");
    public static final TextKey OUT_OF = TextKey.of("jscore.tool.out_of", "Out of %s");
    public static final TextKey HOLDS_COPY = TextKey.of("jscore.tool.holds_copy",
            "Holds a copy of %s blocks, %s by %s by %s");

    private ToolTexts() {
    }
}
