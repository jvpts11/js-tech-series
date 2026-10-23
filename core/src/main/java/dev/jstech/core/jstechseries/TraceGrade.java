/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

/**
 * The grade a trace element has in a mineral in the series' games. A real chalcopyrite holds gold in parts per
 * million, which no player would ever see come out; as the air's rare gases are counted at a thousand times their real
 * share, a trace is counted at a thousand times its real grade, so the rarest stay rare and are still there to find.
 * No trace is counted at more than a tenth of its mineral: a few percent of titanium in magnetite is no trace, and
 * would otherwise come out ten times over.
 *
 * <pre>{@code
 * TraceGrade.share(0.000_01)   // gold in chalcopyrite, ten grams a tonne: 0.01 of the mineral
 * }</pre>
 *
 * <p>Pure: no game types.
 */
public final class TraceGrade {

    /** How many times its real grade a trace is counted at. */
    public static final double BOOST = 1_000.0;
    /** The most of its mineral a trace is counted at. */
    public static final double MOST = 0.1;

    private TraceGrade() {
    }

    /**
     * The share of its mineral a trace of real grade {@code realGrade}, a share of the mineral's mass, is counted at.
     *
     * @throws IllegalArgumentException when the grade is not above none and below the whole
     */
    public static double share(final double realGrade) {
        if (!(realGrade > 0.0 && realGrade < 1.0)) {
            throw new IllegalArgumentException("a trace is a share of its mineral above none, not " + realGrade);
        }
        return Math.min(MOST, realGrade * BOOST);
    }
}
