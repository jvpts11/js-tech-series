/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Something other than a block that holds energy cables and chargers reach: a vehicle's battery, a robot's. A declared
 * entity that holds energy is handed to the game's energy capability through this.
 */
public interface IEnergyHolder {

    /** The energy it holds. */
    IEnergyStorage energy();
}
