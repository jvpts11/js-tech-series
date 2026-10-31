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

/** What a ship in flight is called on: the key that boosts it. */
@TextHolder
public final class ShipFlightTexts {

    public static final TextKey BOOST_KEY = TextKey.of("key.jsspace.ship_boost", "Boost the ship");

    private ShipFlightTexts() {
    }
}
