/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What an Electronics Workbench's screen says: what each era's bench makes, along its front edge, and its power
 * strip's display. Kept apart from the screen so the language generator can read it on a server too, where screens do
 * not exist.
 */
@TextHolder
final class WorkbenchTexts {

    static final TextKey MAKES_VINTAGE = TextKey.of("jsc.workbench.makes.vintage",
            "Vintage parts; the Legacy bench");
    static final TextKey MAKES_LEGACY = TextKey.of("jsc.workbench.makes.legacy",
            "Legacy parts and older; the Transition bench");
    static final TextKey MAKES_TRANSITION = TextKey.of("jsc.workbench.makes.transition",
            "Transition parts and older; the Standard bench");
    static final TextKey MAKES_STANDARD = TextKey.of("jsc.workbench.makes.standard",
            "Standard parts and older; the Advanced bench");
    static final TextKey MAKES_ADVANCED = TextKey.of("jsc.workbench.makes.advanced",
            "Advanced parts and older");
    /** What the strip's display reads: the bench's draw while it works. */
    static final TextKey WATTS = TextKey.of("jsc.workbench.watts", "%sW");

    private WorkbenchTexts() {
    }
}
