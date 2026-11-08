/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * What a cabinet can take away from the machines in it (computers/docs/design/temperature.md): a certain amount on its
 * own, more for each Cooling Unit mounted in it, and a little more for each Vent Panel. The machines make heat in
 * proportion to what they draw, and the cabinet has one temperature for all of them, which follows the same thresholds
 * as every computer's ({@code ThermalModel}): past them every machine in it gives up the same share of its clock.
 *
 * <p>This is arithmetic over the budget, deliberately: the walking of the slots that produces it belongs to the
 * cabinet, and keeping the sums apart from it is what lets them be tested directly.
 */
public final class RackThermals {

    /** What a cabinet sheds with no help, in watts. */
    public static final int PASSIVE_BUDGET_W = 1000;
    /** What a Vent Panel adds, in watts: the cheap partner of the Cooling Unit. */
    public static final int VENT_PANEL_W = 500;

    /**
     * What a Cooling Unit mounted in a cabinet sheds on top of that, by its era from the Vintage's to the Advanced's:
     * each age's fans and coolers move more heat than the last's. Balancing estimates.
     */
    private static final int[] COOLING_UNIT_W = {300, 600, 1_000, 1_500, 2_500};

    private RackThermals() {
    }

    /** What a Cooling Unit of {@code era} sheds; an era past the Advanced, the Advanced one's. */
    public static int coolingUnitWatts(final Era era) {
        return COOLING_UNIT_W[HardwareEra.partsIndex(era)];
    }

    /** The heat a cabinet can shed with units mounted in it that shed {@code coolingWatts} in all. */
    public static int budgetWatts(final int coolingWatts) {
        return PASSIVE_BUDGET_W + Math.max(0, coolingWatts);
    }
}
