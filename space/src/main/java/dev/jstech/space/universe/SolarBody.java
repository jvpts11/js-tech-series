/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.space.orbit.Orbit;
import dev.jstech.space.orbit.OrbitScale;
import dev.jstech.space.orbit.SolarSystemOrbits;
import org.jetbrains.annotations.Nullable;

/**
 * The bodies of the Solar System the skeleton draws in its space: the Overworld and Mars round the Sun at their real
 * distances, and the Moon round the Overworld, its same face always turned to it. Each turns at its own pace: the
 * Overworld once a day, Mars once a sol; the Moon once an orbit, as its face is locked.
 *
 * <p>Pure: no game types.
 */
public enum SolarBody {

    OVERWORLD(SphereMap.EARTH_DIAMETER, SolarSystemOrbits.EARTH, OrbitScale.DAY_TICKS),
    MOON(3_474.8, null, 0),
    MARS(6_779, SolarSystemOrbits.MARS, 24_660);

    private final double diameter;
    @Nullable
    private final Orbit orbit;
    private final double dayTicks;

    SolarBody(final double diameter, @Nullable final Orbit orbit, final double dayTicks) {
        this.diameter = diameter;
        this.orbit = orbit;
        this.dayTicks = dayTicks;
    }

    /** Its width, in kilometres. */
    public double diameter() {
        return this.diameter;
    }

    /** Its path round the Sun, or none for the Moon, which goes round the Overworld. */
    public @Nullable Orbit orbit() {
        return this.orbit;
    }

    /** How long it takes to turn once, in ticks; 0 for the Moon, which turns once an orbit. */
    public double dayTicks() {
        return this.dayTicks;
    }
}
