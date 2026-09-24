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
 * What the Server Router's screen says, kept apart from the screen so the language generator can read it on a
 * server too, where screens do not exist.
 */
@TextHolder
final class ServerRouterTexts {

    static final TextKey TITLE = TextKey.of("jsc.server_router.title", "SERVER ROUTER");
    /* The router's industrial tier. */
    static final TextKey TIER = TextKey.of("jsc.server_router.tier", "T3");
    static final TextKey NAME = TextKey.of("jsc.server_router.name", "NAME");
    static final TextKey NAME_FIELD = TextKey.of("jsc.server_router.name_field", "Name");
    static final TextKey INPUT = TextKey.of("jsc.server_router.input", "INPUT");
    static final TextKey NONE = TextKey.of("jsc.server_router.none", "none");
    static final TextKey RACKS = TextKey.of("jsc.server_router.racks", "RACKS");
    static final TextKey RACKS_OF = TextKey.of("jsc.server_router.racks_of", "%s / %s");
    static final TextKey SECTIONS = TextKey.of("jsc.server_router.sections", "SECTIONS");
    static final TextKey NO_SECTIONS = TextKey.of("jsc.server_router.no_sections", "No datacenter sections");
    /* A section's racks and servers. */
    static final TextKey SECTION_SIZE = TextKey.of("jsc.server_router.section_size", "%sR · %sS");

    // The faces, as a section is named and as the input is.
    static final TextKey DOWN = TextKey.of("jsc.server_router.down", "DOWN");
    static final TextKey UP = TextKey.of("jsc.server_router.up", "UP");
    static final TextKey NORTH = TextKey.of("jsc.server_router.north", "NORTH");
    static final TextKey SOUTH = TextKey.of("jsc.server_router.south", "SOUTH");
    static final TextKey WEST = TextKey.of("jsc.server_router.west", "WEST");
    static final TextKey EAST = TextKey.of("jsc.server_router.east", "EAST");
    static final TextKey INPUT_DOWN = TextKey.of("jsc.server_router.input_down", "Down");
    static final TextKey INPUT_UP = TextKey.of("jsc.server_router.input_up", "Up");
    static final TextKey INPUT_NORTH = TextKey.of("jsc.server_router.input_north", "North");
    static final TextKey INPUT_SOUTH = TextKey.of("jsc.server_router.input_south", "South");
    static final TextKey INPUT_WEST = TextKey.of("jsc.server_router.input_west", "West");
    static final TextKey INPUT_EAST = TextKey.of("jsc.server_router.input_east", "East");

    private ServerRouterTexts() {
    }
}
