/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import dev.jstech.core.air.AirMix;
import java.util.Optional;

/**
 * What a dimension is like at a height, its rules and its bands worked out there ({@link DimensionRules#at}).
 *
 * @param pressure    its pressure, in kilopascals
 * @param temperature its temperature, in degrees Celsius
 * @param air         its air, when the rules say what it is made of
 * @param breathable  whether it can be breathed: by the oxygen's partial pressure when the air is said, else as the
 *                    rules say
 */
public record Climate(double pressure, double temperature, Optional<AirMix> air, boolean breathable) {

    /** Whether a player not made for it burns here. */
    public boolean scorching() {
        return this.temperature > DimensionRules.SCORCHING;
    }

    /** Whether a player not made for it freezes here. */
    public boolean freezing() {
        return this.temperature < DimensionRules.FREEZING;
    }
}
