/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

/**
 * A machine's heat kept as a budget, in watts, rather than as a body of heat: the heat its work makes now, the heat
 * it can take away from itself (its casing, its fans, its coolers), how hot that leaves it, and how much of its work
 * it still does. A computer is one: the heat its parts make against what its case and coolers take away, slowing
 * down when it is over. Offered on {@link ThermalBudgets#BUDGET}, for whatever reads or shows a machine's heat.
 *
 * <pre>{@code
 * IThermalBudget rack = ThermalBudgets.at(level, pos, null);
 * if (rack != null && rack.over()) {
 *     // the rack makes more heat than it takes away, and works at rack.performance() of its speed
 * }
 * }</pre>
 */
public interface IThermalBudget {

    /** The heat its work makes now, in watts. */
    double loadWatts();

    /** The heat it can take away from itself, in watts. */
    double budgetWatts();

    /**
     * How hot it is, in kelvin; not a number for a machine that keeps only its budget and no temperature of its
     * own.
     */
    double temperature();

    /** The share of its full work it does now, from 0 to 1: all of it while it keeps within its budget. */
    double performance();

    /** Whether it makes more heat than it takes away. */
    default boolean over() {
        return loadWatts() > budgetWatts();
    }
}
