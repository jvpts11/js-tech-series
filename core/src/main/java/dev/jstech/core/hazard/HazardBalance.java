/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

/**
 * The world's rules for its hazards, as its settings file gives them: whether minerals with uranium or thorium in them
 * are naturally radioactive.
 */
public final class HazardBalance {

    private static volatile boolean naturalRadioactivity = true;

    private HazardBalance() {
    }

    /** Whether minerals with uranium or thorium in them are naturally radioactive. */
    public static boolean naturalRadioactivity() {
        return naturalRadioactivity;
    }

    public static void setNaturalRadioactivity(final boolean value) {
        naturalRadioactivity = value;
    }
}
