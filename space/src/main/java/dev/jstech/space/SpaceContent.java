/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space;

import dev.jstech.core.content.ModContent;

/**
 * Where J's Space declares what it adds. Each declaration brings its block, item or block entity to the game and,
 * from the same line, its model, its English name and its loot to the data run, so nothing it adds is named in two
 * places.
 */
public final class SpaceContent {

    /** The mod's declarations, registered from its constructor. */
    public static final ModContent CONTENT = new ModContent(JsSpace.MODID);

    private SpaceContent() {
    }
}
