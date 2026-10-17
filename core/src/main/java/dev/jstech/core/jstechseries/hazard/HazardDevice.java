/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

/** The hazard devices, one family of yellow cases with black faces. */
public enum HazardDevice {

    /** The Geiger counter: the rate here, on a needle over a scale of tens, and a light for every click. */
    GEIGER_COUNTER,
    /** The pocket dosimeter: the dose taken, its band and the bands reached. */
    POCKET_DOSIMETER,
    /** The hazard map: the land around with a hazard on it, chunk by chunk, live. */
    HAZARD_MAP
}
