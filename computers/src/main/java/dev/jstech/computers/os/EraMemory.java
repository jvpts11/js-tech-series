/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.jetbrains.annotations.Nullable;

/**
 * How much more memory the same program holds on a machine of a later era: software grows with the machines it is
 * written for, and what was a light tool on a machine of one age is a heavy one on the next.
 *
 * <p>A program declares what it holds on a machine of the Legacy era. On every other era that is multiplied by how much
 * more memory a typical machine of that era has than a Legacy one: a sixteenth on the Vintage, eight times on the
 * Transition, thirty-two on the Standard and a hundred and twenty-eight on the Advanced, each era four times the last
 * past that. So a typical machine of any era holds its system and about the same number of programs.
 */
public final class EraMemory {

    /** The era a program's declared memory is written for. */
    public static final HardwareEra DECLARED_FOR = HardwareEra.LEGACY;

    /** The factors are counted in sixteenths, which is how the Vintage's sixteenth stays a whole number. */
    private static final int SIXTEENTHS_PER_ONE = 16;

    private EraMemory() {
    }

    /**
     * What a program declaring {@code legacyMb} holds on a machine of {@code era}, never less than a megabyte for one
     * that holds anything; a machine whose era is not known is taken for a Legacy one.
     */
    public static int scaled(final int legacyMb, @Nullable final HardwareEra era) {
        if (legacyMb <= 0) {
            return 0;
        }
        final long mb = (long) legacyMb * sixteenths(era == null ? DECLARED_FOR : era) / SIXTEENTHS_PER_ONE;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, mb));
    }

    /** How many times a Legacy machine's memory a typical machine of {@code era} has, in sixteenths. */
    public static int sixteenths(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> 1;
            case LEGACY -> 16;
            case TRANSITION -> 128;
            case STANDARD -> 512;
            case ADVANCED -> 2_048;
            case EXA -> 8_192;
            case SINGULARITY -> 32_768;
        };
    }
}
