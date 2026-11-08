/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * Which case a small computer (a Personal Computer, a Crafting Computer, a Cluster Management Computer) comes in. The
 * Vintage, Legacy and Transition ages had one tower each; from the Standard age on each machine comes in three cases,
 * a block each, that differ in look and in how much they cool (computers/docs/design/temperature.md): what the case
 * takes away on its own and how many fan seats it has. Its coolers and fans are the player's, as everything else in it.
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

    /**
     * How the models name a case of this style in {@code era}: the age alone, or the age and then the style; an era
     * after the Advanced has the Advanced's case.
     */
    public String caseName(final Era era) {
        final String age = HardwareEra.withParts(era).path();
        return this == SOLE ? age : age + "_" + this.serializedName;
    }

    /**
     * How a case of this style in {@code era} cools: the sole tower of its age, or the style's from the Standard on. A
     * sole case past the Transition cools as the Neutral one, the case for whoever wants none of the rest.
     */
    public CaseCooling cooling(final Era era) {
        return switch (this) {
            case SOLE -> switch (HardwareEra.partsIndex(era)) {
                case 0 -> CaseCooling.VINTAGE;
                case 1 -> CaseCooling.LEGACY;
                case 2 -> CaseCooling.TRANSITION;
                default -> CaseCooling.NEUTRAL;
            };
            case NEUTRAL -> CaseCooling.NEUTRAL;
            case HIGH_PERFORMANCE -> CaseCooling.HIGH_PERFORMANCE;
            case AESTHETIC -> CaseCooling.AESTHETIC;
        };
    }
}
