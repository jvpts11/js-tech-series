/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

/**
 * The world's rules for the consequences of mechanical power, as its settings file gives them: whether a part with a
 * rating breaks when the torque through it passes the rating. A shear pin shears either way: that is what it is for.
 */
public final class MechanicalBalance {

    private static volatile boolean partsBreak = true;

    private MechanicalBalance() {
    }

    /** Whether a part with a rating breaks when the torque through it passes the rating. */
    public static boolean partsBreak() {
        return partsBreak;
    }

    public static void setPartsBreak(final boolean value) {
        partsBreak = value;
    }
}
