/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.jei;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the recipe viewer's transfer buttons tell a player. Kept apart from the handlers and free of the viewer's
 * types, so the language generator can read it whether or not the viewer is installed.
 */
@TextHolder
final class JeiTexts {

    static final TextKey OPEN_STUDIO =
            TextKey.of("jsc.jei.open_studio", "Open the Pattern Studio to transfer recipes");

    private JeiTexts() {
    }
}
