/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

/**
 * The standard atomic weight of each of the 118 elements, in daltons, as IUPAC gives them in short; for an element with
 * no stable isotope, the mass number of its longest-lived one, as tables print it in brackets. What a formula's mass
 * is worked out from.
 *
 * <p>Pure: no game types.
 */
final class ElementWeights {

    /* By atomic number; the first is no element. */
    private static final double[] WEIGHTS = {
        0.0,
        1.008, 4.0026, 6.94, 9.0122, 10.81, 12.011, 14.007, 15.999, 18.998, 20.180,
        22.990, 24.305, 26.982, 28.085, 30.974, 32.06, 35.45, 39.95, 39.098, 40.078,
        44.956, 47.867, 50.942, 51.996, 54.938, 55.845, 58.933, 58.693, 63.546, 65.38,
        69.723, 72.630, 74.922, 78.971, 79.904, 83.798, 85.468, 87.62, 88.906, 91.224,
        92.906, 95.95, 98.0, 101.07, 102.91, 106.42, 107.87, 112.41, 114.82, 118.71,
        121.76, 127.60, 126.90, 131.29, 132.91, 137.33, 138.91, 140.12, 140.91, 144.24,
        145.0, 150.36, 151.96, 157.25, 158.93, 162.50, 164.93, 167.26, 168.93, 173.05,
        174.97, 178.49, 180.95, 183.84, 186.21, 190.23, 192.22, 195.08, 196.97, 200.59,
        204.38, 207.2, 208.98, 209.0, 210.0, 222.0, 223.0, 226.0, 227.0, 232.04,
        231.04, 238.03, 237.0, 244.0, 243.0, 247.0, 247.0, 251.0, 252.0, 257.0,
        258.0, 259.0, 266.0, 267.0, 268.0, 269.0, 270.0, 269.0, 278.0, 281.0,
        282.0, 285.0, 286.0, 289.0, 290.0, 293.0, 294.0, 294.0
    };

    private ElementWeights() {
    }

    /** The atomic weight of the element of atomic number {@code number}, from 1 to 118. */
    static double of(final int number) {
        if (number < 1 || number >= WEIGHTS.length) {
            throw new IllegalArgumentException("there is no element " + number + " of the known table");
        }
        return WEIGHTS[number];
    }

    /** How many elements it holds the weight of: 118. */
    static int count() {
        return WEIGHTS.length - 1;
    }
}
