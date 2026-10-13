/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words the toolkit's components say on their own, before a program gives them any of its own. */
@TextHolder
final class ComponentTexts {

    static final TextKey SEARCH = TextKey.of("gui.jscore.search", "Search");

    private ComponentTexts() {
    }
}
