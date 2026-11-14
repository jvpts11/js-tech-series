/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.landing;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a ship's pilot is told of where the ship may set down. */
@TextHolder
public final class ShipLandingTexts {

    public static final TextKey RUNWAY_BLOCKED = TextKey.of("jsspace.ship_landing.runway_blocked",
            "The runway past the beacon is not clear: no landing there");
    public static final TextKey CANNOT_HOVER = TextKey.of("jsspace.ship_landing.cannot_hover",
            "Too little air here for the nozzles, and no thrusters to hold the ship up: no landing");

    private ShipLandingTexts() {
    }
}
