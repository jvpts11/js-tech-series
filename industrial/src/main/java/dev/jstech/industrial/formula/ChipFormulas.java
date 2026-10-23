/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

/**
 * The formulas of chips: how many dies a wafer gives, by the wafer's area over the die's less the ones its round edge
 * cuts through, so a 300 mm wafer gives about forty times the chips of a 50 mm one; how many of them are good, the
 * share falling as the defects the room's particles leave rise; and the particles each ISO class of clean room allows
 * (industrial/docs/design/formulas.md).
 *
 * <p>Pure: no game types.
 */
public final class ChipFormulas {

    /*
     * The particles of half a micrometre and larger a cubic metre holds at the limit of an ISO class, over ten to the
     * class: ISO 14644-1's (0.1 / 0.5) to the power 2.08.
     */
    private static final double HALF_MICROMETRE = 0.035_2;

    private ChipFormulas() {
    }

    /**
     * The whole dies of {@code dieMm2} a round wafer of {@code waferMm} across gives: its area over the die's, less
     * the dies its edge cuts through, the industry's formula.
     */
    public static long diesPerWafer(final double waferMm, final double dieMm2) {
        if (!(waferMm > 0.0) || !(dieMm2 > 0.0)) {
            return 0L;
        }
        final double radius = waferMm / 2.0;
        final double dies = Math.PI * radius * radius / dieMm2 - Math.PI * waferMm / Math.sqrt(2.0 * dieMm2);
        return Math.max(0L, (long) Math.floor(dies));
    }

    /**
     * The share of good dies of {@code dieMm2}, with {@code defectsPerMm2} defects on the wafer: the chance a die
     * holds none, as defects fall at random, the Poisson model of the industry.
     */
    public static double goodShare(final double dieMm2, final double defectsPerMm2) {
        return Math.exp(-Math.max(0.0, dieMm2) * Math.max(0.0, defectsPerMm2));
    }

    /**
     * The particles of half a micrometre and larger a cubic metre of a room of ISO class {@code isoClass} holds at
     * most: ten times more for each class dirtier, 3,520 at class 5.
     */
    public static double particlesAllowed(final int isoClass) {
        return HALF_MICROMETRE * Math.pow(10.0, isoClass);
    }
}
