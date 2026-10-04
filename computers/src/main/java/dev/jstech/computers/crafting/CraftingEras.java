/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.core.tier.HardwareEra;

/**
 * How much the autocrafting hardware of each era holds, in one place: how many Crafting Interfaces a Crafting Card
 * drives, how many bench recipes its ROM keeps, and how many {@code .craft} files an interface of that era holds. A
 * later era always holds more; the numbers are estimates a play test may move.
 */
public final class CraftingEras {

    private CraftingEras() {
    }

    /** How many Crafting Interfaces a Crafting Card of {@code era} drives. */
    public static int interfacesPerCard(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> 2;
            case LEGACY -> 4;
            case TRANSITION -> 5;
            case STANDARD -> 6;
            case ADVANCED, EXA, SINGULARITY -> 8;
        };
    }

    /** How many bench recipes the ROM on a Crafting Card of {@code era} keeps. */
    public static int romPerCard(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> 2;
            case LEGACY -> 4;
            case TRANSITION -> 5;
            case STANDARD -> 6;
            case ADVANCED, EXA, SINGULARITY -> 8;
        };
    }

    /** How many {@code .craft} files a Crafting Interface of {@code era} holds. */
    public static int patternsPerInterface(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> 3;
            case LEGACY -> 6;
            case TRANSITION -> 8;
            case STANDARD -> 9;
            case ADVANCED, EXA, SINGULARITY -> 12;
        };
    }
}
