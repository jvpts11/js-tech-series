/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

/**
 * The thumbwheels of the Sides tab, which set what a machine pushes and pulls a second: three drums of digits, each
 * turned up or down on its own and going round from nine to nought, as a real thumbwheel's do. A wheel counts items
 * one by one, and liquids, matter and energy in hundreds.
 */
public final class MachineWheels {

    /** What one step of the units' drum is worth for liquids, matter and energy. */
    public static final int HUNDREDS = 100;
    private static final int DIGITS = 3;
    private static final int TEN = 10;

    private MachineWheels() {
    }

    /** What one step of the units' drum is worth for {@code resource}. */
    public static int unit(final MachineResource resource) {
        return resource == MachineResource.ITEMS ? 1 : HUNDREDS;
    }

    /** The digits {@code value} shows on the wheel, nought-padded, of no more than three. */
    public static String shown(final int value) {
        final int held = Math.max(0, Math.min(MachineMenu.WHEEL_MOST, value));
        final String digits = String.valueOf(held);
        return "0".repeat(DIGITS - digits.length()) + digits;
    }

    /**
     * {@code value} with drum {@code digit} (0 the hundreds, 1 the tens, 2 the units) turned one step up or down,
     * going round on its own without touching the others; never below one, the least a machine moves.
     */
    public static int turn(final int value, final int digit, final boolean up) {
        final int held = Math.max(0, Math.min(MachineMenu.WHEEL_MOST, value));
        final int weight = (int) Math.pow(TEN, DIGITS - 1 - Math.max(0, Math.min(DIGITS - 1, digit)));
        final int drum = held / weight % TEN;
        final int next = Math.floorMod(drum + (up ? 1 : -1), TEN);
        return Math.max(1, held - drum * weight + next * weight);
    }
}
