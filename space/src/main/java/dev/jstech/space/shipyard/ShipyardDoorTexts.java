/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Small Shipyard tells the pilot as it opens round their ship and closes round it. */
@TextHolder
public final class ShipyardDoorTexts {

    public static final TextKey OPENING = TextKey.of("jsspace.shipyard.doors.opening",
            "The dock opens: the ship goes once it stands on its gear");
    public static final TextKey MOVING = TextKey.of("jsspace.shipyard.doors.moving",
            "The dock is moving: wait until it stands");
    public static final TextKey CRANE_IN_THE_WAY = TextKey.of("jsspace.shipyard.doors.crane_in_the_way",
            "Something stands in the crane's way at %s");
    public static final TextKey FLOOR_IN_THE_WAY = TextKey.of("jsspace.shipyard.doors.floor_in_the_way",
            "Something stands in the floor's way at %s");
    public static final TextKey SOMEONE_IN_THE_PIT = TextKey.of("jsspace.shipyard.doors.someone_in_the_pit",
            "Someone is in the pit: the floor cannot move");
    public static final TextKey SHIP_IN_THE_BAY = TextKey.of("jsspace.shipyard.doors.ship_in_the_bay",
            "A ship stands in the bay: the floor cannot move under it");
    public static final TextKey CANNOT_GO = TextKey.of("jsspace.shipyard.doors.cannot_go",
            "The ship cannot go: %s. The dock closes round it again");
    public static final TextKey NOT_READY = TextKey.of("jsspace.shipyard.doors.not_ready",
            "it is not whole, or its flight console is gone");

    private ShipyardDoorTexts() {
    }
}
