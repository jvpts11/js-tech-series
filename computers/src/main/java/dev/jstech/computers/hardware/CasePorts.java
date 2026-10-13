/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.HardwareEra;

/**
 * How many USB ports the case of each era has on its front, to the left of it, where a USB flash drive plugs in. The
 * Vintage and Legacy towers had none there; from the Transition on a case has two, low on the left of its front where
 * a monitor standing on it or a wall it is built into never covers them.
 */
public final class CasePorts {

    private CasePorts() {
    }

    /** The USB ports on the front of that era's case. */
    public static int of(final HardwareEra era) {
        return switch (era) {
            case VINTAGE, LEGACY -> 0;
            case TRANSITION, STANDARD, ADVANCED, EXA, SINGULARITY -> 2;
        };
    }
}
