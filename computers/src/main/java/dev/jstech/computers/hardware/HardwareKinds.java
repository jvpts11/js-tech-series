/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import org.jetbrains.annotations.ApiStatus;

/** The kinds hardware is made of, closed together once the game has loaded: what a world knows doesn't change. */
@ApiStatus.Internal
public final class HardwareKinds {

    private HardwareKinds() {
    }

    /** Closes the memory generations, the slot generations, the form factors and the kinds of card; from the mod. */
    public static void close() {
        RamGeneration.close();
        PcieGeneration.close();
        FormFactor.close();
        ExpansionCardKind.close();
    }
}
