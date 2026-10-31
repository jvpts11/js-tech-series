/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.moving.YardLayout;
import dev.jstech.space.ship.AirReserveEntity;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * The flight consoles of the ships drawn on a player's game, by the plot of the yard their ship's blocks fill: what the
 * HUD of the pilot at a ship's helm reads its ship by.
 */
public final class ShipConsoles {

    private static final Map<Integer, WeakReference<AirReserveEntity>> DRAWN = new HashMap<>();

    private ShipConsoles() {
    }

    /** {@code console} was drawn, its ship's blocks in its plot of the yard. */
    static void drawn(final AirReserveEntity console) {
        DRAWN.put(YardLayout.plotOfBlock(console.getBlockPos().getX(), console.getBlockPos().getZ()),
                new WeakReference<>(console));
    }

    /** The console of the ship whose blocks fill the yard's plot {@code plot}, drawn on this game, or none. */
    static @Nullable AirReserveEntity of(final int plot) {
        final WeakReference<AirReserveEntity> kept = DRAWN.get(plot);
        final AirReserveEntity console = kept == null ? null : kept.get();
        if (console == null || console.isRemoved()) {
            DRAWN.remove(plot);
            return null;
        }
        return console;
    }
}
