/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a Rocket Hangar's design screen says of a design: where it can go, and what it lacks or has. */
@TextHolder
public final class DesignTexts {

    public static final TextKey LOW_ORBIT = TextKey.of("jsspace.design.low_orbit", "Low Orbit");
    public static final TextKey OVERWORLD = TextKey.of("jsspace.design.overworld", "The Overworld");
    public static final TextKey MOON = TextKey.of("jsspace.design.moon", "The Moon");
    public static final TextKey MARS = TextKey.of("jsspace.design.mars", "Mars");
    public static final TextKey NO_PARTS = TextKey.of("jsspace.design.no_parts", "No parts yet");
    public static final TextKey MISFIT = TextKey.of("jsspace.design.misfit", "Part %s does not fit");
    public static final TextKey NO_ENGINE = TextKey.of("jsspace.design.no_engine", "S%s has no engine");
    public static final TextKey NO_PROPELLANT = TextKey.of("jsspace.design.no_propellant", "S%s has no propellant");
    public static final TextKey STAYS_DOWN = TextKey.of("jsspace.design.stays_down", "Too weak to lift off");
    public static final TextKey TOO_TALL = TextKey.of("jsspace.design.too_tall", "%s tall, the bay %s");
    public static final TextKey TOO_WIDE = TextKey.of("jsspace.design.too_wide", "%s wide, the bay %s");
    public static final TextKey NO_SEATS = TextKey.of("jsspace.design.no_seats", "No crew seats");
    public static final TextKey SEATS = TextKey.of("jsspace.design.seats", "Crew: %s seats");
    public static final TextKey SEAT_ONE = TextKey.of("jsspace.design.seat_one", "Crew: 1 seat");
    public static final TextKey NO_SHIELD = TextKey.of("jsspace.design.no_shield", "No heat shield for air");
    public static final TextKey SHIELD = TextKey.of("jsspace.design.shield", "Heat shield on board");
    public static final TextKey TANKS = TextKey.of("jsspace.design.tanks", "Tanks: fill at the pad");
    public static final TextKey LAUNCHER = TextKey.of("jsspace.design.launcher", "Stands on a mobile launcher");
    public static final TextKey NO_LAUNCHER = TextKey.of("jsspace.design.no_launcher",
            "No room for a mobile launcher: on the floor");

    private DesignTexts() {
    }
}
