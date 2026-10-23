/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

/**
 * The formulas of energy J's Industrial uses beyond the Core's: real units, the energy a pumped-storage plant holds
 * and the heat evaporating water carries away. The rate between FE and J's Energy, the loss along a line and a
 * machine's speed short of energy are the Core's (industrial/docs/design/formulas.md).
 *
 * <p>Pure: no game types.
 */
public final class EnergyFormulas {

    /** The joules in a kilowatt-hour. */
    public static final double KILOWATT_HOUR = 3.6e6;
    /** The joules in a megawatt-hour. */
    public static final double MEGAWATT_HOUR = 3.6e9;
    /** The density of water, in kilograms a cubic metre. */
    public static final double WATER_DENSITY = 1_000.0;
    /** The pull of the Overworld's gravity, in metres a second squared. */
    public static final double GRAVITY = 9.81;
    /** The heat a kilogram of water carries away as it evaporates, in joules. */
    public static final double EVAPORATION = 2.26e6;

    private EnergyFormulas() {
    }

    /**
     * The energy, in joules, {@code cubicMetres} of water hold {@code metres} above the lower reservoir: a thousand
     * blocks of water fifty blocks up hold 490 MJ.
     */
    public static double pumpedStorage(final double cubicMetres, final double metres) {
        return WATER_DENSITY * Math.max(0.0, cubicMetres) * GRAVITY * Math.max(0.0, metres);
    }

    /** The heat, in joules, {@code kilograms} of water carry away as they evaporate. */
    public static double evaporativeCooling(final double kilograms) {
        return EVAPORATION * Math.max(0.0, kilograms);
    }
}
