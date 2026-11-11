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
 * What a 3D printer's own panel says in the world: the Prosa's display and the Rattan's touchscreen. Apart from the
 * renderer so a server can read the words without loading it.
 */
@TextHolder
final class Printer3DPanelTexts {

    static final TextKey READY = TextKey.of("jsc.printer3d.screen.ready", "Ready");
    static final TextKey PRINTING = TextKey.of("jsc.printer3d.screen.printing", "Printing %s%%");
    static final TextKey TO_TRAY = TextKey.of("jsc.printer3d.screen.to_tray", "Done: to the tray");
    static final TextKey WAITING = TextKey.of("jsc.printer3d.screen.waiting", "Waiting for TRC");
    static final TextKey NO_WALLET = TextKey.of("jsc.printer3d.screen.no_wallet", "No wallet file");
    static final TextKey FULL = TextKey.of("jsc.printer3d.screen.full", "Output full");
    static final TextKey PAUSED = TextKey.of("jsc.printer3d.screen.paused", "Paused");
    static final TextKey HEIGHT = TextKey.of("jsc.printer3d.screen.height", "Z %s mm");
    static final TextKey TOUCH_PRINTING = TextKey.of("jsc.printer3d.touch.printing", "PRINTING");
    static final TextKey TOUCH_DONE = TextKey.of("jsc.printer3d.touch.done", "DONE");
    static final TextKey TOUCH_READY = TextKey.of("jsc.printer3d.touch.ready", "READY");
    static final TextKey TOUCH_WAITING = TextKey.of("jsc.printer3d.touch.waiting", "WAITING");
    static final TextKey TOUCH_PAUSED = TextKey.of("jsc.printer3d.touch.paused", "PAUSED");
    static final TextKey TOUCH_NAME = TextKey.of("jsc.printer3d.touch.name", "%s %s/%s");

    private Printer3DPanelTexts() {
    }
}
