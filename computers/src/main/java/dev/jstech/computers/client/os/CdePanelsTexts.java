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

/** What CDE's Front Panel itself says: the four workspace names and the way out. */
@TextHolder
final class CdePanelsTexts {

    static final TextKey WORKSPACE_ONE = TextKey.of("jsc.cde.panels.workspace_one", "One");
    static final TextKey WORKSPACE_TWO = TextKey.of("jsc.cde.panels.workspace_two", "Two");
    static final TextKey WORKSPACE_THREE = TextKey.of("jsc.cde.panels.workspace_three", "Three");
    static final TextKey WORKSPACE_FOUR = TextKey.of("jsc.cde.panels.workspace_four", "Four");
    static final TextKey EXIT = TextKey.of("jsc.cde.panels.exit", "EXIT");

    private CdePanelsTexts() {
    }
}
