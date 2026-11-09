/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.energy.EnergyStore;

/**
 * A device's draw of so many watts, spent from its store a tick at a time: a tick's share is rarely a whole joule, so
 * the part left over is paid with the next tick's. The store is sized to a second of the draw, which is all a line
 * needs to keep it filled.
 */
final class TickDraw {

    /* The part of a joule a tick's draw left over. */
    private double owed;

    private static final int TICKS_A_SECOND = 20;

    /**
     * Spends a tick of {@code watts} from {@code store} and says whether it had them; a server that lets devices run
     * free spends nothing and always has them.
     */
    boolean draw(final EnergyStore store, final int watts) {
        store.setCapacity(watts);
        if (!ComputersServerConfig.drawEnergy()) {
            return true;
        }
        final double need = (double) watts / TICKS_A_SECOND + owed;
        final long whole = (long) need;
        // A store with less than this tick's share in it has run dry, though the share rounds down to nothing.
        if (store.damaged() || store.stored() < (long) Math.ceil(need)) {
            owed = 0.0;
            return false;
        }
        store.spend(whole);
        owed = need - whole;
        return true;
    }
}
