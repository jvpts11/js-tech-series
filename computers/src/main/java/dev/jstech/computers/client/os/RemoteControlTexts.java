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
 * What Remote Control says: its heading, its button and whether each machine is up. Host names and systems are
 * data. Kept apart from the window so the language generator can read it on a server too, where windows do not
 * exist.
 */
@TextHolder
final class RemoteControlTexts {

    static final TextKey TITLE = TextKey.of("jsc.remote_control.title", "Remote Control");
    static final TextKey MACHINES = TextKey.of("jsc.remote_control.machines", "Machines on this network");
    static final TextKey NONE_REACHABLE =
            TextKey.of("jsc.remote_control.none_reachable", "No other machine is reachable.");
    static final TextKey TAKE_OVER = TextKey.of("jsc.remote_control.take_over", "Take over");
    static final TextKey SELECT = TextKey.of("jsc.remote_control.select", "Select a machine");
    static final TextKey KIND_AND_SYSTEM = TextKey.of("jsc.remote_control.kind_and_system", "%s  -  %s");
    static final TextKey UP = TextKey.of("jsc.remote_control.up", "up");
    static final TextKey OFF = TextKey.of("jsc.remote_control.off", "off");

    private RemoteControlTexts() {
    }
}
