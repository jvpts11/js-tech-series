/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.tier.HardwareEra;

/**
 * Which case a small computer (a Personal Computer, a Crafting Computer, a Cluster Management Computer) comes in. The
 * Vintage, Legacy and Transition ages had one tower each; from the Standard age on each machine comes in three cases,
 * a block each, that differ in look alone: the case brings its own cooler and fans, and everything else in it is the
 * player's.
 */
public enum CaseStyle implements IStableName {

    /** The one case of an age that had only one. */
    SOLE("sole"),

    /** Closed and quiet, with no light: the case for whoever wants none of the rest. */
    NEUTRAL("neutral"),

    /** The neutral case's sobriety with the look of airflow: mesh, more fans and a heavier cooler. */
    HIGH_PERFORMANCE("high_performance"),

    /** Glass and light. */
    AESTHETIC("aesthetic");

    private final String serializedName;

    CaseStyle(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** How the models name a case of this style in {@code era}: the age alone, or the age and then the style. */
    public String caseName(final HardwareEra era) {
        return this == SOLE ? era.serializedName() : era.serializedName() + "_" + this.serializedName;
    }
}
