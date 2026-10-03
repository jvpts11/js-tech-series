/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import dev.jstech.core.connect.Connection;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.resources.ResourceLocation;

/**
 * The peripheral line: the short cables between a machine and its own screens, speakers and devices, one cable for
 * each era (serial and parallel on the Vintage, then USB with the video cable of its day). Each era's cable is a
 * generation of the line, so a port takes its own era's cable and every earlier one, as a newer machine still takes an
 * older plug; an older device never takes a newer cable.
 *
 * <p>How far a run reaches grows with the era: eight cables on the Vintage, then twelve, fourteen, sixteen and twenty.
 * The numbers are first estimates.
 */
public final class PeripheralLine {

    private PeripheralLine() {
    }

    /** The line's id, as cables and ports name it. */
    public static ResourceLocation id() {
        return Ids.LINE;
    }

    /** What the peripheral cable of {@code era} offers the face it touches, and what a port of that era takes. */
    public static Connection of(final HardwareEra era) {
        return new Connection(id(), era.id());
    }

    /** Whether {@code line} is the peripheral line. */
    public static boolean is(final ResourceLocation line) {
        return id().equals(line);
    }

    /** How many cables a run of the peripheral cable of {@code era} reaches. */
    public static int range(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> 8;
            case LEGACY -> 12;
            case TRANSITION -> 14;
            case STANDARD -> 16;
            case ADVANCED, EXA, SINGULARITY -> 20;
        };
    }

    /* The id, made the first time it is asked for: the line's reaches are read where the game's types are not. */
    private static final class Ids {
        private static final ResourceLocation LINE = ResourceLocation.fromNamespaceAndPath("jscore", "peripheral");
    }
}
