/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;

/**
 * A processor's cooler (computers/docs/design/temperature.md): it sits over a processor's socket and takes away the
 * heat it is rated for, which a processor with no cooler lacks. An all-in-one water cooler's radiator takes two of the
 * case's fan seats besides.
 *
 * @param era            the era it is made in: a case seats a cooler of its own era or an earlier one
 * @param kind           what kind of cooler it is
 * @param takesAwayWatts the heat it takes away, in watts
 */
@ApiStatus.Experimental
public record CoolerSpec(Era era, Kind kind, int takesAwayWatts) {

    public CoolerSpec {
        Objects.requireNonNull(era, "era");
        Objects.requireNonNull(kind, "kind");
        if (takesAwayWatts < 0) {
            throw new IllegalArgumentException("a cooler takes away no less than nothing");
        }
    }

    /** How many of the case's fan seats its radiator takes: two for a water cooler, none for the others. */
    public int radiatorFans() {
        return kind.radiatorFans();
    }

    /** What kind of cooler: the one a processor comes with, a tower, or an all-in-one water cooler. */
    public enum Kind {
        /** The cooler a processor comes with: a bare heatsink in the Vintage, a small fan on a sink after. */
        STOCK(0),
        /** The tall fin stack with its fan on the side and the heat pipes out of its top. */
        TOWER(0),
        /** A pump over the processor and a radiator at two of the case's fan seats. */
        WATER(2);

        private final int radiatorFans;

        Kind(final int radiatorFans) {
            this.radiatorFans = radiatorFans;
        }

        /** How many fan seats its radiator takes. */
        public int radiatorFans() {
            return radiatorFans;
        }
    }
}
