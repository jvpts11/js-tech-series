/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a starship's fixtures tell the player who uses them. */
@TextHolder
public final class ShipFixtureTexts {

    public static final TextKey SUIT_LOCKER = TextKey.of("jsspace.ship_fixture.suit_locker", "Suit Locker");
    public static final TextKey NO_SUIT = TextKey.of("jsspace.ship_fixture.no_suit",
            "Wear a suit to fill its tanks");
    public static final TextKey FILLED = TextKey.of("jsspace.ship_fixture.filled",
            "%s seconds of oxygen in the suit's tanks");
    public static final TextKey FULL = TextKey.of("jsspace.ship_fixture.full", "The suit's tanks are full");
    public static final TextKey NO_OXYGEN = TextKey.of("jsspace.ship_fixture.no_oxygen",
            "No oxygen in the cabin's air to fill from");
    public static final TextKey RESPAWN_SET = TextKey.of("jsspace.ship_fixture.respawn_set",
            "You will wake in this bunk");
    public static final TextKey NO_AIR = TextKey.of("jsspace.ship_fixture.no_air",
            "No air to breathe here: the bunk needs a sealed cabin");

    private ShipFixtureTexts() {
    }
}
