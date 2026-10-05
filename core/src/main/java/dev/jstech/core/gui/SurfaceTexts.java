/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a window drawn by a renderer says in place of its picture: to someone other than whoever opened it, and when
 * the renderer failed. They live where a server can load them too, so the language generator finds them.
 */
@TextHolder
public final class SurfaceTexts {

    public static final TextKey ONLY_OPENER = TextKey.of("jscore.surface.only_opener",
            "Shown only to whoever opened it");
    public static final TextKey FAILED = TextKey.of("jscore.surface.failed", "This picture could not be drawn");

    private SurfaceTexts() {
    }
}
