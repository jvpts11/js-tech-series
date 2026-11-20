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
    public static final TextKey SHIP_IN_THE_BAY = TextKey.of("jsspace.shipyard.doors.ship_in_the_bay",
            "A ship stands in the bay: the floor cannot move under it");
    public static final TextKey CANNOT_GO = TextKey.of("jsspace.shipyard.doors.cannot_go",
            "The ship cannot go: %s. The dock closes round it again");
    public static final TextKey NOT_READY = TextKey.of("jsspace.shipyard.doors.not_ready",
            "it is not whole, or its flight console is gone");
    /** What the dock says when it is asked to take the ship in its bay back. */
    public static final TextKey TAKEN_IN = TextKey.of("jsspace.shipyard.doors.taken_in",
            "The dock takes the ship in and closes round it");
    public static final TextKey NO_SHIP = TextKey.of("jsspace.shipyard.doors.no_ship", "No ship stands in the bay");
    public static final TextKey NOT_YOUR_SHIP = TextKey.of("jsspace.shipyard.doors.not_your_ship",
            "Only a pilot of the ship in the bay may have it taken in");
    public static final TextKey CANNOT_TAKE = TextKey.of("jsspace.shipyard.doors.cannot_take",
            "The ship in the bay cannot be set down in the dock");
    /** What the dock does as it opens or closes, as its builder's screen shows it in place of its Release button. */
    public static final TextKey CRANE_BACK = TextKey.of("jsspace.shipyard.doors.crane_back", "Crane rolling back");
    public static final TextKey FLOOR_UP = TextKey.of("jsspace.shipyard.doors.floor_up", "Floor rising");
    public static final TextKey CRADLE_FOLDS = TextKey.of("jsspace.shipyard.doors.cradle_folds",
            "Cradle folding down");
    public static final TextKey OPEN = TextKey.of("jsspace.shipyard.doors.open", "Dock open");
    public static final TextKey CRADLE_HOLDS = TextKey.of("jsspace.shipyard.doors.cradle_holds", "Cradle rising");
    public static final TextKey FLOOR_DOWN = TextKey.of("jsspace.shipyard.doors.floor_down", "Floor lowering");
    public static final TextKey CRANE_IN = TextKey.of("jsspace.shipyard.doors.crane_in", "Crane rolling in");
    public static final TextKey CLOSED = TextKey.of("jsspace.shipyard.doors.closed", "Dock closed");

    private ShipyardDoorTexts() {
    }

    /** What the dock does standing or moving as {@code state}, in a few words. */
    public static TextKey of(final ShipyardDoors.State state) {
        return switch (state) {
            case CLOSED -> CLOSED;
            case CRANE_BACK -> CRANE_BACK;
            case FLOOR_UP -> FLOOR_UP;
            case CRADLE_FOLDS -> CRADLE_FOLDS;
            case OPEN -> OPEN;
            case CRADLE_HOLDS -> CRADLE_HOLDS;
            case FLOOR_DOWN -> FLOOR_DOWN;
            case CRANE_IN -> CRANE_IN;
        };
    }
}
