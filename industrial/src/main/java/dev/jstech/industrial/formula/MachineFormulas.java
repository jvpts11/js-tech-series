/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

/**
 * The formulas of J's Industrial's machines and their upgrades: the slots a machine has by its tier, and what Motor
 * Upgrades, Variable Frequency Drives, Overdrives and Parallel Lines do together. Speeds add up; savings multiply; the
 * energy grows with the recipes worked at once (industrial/docs/design/formulas.md).
 *
 * <p>Pure: no game types.
 */
public final class MachineFormulas {

    /** What a Motor Upgrade adds to the speed. */
    public static final double MOTOR_SPEED = 0.5;
    /** What a Motor Upgrade adds to the energy. */
    public static final double MOTOR_ENERGY = 0.75;
    /** What an Overdrive adds to the speed. */
    public static final double OVERDRIVE_SPEED = 1.0;
    /** What an Overdrive multiplies the energy by. */
    public static final double OVERDRIVE_ENERGY = 2.0;
    /** What a Variable Frequency Drive multiplies the energy by. */
    public static final double DRIVE_ENERGY = 0.8;
    /** The recipes at once an Advanced Parallel Line adds; a Parallel Line adds one. */
    public static final int ADVANCED_PARALLEL = 2;

    private static final int SMALL_SLOTS = 2;
    private static final int MIDDLE_SLOTS = 3;
    private static final int LARGE_SLOTS = 4;
    private static final int FIRST_LARGE_TIER = 4;
    private static final int FIRST_MIDDLE_TIER = 2;

    private MachineFormulas() {
    }

    /** The upgrade slots of a machine of {@code tier}: none at T0, 2 at T1, 3 at T2 and T3, 4 from T4. */
    public static int upgradeSlots(final int tier) {
        if (tier >= FIRST_LARGE_TIER) {
            return LARGE_SLOTS;
        }
        if (tier >= FIRST_MIDDLE_TIER) {
            return MIDDLE_SLOTS;
        }
        return tier >= 1 ? SMALL_SLOTS : 0;
    }

    /** How many times as fast a machine works: 1 + 0.5 a Motor Upgrade + 1 an Overdrive. */
    public static double speed(final int motors, final int overdrives) {
        return 1.0 + MOTOR_SPEED * motors + OVERDRIVE_SPEED * overdrives;
    }

    /**
     * How many times the energy a tick it spends: (1 + 0.75 a Motor Upgrade) × 0.8 a Variable Frequency Drive × 2 an
     * Overdrive.
     */
    public static double energy(final int motors, final int drives, final int overdrives) {
        return (1.0 + MOTOR_ENERGY * motors) * Math.pow(DRIVE_ENERGY, drives) * Math.pow(OVERDRIVE_ENERGY,
                overdrives);
    }

    /** How many times the energy each thing it makes costs: the energy a tick over the speed. */
    public static double energyPerItem(final int motors, final int drives, final int overdrives) {
        return energy(motors, drives, overdrives) / speed(motors, overdrives);
    }

    /** How many recipes it works at once: 1 + 1 a Parallel Line + 2 an Advanced Parallel Line. */
    public static int recipesAtOnce(final int lines, final int advancedLines) {
        return 1 + lines + ADVANCED_PARALLEL * advancedLines;
    }
}
