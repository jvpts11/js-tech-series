/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import java.util.Optional;

/**
 * The five voltage classes of J's Energy, as real grids are classed, from the socket to the lines of thousands of
 * kilometres. Voltage is how energy travels, not how the game progresses: there is no amperage and no machine per
 * voltage. Pure: no game types.
 */
public enum VoltageClass {

    /** 400 V: up to 1 kV, the socket and the factory. */
    LOW("low", 1, 400L, false),
    /** 11 kV: 1 to 35 kV, the neighbourhood's grid and the big factories. */
    MEDIUM("medium", 2, 11_000L, false),
    /** 138 kV: 35 to 230 kV, regional transmission. */
    HIGH("high", 3, 138_000L, false),
    /** 500 kV: 230 to 800 kV, national transmission. */
    EXTRA_HIGH("extra_high", 4, 500_000L, false),
    /** ±1,100 kV, direct current: above 800 kV, lines of thousands of kilometres. */
    ULTRA_HIGH("ultra_high", 5, 1_100_000L, true);

    private final String path;
    private final int rank;
    private final long volts;
    private final boolean directCurrent;

    VoltageClass(final String path, final int rank, final long volts, final boolean directCurrent) {
        this.path = path;
        this.rank = rank;
        this.volts = volts;
        this.directCurrent = directCurrent;
    }

    /** Its name in ids, as its grid is named. */
    public String path() {
        return this.path;
    }

    /** Its nominal voltage, in volts. */
    public long volts() {
        return this.volts;
    }

    /** Whether it is direct current, as the ultra-high class is. */
    public boolean directCurrent() {
        return this.directCurrent;
    }

    /**
     * What joins its lines to those of {@code other}: a transformer between neighbouring classes of alternating
     * current, an HVDC converter station between the extra-high class and the direct current of the ultra-high one;
     * nothing between classes further apart, or a class and itself.
     */
    public Optional<GridEquipment> joinedTo(final VoltageClass other) {
        if (Math.abs(this.rank - other.rank) != 1) {
            return Optional.empty();
        }
        return Optional.of(this.directCurrent || other.directCurrent ? GridEquipment.HVDC_STATION
                : GridEquipment.TRANSFORMER);
    }
}
