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
 * What the buses' screens say, kept apart from the screens so the language generator can read it on a server too,
 * where screens do not exist.
 */
@TextHolder
final class BusTexts {

    static final TextKey EXPORT_TITLE = TextKey.of("jsc.bus.screen.export_title", "EXPORT BUS");
    static final TextKey IMPORT_TITLE = TextKey.of("jsc.bus.screen.import_title", "IMPORT BUS");
    static final TextKey INPUT_TITLE = TextKey.of("jsc.bus.screen.input_title", "CRAFTING INPUT BUS");
    static final TextKey RECEIVING_TITLE = TextKey.of("jsc.bus.screen.receiving_title", "CRAFTING RECEIVING BUS");
    static final TextKey EXPORT_HINT =
            TextKey.of("jsc.bus.screen.export_hint", "Click an item to set the export filter");
    static final TextKey IMPORT_HINT = TextKey.of("jsc.bus.screen.import_hint",
            "Click an item to import only that type (empty = import everything)");
    static final TextKey INPUT_HINT =
            TextKey.of("jsc.bus.screen.input_hint", "Click an item to set what to feed the machine");
    static final TextKey RECEIVING_HINT =
            TextKey.of("jsc.bus.screen.receiving_hint", "Click an item to set what to receive from the machine");
    static final TextKey NAME_FIELD = TextKey.of("jsc.bus.screen.name_field", "name");
    static final TextKey LINKED = TextKey.of("jsc.bus.screen.linked", "LINKED");
    static final TextKey OFFLINE = TextKey.of("jsc.bus.screen.offline", "OFFLINE");
    static final TextKey NAME = TextKey.of("jsc.bus.screen.name", "NAME");
    static final TextKey MIN = TextKey.of("jsc.bus.screen.min", "MIN");
    static final TextKey MAX = TextKey.of("jsc.bus.screen.max", "MAX");
    static final TextKey ANY = TextKey.of("jsc.bus.screen.any", "any");
    static final TextKey MODE = TextKey.of("jsc.bus.screen.mode", "MODE");
    static final TextKey CONTINUOUS = TextKey.of("jsc.bus.screen.continuous", "CONTINUOUS");
    static final TextKey ON_DEMAND = TextKey.of("jsc.bus.screen.on_demand", "ON DEMAND");
    static final TextKey PASSIVE_FILTER = TextKey.of("jsc.bus.screen.passive_filter",
            "Filter pins what this face carries (empty = any). The crafting engine drives it.");
    static final TextKey INVENTORY = TextKey.of("jsc.bus.screen.inventory", "INVENTORY");

    private BusTexts() {
    }
}
