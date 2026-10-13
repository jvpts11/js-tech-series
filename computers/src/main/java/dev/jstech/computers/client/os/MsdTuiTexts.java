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

/**
 * What the terminal says round the Vintage systems' diagnostics screen: the line at the foot. The keys' names, L or
 * F3, are data. Kept apart from the keys so the language generator can read it on a server too, where terminals do
 * not exist.
 */
@TextHolder
final class MsdTuiTexts {

    /* The main screen: the keys that move, open and leave. */
    static final TextKey MAIN = TextKey.of("jsc.msd_tui.main",
            "Arrows move   Enter opens   %s LPT ports   %s COM ports   %s exit");
    /* The ports dialog: the keys that pick, enable, disable and put it away. */
    static final TextKey PORTS = TextKey.of("jsc.msd_tui.ports",
            "Arrows pick a port   %s enable   %s disable   Enter closes");
    static final TextKey NO_MACHINE = TextKey.of("jsc.msd_tui.no_machine", "%s: there is no machine to read");

    private MsdTuiTexts() {
    }
}
