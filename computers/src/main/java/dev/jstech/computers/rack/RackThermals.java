/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

/**
 * How hot a cabinet runs, and what that costs the machines in it.
 *
 * <p>A cabinet sheds a certain amount of heat on its own, and each Cooling Unit mounted in it sheds more.
 * The machines inside make heat in proportion to what they draw. While the heat made is within what can be
 * shed, everything runs at full speed; past that, every machine in the cabinet gives up the same share of
 * its capacity, so the one answer applies to all of them rather than picking a victim.
 *
 * <p>There is a floor. Hot hardware slows down; it does not stop. A cabinet can be packed far past what it
 * can cool and will still deliver a quarter of what it would cold, which keeps a badly built rack slow and
 * annoying rather than dead and confusing.
 *
 * <p>This is arithmetic over two numbers, deliberately: the walking of the slots that produces them belongs
 * to the cabinet, and keeping the sums apart from it is what lets the awkward cases be tested directly.
 */
public final class RackThermals {

    /** What a cabinet sheds with no help, in watts. */
    public static final int PASSIVE_BUDGET_W = 1000;

    /** What each Cooling Unit mounted in it sheds on top of that. */
    public static final int COOLING_UNIT_BUDGET_W = 1500;

    /** However hot it gets, a machine keeps this share of its capacity. */
    public static final int MIN_THROTTLE_PERCENT = 25;

    private RackThermals() {
    }

    /** The heat a cabinet can shed with {@code coolingUnits} Cooling Units mounted in it. */
    public static int budgetWatts(final int coolingUnits) {
        return PASSIVE_BUDGET_W + Math.max(0, coolingUnits) * COOLING_UNIT_BUDGET_W;
    }

    /**
     * How much of its capacity a machine in the cabinet delivers, in percent. Within budget it is all of
     * it; past budget it falls in proportion to how far past, and never below the floor.
     *
     * <p>A cabinet drawing nothing is not throttled, which also keeps the division below from ever being
     * by zero.
     */
    public static int throttlePercent(final int loadWatts, final int budgetWatts) {
        if (loadWatts <= budgetWatts || loadWatts <= 0) {
            return 100;
        }
        return Math.max(MIN_THROTTLE_PERCENT, (int) (100L * budgetWatts / loadWatts));
    }

    /** Whether the cabinet is past its budget and holding the machines in it back. */
    public static boolean throttled(final int loadWatts, final int budgetWatts) {
        return throttlePercent(loadWatts, budgetWatts) < 100;
    }
}
