/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.telemetry;

/**
 * How big a dish is, which gives how far it hears and how fast: the small one for the low orbits and the weather, the
 * medium one for the geostationary orbit and the Moon, the large one for deep space, as the 70 m antennas of the Deep
 * Space Network. The data rates are estimates, to be set by playing.
 *
 * <p>Pure: no game types.
 */
public enum AntennaSize {

    SMALL(Reach.NEAR, 1),
    MEDIUM(Reach.FAR, 4),
    LARGE(Reach.DEEP, 16);

    private final Reach reach;
    private final int rate;

    AntennaSize(final Reach reach, final int rate) {
        this.reach = reach;
        this.rate = rate;
    }

    /** How far it hears. */
    public Reach reach() {
        return this.reach;
    }

    /** How much data it brings down a second, in the units the data is counted in. */
    public int rate() {
        return this.rate;
    }

    /** How far an antenna hears, from the nearest to the farthest. */
    public enum Reach {

        /** The low orbits of its own world. */
        NEAR(1),
        /** Its world's geostationary orbit, and the Moon. */
        FAR(2),
        /** The other worlds. */
        DEEP(3);

        private final int rank;

        Reach(final int rank) {
            this.rank = rank;
        }

        /** Whether this reach hears as far as {@code wanted}. */
        public boolean covers(final Reach wanted) {
            return this.rank >= wanted.rank;
        }
    }
}
