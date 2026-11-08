/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.hardware.ComputerBuild;
import org.jetbrains.annotations.Nullable;

/**
 * What a computer's power draws, as its assembly screen shows it: the DRAW readout, its gauge and its tooltip.
 *
 * @param partsWatts what its parts take, in watts
 * @param psuWatts   its supply's rating, in watts; nothing without a supply
 * @param lineWatts  what its parts take from the line through its supply's efficiency, in watts
 * @param energized  whether it has the energy for a tick, or the server lets computers run free
 */
public record PowerShown(int partsWatts, int psuWatts, int lineWatts, boolean energized) {

    /** What {@code build}'s power draws, with energy for it or not; nothing at all for no build. */
    public static PowerShown of(@Nullable final ComputerBuild build, final boolean energized) {
        if (build == null) {
            return new PowerShown(0, 0, 0, energized);
        }
        return new PowerShown(build.powerDraw(), build.psu().wattage(),
                (int) Math.ceil(ComputerEnergy.lineWatts(build.powerDraw(), build.psu())), energized);
    }

    /** Its supply's load: what its parts take over its rating, from 0; nothing without a supply. */
    public double load() {
        return psuWatts == 0 ? 0.0 : (double) partsWatts / psuWatts;
    }
}
