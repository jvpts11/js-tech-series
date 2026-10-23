/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

/**
 * The formulas of science and nuclear power: a research group's power, a synchrotron's reach, the heat a reactor
 * keeps giving off after it stops, and how far uranium may be enriched (industrial/docs/design/formulas.md).
 *
 * <p>Pure: no game types.
 */
public final class ScienceFormulas {

    /** The share of each CRC's power after the first a research group gets. */
    public static final double GROUP_SHARE = 0.9;
    /** The momentum, in GeV/c, a particle of unit charge has on a ring of one metre in a field of one tesla. */
    public static final double MOMENTUM_PER_TESLA_METRE = 0.299_79;
    /** The share of its power a reactor gives off as heat the second after it stops. */
    public static final double DECAY_HEAT = 0.066;
    /** How fast that heat falls: as the time since it stopped to this power. */
    public static final double DECAY_FALL = -0.2;
    /** The least uranium-235 a power reactor's fuel holds. */
    public static final double POWER_LEAST = 0.03;
    /** The most a power reactor's fuel holds. */
    public static final double POWER_MOST = 0.05;
    /** The most any reactor's fuel holds: a research reactor's; nothing above is made. */
    public static final double RESEARCH_MOST = 0.20;

    private ScienceFormulas() {
    }

    /** A research group's power: its first CRC's, and 90% of each other's. */
    public static double researchGroup(final double... powers) {
        double total = 0.0;
        for (int i = 0; i < powers.length; i++) {
            total += (i == 0 ? 1.0 : GROUP_SHARE) * Math.max(0.0, powers[i]);
        }
        return total;
    }

    /**
     * The momentum, in GeV/c, a synchrotron of {@code radiusMetres} with magnets of {@code tesla} holds a proton at:
     * a larger ring, or a stronger field, reaches further.
     */
    public static double synchrotronMomentum(final double tesla, final double radiusMetres) {
        return MOMENTUM_PER_TESLA_METRE * Math.max(0.0, tesla) * Math.max(0.0, radiusMetres);
    }

    /**
     * The heat, in the units of {@code power}, a reactor that ran at {@code power} gives off {@code seconds} after it
     * stopped: about 7% at once, a little over 1% an hour on, half a percent a day on, as the Way-Wigner formula has
     * it.
     */
    public static double decayHeat(final double power, final double seconds) {
        return Math.max(0.0, power) * DECAY_HEAT * Math.pow(Math.max(1.0, seconds), DECAY_FALL);
    }

    /** Whether fuel holding {@code share} of uranium-235 may be made: up to a research reactor's 20%, no more. */
    public static boolean enrichmentAllowed(final double share) {
        return share >= 0.0 && share <= RESEARCH_MOST;
    }
}
