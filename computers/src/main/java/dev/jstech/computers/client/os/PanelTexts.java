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

/** The words a desktop's panel says on its own: its tray's tip and the list of a program's windows. */
@TextHolder
final class PanelTexts {

    static final TextKey NETWORK_CONNECTED = TextKey.of("jsc.panel.network_connected", "Network connected");
    static final TextKey NO_NETWORK = TextKey.of("jsc.panel.no_network", "No network");
    static final TextKey RAM = TextKey.of("jsc.panel.ram", "RAM %s");
    static final TextKey CLOSE_ALL = TextKey.of("jsc.panel.close_all", "Close all");

    private PanelTexts() {
    }
}
