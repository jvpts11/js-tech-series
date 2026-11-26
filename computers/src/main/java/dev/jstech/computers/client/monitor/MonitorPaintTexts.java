/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a monitor's own menu says on its glass in the world. */
@TextHolder
final class MonitorPaintTexts {

    static final TextKey NO_VRAM_TITLE = TextKey.of("jsc.monitor.osd.no_vram", "Not enough video memory");
    static final TextKey NO_VRAM_NEED = TextKey.of("jsc.monitor.osd.no_vram_need",
            "This monitor needs %s; %s are free.");
    static final TextKey NO_VRAM_WAIT = TextKey.of("jsc.monitor.osd.no_vram_wait",
            "It lights when the card has room.");
    static final TextKey NO_SIGNAL = TextKey.of("jsc.monitor.osd.no_signal", "NO SIGNAL");
    /* How a monitor finds the computer it hangs from, while none is in reach. */
    static final TextKey NO_COMPUTER_RULE = TextKey.of("jsc.monitor.osd.no_computer_rule",
            "A monitor hangs from the nearest computer: one against it, or one its era's peripheral cable leads to,"
                    + " up to the cable's reach.");
    /* The monitor's era, and how many cables its era's peripheral cable reaches. */
    static final TextKey NO_COMPUTER_REACH = TextKey.of("jsc.monitor.osd.no_computer_reach",
            "%s cable reaches %s. None in reach.");

    private MonitorPaintTexts() {
    }
}
