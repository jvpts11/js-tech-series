/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * How many 5.25-inch bays the case of each era has on its front, where an internal drive or the hot-swap bay goes.
 * The beige AT tower of the Vintage age had one, the ATX towers of the Legacy and Transition ages two, a Standard case
 * keeps one high on its front, and the Advanced cases dropped them, as cases did once the optical disc went out of use.
 * The Personal Computer, the Crafting Computer and the Cluster Management Computer share their era's case, so they
 * share its bays.
 */
public final class CaseBays {

    private CaseBays() {
    }

    /** The 5.25-inch bays on the front of that era's case. */
    public static int of(final HardwareEra era) {
        return switch (era) {
            case VINTAGE, STANDARD -> 1;
            case LEGACY, TRANSITION -> 2;
            case ADVANCED, EXA, SINGULARITY -> 0;
        };
    }
}
